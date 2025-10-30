package com.zazeks.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Полноценная точка входа Spring Boot-приложения, заменяющая минимальный
 * HTTP-сервер и предоставляющая структурированную маршрутизацию и поддержку
 * WebSocket, эквивалентную Python-приложению {@code backend/src/main.py}.
 */
@SpringBootApplication(scanBasePackages = "com.zazeks")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
