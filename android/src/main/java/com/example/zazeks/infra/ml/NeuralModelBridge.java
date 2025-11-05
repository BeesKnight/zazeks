package com.example.zazeks.infra.ml;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Шлюз к HTTP-бэкенду детекции.
 * ВОЗВРАЩАЕТ DetectionResult (как ждут VM после последних изменений).
 * Оставлены статические фабрики remoteHttp(...) для совместимости со старым DI.
 */
public class NeuralModelBridge {

    private static final String TAG = "NN";
    private static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");
    private static final String CONFIG_PATH = "config/backend.json";

    private final OkHttpClient http;
    private final String baseUrl; // нормализованный, без завершающего '/'

    // ---- КОНСТРУКТОРЫ ----

    /** Читает baseUrl из assets/config/backend.json */
    public NeuralModelBridge(@NonNull Context appContext) {
        this.http = new OkHttpClient();
        String url = "http://127.0.0.1:8082";
        try {
            JSONObject cfg = readJsonFromAssets(appContext.getAssets(), CONFIG_PATH);
            if (cfg != null) url = cfg.optString("baseUrl", url);
        } catch (Exception e) {
            Log.w(TAG, "backend.json read failed, using default baseUrl: " + url, e);
        }
        this.baseUrl = normalizeUrl(url);
    }

    /** Новый путь: DI прокидывает client + endpoint вручную */
    public NeuralModelBridge(@NonNull OkHttpClient client, @NonNull String endpoint) {
        this.http = client;
        this.baseUrl = normalizeUrl(endpoint);
    }

    // ---- СТАТИЧЕСКИЕ ФАБРИКИ (совместимость с прежним кодом) ----
    public static NeuralModelBridge remoteHttp(@NonNull Context context) {
        return new NeuralModelBridge(context);
    }

    public static NeuralModelBridge remoteHttp(@NonNull OkHttpClient client, @NonNull String endpoint) {
        return new NeuralModelBridge(client, endpoint);
    }

    // ---- ПУБЛИЧНОЕ API ----

    /** Синхронная детекция. Оборачивайте в корутину/Executor при необходимости. */
    @NonNull
    public DetectionResult detect(@NonNull GameFrame frame) throws IOException, JSONException {
        byte[] jpeg = getBytes(frame);
        if (jpeg == null || jpeg.length == 0) throw new IOException("Empty JPEG bytes");

        RequestBody filePart = RequestBody.create(jpeg, MEDIA_TYPE_JPEG);
        MultipartBody reqBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "frame.jpg", filePart)
                .build();

        Request req = new Request.Builder()
                .url(baseUrl + "/model/detect")
                .post(reqBody)
                .build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code() + " " + resp.message());
            try (ResponseBody body = resp.body()) {
                if (body == null) throw new IOException("Empty body");
                String raw = body.string();

                // Лог на время отладки (в релизе отключить)
                Log.d(TAG, "model raw: " + raw);

                return parseResponse(raw, frame.getWidth(), frame.getHeight());
            }
        }
    }

    // ---- ПАРСИНГ JSON -> DetectionResult ----

    @NonNull
    private DetectionResult parseResponse(@NonNull String json, int frameW, int frameH) throws JSONException {
        JSONObject obj = new JSONObject(json);

        String gesture = obj.optString("gesture", "Unknown");
        Double confidence = obj.has("confidence") ? obj.optDouble("confidence") : null;

        int left = 0, top = 0, right = frameW, bottom = frameH;
        if (obj.has("bbox")) {
            JSONArray bb = obj.getJSONArray("bbox");
            if (bb.length() >= 4) {
                double x1 = asDouble(bb, 0);
                double y1 = asDouble(bb, 1);
                double x2 = asDouble(bb, 2);
                double y2 = asDouble(bb, 3);

                boolean normalized = (x1 >= 0 && x1 <= 1) && (y1 >= 0 && y1 <= 1)
                                  && (x2 >= 0 && x2 <= 1) && (y2 >= 0 && y2 <= 1);

                if (normalized) {
                    x1 *= frameW; x2 *= frameW;
                    y1 *= frameH; y2 *= frameH;
                }

                double l = Math.min(x1, x2);
                double r = Math.max(x1, x2);
                double t = Math.min(y1, y2);
                double b = Math.max(y1, y2);

                left   = clamp((int)Math.round(l), 0, frameW - 1);
                right  = clamp((int)Math.round(r), 0, frameW - 1);
                top    = clamp((int)Math.round(t), 0, frameH - 1);
                bottom = clamp((int)Math.round(b), 0, frameH - 1);
            }
        }

        DetectionResult res = new DetectionResult();
        res.setGesture((gesture == null || gesture.isEmpty()) ? "Unknown" : gesture);
        res.setConfidence(confidence);
        res.setLeft(left);
        res.setTop(top);
        res.setRight(right);
        res.setBottom(bottom);
        res.setFrameWidth(frameW);
        res.setFrameHeight(frameH);
        return res;
    }

    // ---- УТИЛИТЫ ----

    private static JSONObject readJsonFromAssets(AssetManager am, String path) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(am.open(path), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = br.readLine()) != null; ) sb.append(line);
            return new JSONObject(sb.toString());
        }
    }

    private static String normalizeUrl(String base) {
        if (base == null || base.isEmpty()) return "http://127.0.0.1:8082";
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private static double asDouble(JSONArray arr, int idx) {
        Object o = arr.opt(idx);
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception ignore) { return 0.0; }
    }

    /** Универсально достаём JPEG-байты из GameFrame разных реализаций. */
    private static byte[] getBytes(GameFrame frame) {
        try { return (byte[]) GameFrame.class.getMethod("getBytes").invoke(frame); } catch (Throwable ignore) {}
        try { return (byte[]) GameFrame.class.getMethod("getJpeg").invoke(frame); } catch (Throwable ignore) {}
        try { return (byte[]) GameFrame.class.getMethod("getJpegBytes").invoke(frame); } catch (Throwable ignore) {}
        try { return (byte[]) GameFrame.class.getField("bytes").get(frame); } catch (Throwable ignore) {}
        return null;
    }
}
