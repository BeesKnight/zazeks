package com.zazeks.api;

import com.zazeks.config.Settings;
import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.User;
import com.zazeks.security.PasswordService;
import com.zazeks.security.TokenService;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Java-аналог модуля {@code backend/src/api/auth.py}.
 * Реализует регистрацию и авторизацию пользователей.
 */
public class AuthService {
    private static final Pattern USERNAME_PASSWORD_REGEX = Pattern.compile("^[a-zA-Z0-9_]{4,15}$");

    private final InMemoryDatabase database;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final Settings settings = Settings.getInstance();

    public AuthService(InMemoryDatabase database, PasswordService passwordService, TokenService tokenService) {
        this.database = database;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
    }

    public RegistrationResult register(String username, String password, String photo) {
        if (!USERNAME_PASSWORD_REGEX.matcher(username).matches()) {
            throw new IllegalArgumentException("Username must be 4 to 15 characters long and contain only letters, digits, or underscore (_).");
        }
        if (!USERNAME_PASSWORD_REGEX.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must be 4 to 15 characters long and contain only letters, digits, or underscore (_).");
        }
        Optional<User> existingUser = database.findUserByUsername(username);
        if (existingUser.isPresent()) {
            throw new IllegalStateException("Username already exists");
        }
        String hashed = passwordService.hashPassword(password);
        User user = new User(username, hashed, photo);
        database.saveUser(user);
        return new RegistrationResult(user.getId(), "User registered successfully");
    }

    public LoginResult login(String username, String password) {
        User user = database.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
        if (!passwordService.verifyPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        Duration ttl = settings.getAccessTokenTtl();
        String token = tokenService.createAccessToken(Map.of("sub", String.valueOf(user.getId())), ttl);
        return new LoginResult(token, "bearer", user.getId());
    }

    public record RegistrationResult(int userId, String message) {}

    public record LoginResult(String accessToken, String tokenType, int userId) {}
}
