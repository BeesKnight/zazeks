package com.zazeks.app;

import com.zazeks.api.AuthService;
import com.zazeks.api.GameService;
import com.zazeks.api.UserService;
import com.zazeks.database.InMemoryDatabase;
import com.zazeks.security.PasswordService;
import com.zazeks.security.TokenService;

/**
 * Минимальный Java-эквивалент Python-приложения {@code backend/src/main.py}.
 * Вместо запуска FastAPI настраивает in-memory сервисы и демонстрирует
 * базовое использование.
 */
public final class Application {
    private Application() {}

    public static void main(String[] args) {
        InMemoryDatabase database = new InMemoryDatabase();
        PasswordService passwordService = new PasswordService();
        TokenService tokenService = new TokenService();

        AuthService authService = new AuthService(database, passwordService, tokenService);
        UserService userService = new UserService(database);
        GameService gameService = new GameService(database);

        System.out.println("Java backend analogue initialized. You can wire these services into a web framework if needed.");
        System.out.printf("Services: %s, %s, %s%n", authService.getClass().getSimpleName(), userService.getClass().getSimpleName(), gameService.getClass().getSimpleName());
    }
}
