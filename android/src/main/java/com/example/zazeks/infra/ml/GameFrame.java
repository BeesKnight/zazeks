package com.example.zazeks.infra.ml;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable container with a single frame captured by the game engine.
 * The frame is expected to be JPEG/PNG encoded bytes that can be
 * forwarded to the Python recognition service.
 */
public final class GameFrame {
    private final byte[] imageBytes;
    private final int width;
    private final int height;
    private final long timestampMillis;

    public GameFrame(byte[] imageBytes, int width, int height, long timestampMillis) {
        this.imageBytes = Objects.requireNonNull(imageBytes, "imageBytes").clone();
        this.width = width;
        this.height = height;
        this.timestampMillis = timestampMillis;
    }

    public byte[] getImageBytes() {
        return imageBytes.clone();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    @Override
    public String toString() {
        return "GameFrame{" +
            "imageBytes=" + imageBytes.length +
            ", width=" + width +
            ", height=" + height +
            ", timestampMillis=" + timestampMillis +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameFrame)) return false;
        GameFrame that = (GameFrame) o;
        return width == that.width &&
            height == that.height &&
            timestampMillis == that.timestampMillis &&
            Arrays.equals(imageBytes, that.imageBytes);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(imageBytes);
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + Long.hashCode(timestampMillis);
        return result;
    }
}
