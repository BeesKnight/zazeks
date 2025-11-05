package com.zazeks.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Простой holder настроек ML-сервиса.
 * Читает ml.base-url из application.yml (по умолчанию http://localhost:8083).
 */
@Component
public class MlProperties {

    @Value("${ml.base-url:http://localhost:8083}")
    private String baseUrl;

    public String getBaseUrl() { return baseUrl; }

    @PostConstruct
    void normalize() {
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }

    @Override public String toString() { return "MlProperties{baseUrl='" + baseUrl + "'}"; }
}
