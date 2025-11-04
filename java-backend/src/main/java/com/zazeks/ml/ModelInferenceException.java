package com.zazeks.ml;

/**
 * Signals irrecoverable problems while running model inference.
 */
public class ModelInferenceException extends RuntimeException {
    public ModelInferenceException(String message) {
        super(message);
    }

    public ModelInferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
