package com.zazeks.ml;

import java.util.Arrays;

/**
 * Минималистичная полносвязная нейросеть без внешних библиотек.
 * Используется для классификации признаков изображения на три класса жестов.
 */
public final class SimpleNeuralNetwork {
    private final double[][] weights;
    private final double[] biases;

    private SimpleNeuralNetwork(double[][] weights, double[] biases) {
        this.weights = weights;
        this.biases = biases;
    }

    public static SimpleNeuralNetwork defaultModel() {
        double[][] weights = {
                {3.0, -1.0, -1.0, 0.5},   // rock
                {-1.0, 3.0, -1.0, 0.5},   // paper
                {-1.0, -1.0, 3.0, 0.5}    // scissors
        };
        double[] biases = {0.2, 0.2, 0.2};
        return new SimpleNeuralNetwork(weights, biases);
    }

    public double[] predict(double[] features) {
        if (features.length != weights[0].length) {
            throw new IllegalArgumentException("Unexpected feature vector size: " + features.length);
        }
        double[] logits = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            double sum = biases[i];
            for (int j = 0; j < features.length; j++) {
                sum += weights[i][j] * features[j];
            }
            logits[i] = sum;
        }
        return softmax(logits);
    }

    private double[] softmax(double[] logits) {
        double max = Arrays.stream(logits).max().orElse(0.0);
        double[] exps = new double[logits.length];
        double sum = 0.0;
        for (int i = 0; i < logits.length; i++) {
            exps[i] = Math.exp(logits[i] - max);
            sum += exps[i];
        }
        for (int i = 0; i < exps.length; i++) {
            exps[i] = exps[i] / sum;
        }
        return exps;
    }
}
