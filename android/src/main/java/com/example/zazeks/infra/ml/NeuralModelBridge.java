package com.example.zazeks.infra.ml;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Adapter that forwards frames produced by the Android game engine to the
 * recognition service. The bridge is synchronous to simplify
 * integration with the existing coroutine-based game flow.
 */
public final class NeuralModelBridge {
    private static final MediaType MEDIA_TYPE_JPEG = MediaType.get("image/jpeg");

    private final OkHttpClient httpClient;
    private final HttpUrl detectEndpoint;

    /** Canonical ctor. */
    public NeuralModelBridge(OkHttpClient httpClient, HttpUrl detectEndpoint) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.detectEndpoint = Objects.requireNonNull(detectEndpoint, "detectEndpoint");
    }

    /** Convenience overload accepting a String URL. */
    public NeuralModelBridge(OkHttpClient httpClient, String detectEndpoint) {
        this(httpClient, requireHttpUrl(detectEndpoint));
    }

    /**
     * Factory method that configures the bridge in remote HTTP mode.
     *
     * @param baseUrl base URL of the backend, e.g. {@code http://10.0.2.2:8000/}
     */
    public static NeuralModelBridge remoteHttp(String baseUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl");
        HttpUrl parsed = HttpUrl.parse(baseUrl);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid base URL: " + baseUrl);
        }
        HttpUrl endpoint = parsed.resolve("/model/detect");
        if (endpoint == null) {
            throw new IllegalArgumentException("Unable to resolve /model/detect using base URL: " + baseUrl);
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        return new NeuralModelBridge(client, endpoint);
    }

    private static HttpUrl requireHttpUrl(String url) {
        HttpUrl parsed = HttpUrl.parse(Objects.requireNonNull(url, "detectEndpoint"));
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid detectEndpoint URL: " + url);
        }
        return parsed;
    }

    /**
     * Performs gesture detection. The frame is sent as multipart/form-data using
     * the same contract that the backend service expects.
     */
    public DetectionResult detect(GameFrame frame) throws IOException {
        Objects.requireNonNull(frame, "frame");

        // NB: For OkHttp4 from Java this overload is valid: (MediaType, byte[])
        RequestBody imageBody = RequestBody.create(MEDIA_TYPE_JPEG, frame.getImageBytes());

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", buildFileName(frame), imageBody)
                .build();

        Request request = new Request.Builder()
                .url(detectEndpoint)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Model service returned " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response body from model service");
            }
            return parseResponse(body.string(), frame.getWidth(), frame.getHeight());
        }
    }

    private DetectionResult parseResponse(String body, int frameWidth, int frameHeight) throws IOException {
        try {
            JSONObject json = new JSONObject(body);
            String gesture = json.optString("gesture", "Unknown");
            JSONArray bboxJson = json.optJSONArray("bbox");
            double confidence = json.optDouble("confidence", 0.0);

            int[] bbox = new int[0];
            if (bboxJson != null && bboxJson.length() == 4) {
                bbox = new int[4];
                for (int i = 0; i < 4; i++) {
                    bbox[i] = bboxJson.optInt(i, 0);
                }
            }
            return new DetectionResult(gesture, bbox, confidence, frameWidth, frameHeight);
        } catch (JSONException e) {
            throw new IOException("Failed to parse model response", e);
        }
    }

    private static String buildFileName(GameFrame frame) {
        return String.format(
                Locale.US,
                "frame_%dx%d_%d.jpg",
                frame.getWidth(),
                frame.getHeight(),
                frame.getTimestampMillis()
        );
    }
}
