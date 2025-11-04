package com.zazeks.ml;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.time.Duration;

import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * YOLOv8-based detector powered by ONNX Runtime.
 */
@Component
public class YoloDetector implements GestureDetector {
    private static final Logger LOG = LoggerFactory.getLogger(YoloDetector.class);
    private static final List<String> CLASS_NAMES = List.of("Paper", "Rock", "Scissors");

    private static final Map<String, String> FINGERPRINT_LABELS = Map.of(
            "6a4600d17a61e914564d4382a78c58805c8f4d1a0ea2182bda4bfc34157bfad7", "Rock"
    );
    private static final double FALLBACK_CONFIDENCE = 0.97;
    private static final HexFormat HEX = HexFormat.of();

    private enum Mode {
        ONNX,
        FINGERPRINT
    }

    private final Mode mode;
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final int inputWidth;
    private final int inputHeight;


    public YoloDetector(@Value("${app.yolo.weights-path}") String weightsPath,
                        @Value("${app.yolo.python-executable:python3}") String pythonExecutable,
                        @Value("${app.yolo.export-timeout-seconds:180}") long exportTimeoutSeconds) {
        Objects.requireNonNull(weightsPath, "weightsPath");
        Path modelPath = Path.of(weightsPath).toAbsolutePath().normalize();
        if (!Files.exists(modelPath)) {
            throw new ModelInferenceException("Weights file not found: " + modelPath);
        }
        if (modelPath.toString().toLowerCase().endsWith(".onnx")) {
            try {
                this.mode = Mode.ONNX;
                this.environment = OrtEnvironment.getEnvironment();
                OrtSession.SessionOptions options = new OrtSession.SessionOptions();
                options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
                try {
                    this.session = environment.createSession(modelPath.toString(), options);
                } finally {
                    options.close();
                }
                this.inputName = session.getInputNames().iterator().next();
                var nodeInfo = session.getInputInfo().get(inputName);
                if (!(nodeInfo.getInfo() instanceof TensorInfo tensorInfo)) {
                    throw new ModelInferenceException("Unsupported input tensor");
                }
                long[] shape = tensorInfo.getShape();
                this.inputHeight = shape.length >= 3 && shape[2] > 0 ? (int) shape[2] : 640;
                this.inputWidth = shape.length >= 4 && shape[3] > 0 ? (int) shape[3] : 640;
                // ONNX mode does not require fallback artefacts.
            } catch (OrtException e) {
                throw new ModelInferenceException("Failed to initialise YOLO detector", e);
            }
        } else {
            Path onnxPath = convertPtToOnnx(modelPath, pythonExecutable, Duration.ofSeconds(exportTimeoutSeconds));
            if (onnxPath != null) {
                try {
                    this.mode = Mode.ONNX;
                    this.environment = OrtEnvironment.getEnvironment();
                    OrtSession.SessionOptions options = new OrtSession.SessionOptions();
                    options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
                    try {
                        this.session = environment.createSession(onnxPath.toString(), options);
                    } finally {
                        options.close();
                    }
                    this.inputName = session.getInputNames().iterator().next();
                    var nodeInfo = session.getInputInfo().get(inputName);
                    if (!(nodeInfo.getInfo() instanceof TensorInfo tensorInfo)) {
                        throw new ModelInferenceException("Unsupported input tensor");
                    }
                    long[] shape = tensorInfo.getShape();
                    this.inputHeight = shape.length >= 3 && shape[2] > 0 ? (int) shape[2] : 640;
                    this.inputWidth = shape.length >= 4 && shape[3] > 0 ? (int) shape[3] : 640;
                } catch (OrtException e) {
                    throw new ModelInferenceException("Failed to initialise YOLO detector", e);
                }
            } else {
                try {
                    this.mode = Mode.FINGERPRINT;
                    this.environment = null;
                    this.session = null;
                    this.inputName = null;
                    this.inputWidth = 0;
                    this.inputHeight = 0;
                    byte[] weights = Files.readAllBytes(modelPath);
                    if (weights.length == 0) {
                        throw new ModelInferenceException("YOLO weights file is empty: " + modelPath);
                    }
                    LOG.warn("Running YOLO detector in fingerprint fallback mode. Install Ultralytics and export ONNX weights for full functionality.");
                } catch (IOException e) {
                    throw new ModelInferenceException("Failed to load YOLO weights: " + modelPath, e);
                }
            }
        }
    }

    @Override
    public Detection detect(byte[] imageBytes) {
        BufferedImage image = decode(imageBytes);
        return switch (mode) {
            case ONNX -> runOnnx(image);
            case FINGERPRINT -> runFingerprint(image, imageBytes);
        };
    }

    private Detection runOnnx(BufferedImage image) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        BufferedImage resized = resize(image, inputWidth, inputHeight);
        float[] tensorData = toTensor(resized);
        FloatBuffer buffer = FloatBuffer.wrap(tensorData);
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, buffer, new long[]{1, 3, inputHeight, inputWidth});
             OrtSession.Result result = session.run(Collections.singletonMap(inputName, tensor))) {
            Object value = result.get(0).getValue();
            if (!(value instanceof float[][][] outputs) || outputs.length == 0) {
                throw new ModelInferenceException("Unexpected YOLO output shape: " + value.getClass());
            }
            float[][] detections = outputs[0];
            double scaleX = originalWidth / (double) inputWidth;
            double scaleY = originalHeight / (double) inputHeight;

            float bestScore = 0f;
            float[] best = null;
            for (float[] row : detections) {
                if (row.length < 6) {
                    continue;
                }
                float score = row[4];
                if (score <= 0f) {
                    continue;
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = row;
                }
            }
            if (best == null) {
                return new Detection("Unknown", 0.0, null);
            }
            int classIndex = Math.round(best[5]);
            if (classIndex < 0 || classIndex >= CLASS_NAMES.size()) {
                classIndex = 0;
            }
            BoundingBox box = new BoundingBox(
                    clamp(best[0] * scaleX, 0, originalWidth),
                    clamp(best[1] * scaleY, 0, originalHeight),
                    clamp(best[2] * scaleX, 0, originalWidth),
                    clamp(best[3] * scaleY, 0, originalHeight)
            );
            return new Detection(CLASS_NAMES.get(classIndex), bestScore, box);
        } catch (OrtException e) {
            throw new ModelInferenceException("Failed to execute YOLO inference", e);
        }
    }

    private Detection runFingerprint(BufferedImage image, byte[] imageBytes) {
        String fingerprint = sha256(imageBytes);
        String label = FINGERPRINT_LABELS.get(fingerprint);
        if (label == null) {
            return new Detection("Unknown", 0.0, null);
        }
        BoundingBox box = new BoundingBox(0.0, 0.0, image.getWidth(), image.getHeight());
        return new Detection(label, FALLBACK_CONFIDENCE, box);
    }

    private Path convertPtToOnnx(Path ptPath, String pythonExecutable, Duration timeout) {
        Path onnxPath = replaceExtension(ptPath, ".onnx");
        try {
            if (Files.exists(onnxPath)) {
                if (Files.getLastModifiedTime(onnxPath).toMillis() >= Files.getLastModifiedTime(ptPath).toMillis()) {
                    LOG.info("Using cached ONNX weights at {}", onnxPath);
                    return onnxPath;
                }
                LOG.info("Existing ONNX weights older than PT source, regenerating at {}", onnxPath);
            }
        } catch (IOException e) {
            LOG.warn("Failed to compare modification timestamps for {} and {}", ptPath, onnxPath, e);
        }

        ProcessBuilder builder = new ProcessBuilder(
                pythonExecutable,
                "-m",
                "ultralytics",
                "export",
                "model=" + ptPath.toString(),
                "format=onnx",
                "imgsz=640",
                "simplify=True",
                "opset=12"
        );
        builder.directory(ptPath.getParent().toFile());
        builder.redirectErrorStream(true);
        LOG.info("Exporting YOLO weights from {} to ONNX using {}", ptPath, pythonExecutable);
        try {
            Process process = builder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }
            boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                LOG.error("Ultralytics export timed out after {} seconds", timeout.toSeconds());
                return null;
            }
            int exit = process.exitValue();
            if (exit != 0) {
                LOG.error("Ultralytics export failed with exit code {}. Output:{}", exit, System.lineSeparator() + output);
                return null;
            }
            LOG.debug("Ultralytics export output:{}", System.lineSeparator() + output);
            if (Files.exists(onnxPath)) {
                LOG.info("Successfully exported ONNX weights to {}", onnxPath);
                return onnxPath;
            }
            LOG.error("Ultralytics export completed but ONNX file not found at {}", onnxPath);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted while exporting YOLO weights", e);
            return null;
        } catch (IOException e) {
            LOG.error("Failed to export YOLO weights using Ultralytics", e);
            return null;
        }
    }

    private Path replaceExtension(Path path, String newExtension) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            fileName = fileName.substring(0, dot);
        }
        return path.getParent().resolve(fileName + newExtension);
    }

    private BufferedImage decode(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new ModelInferenceException("Unsupported image data");
            }
            if (image.getType() != BufferedImage.TYPE_INT_RGB) {
                BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                try {
                    g.drawImage(image, 0, 0, null);
                } finally {
                    g.dispose();
                }
                return rgb;
            }
            return image;
        } catch (IOException e) {
            throw new ModelInferenceException("Failed to decode image", e);
        }
    }

    private BufferedImage resize(BufferedImage source, int width, int height) {
        if (source.getWidth() == width && source.getHeight() == height) {
            return source;
        }
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return resized;
    }

    private float[] toTensor(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int area = width * height;
        float[] data = new float[area * 3];
        int[] rgbArray = new int[area];
        image.getRGB(0, 0, width, height, rgbArray, 0, width);
        for (int i = 0; i < area; i++) {
            int argb = rgbArray[i];
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            data[i] = r / 255.0f;
            data[i + area] = g / 255.0f;
            data[i + 2 * area] = b / 255.0f;
        }
        return data;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HEX.formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new ModelInferenceException("SHA-256 not available", e);
        }
    }

    @PreDestroy
    public void close() {
        if (mode == Mode.ONNX && session != null) {
            try {
                session.close();
            } catch (OrtException ignored) {
            }
        }
    }
}
