package com.zazeks.config;

import java.time.Duration;
import java.util.Optional;

/**
 * Java-аналог Python-модуля {@code backend/src/config.py}.
 * Считывает ключевые настройки приложения из переменных окружения и
 * предоставляет их в виде неизменяемого singleton-объекта.
 */
public final class Settings {
    private static final String DEFAULT_DATABASE_URL = "jdbc:sqlite:./test.db";
    private static final String DEFAULT_SECRET_KEY = "6cJONYl/S0ugD1OhpohcSeDS8os3ZQg/xPG2EvJ9J7o="; // Base64-encoded 256-bit ключ
    private static final String DEFAULT_ALGORITHM = "HS256";
    private static final long DEFAULT_ACCESS_TOKEN_MINUTES = 30L;

    private final String databaseUrl;
    private final String secretKey;
    private final String algorithm;
    private final Duration accessTokenTtl;

    private static final Settings INSTANCE = new Settings();

    private Settings() {
        this.databaseUrl = Optional.ofNullable(System.getenv("DATABASE_URL")).orElse(DEFAULT_DATABASE_URL);
        this.secretKey = Optional.ofNullable(System.getenv("SECRET_KEY")).orElse(DEFAULT_SECRET_KEY);
        this.algorithm = DEFAULT_ALGORITHM;
        long minutes = DEFAULT_ACCESS_TOKEN_MINUTES;
        String raw = System.getenv("ACCESS_TOKEN_EXPIRE_MINUTES");
        if (raw != null) {
            try {
                minutes = Long.parseLong(raw);
            } catch (NumberFormatException ignored) {
                minutes = DEFAULT_ACCESS_TOKEN_MINUTES;
            }
        }
        this.accessTokenTtl = Duration.ofMinutes(minutes);
    }

    public static Settings getInstance() {
        return INSTANCE;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }
}
