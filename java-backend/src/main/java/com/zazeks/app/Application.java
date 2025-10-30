package com.zazeks.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zazeks.api.AuthService;
import com.zazeks.api.GameService;
import com.zazeks.api.UserService;
import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.Game;
import com.zazeks.security.PasswordService;
import com.zazeks.security.TokenService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Минимальный HTTP-сервер, использующий in-memory реализацию хранилища.
 */
public final class Application {
    private Application() {
    }

    public static void main(String[] args) throws IOException {
        InMemoryDatabase database = new InMemoryDatabase();
        PasswordService passwordService = new PasswordService();
        TokenService tokenService = new TokenService();

        AuthService authService = new AuthService(database, passwordService, tokenService);
        UserService userService = new UserService(database);
        GameService gameService = new GameService(database);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        int port = resolvePort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));

        server.createContext("/health", exchange ->
                handleRequest(exchange, "GET", objectMapper, () -> sendJson(exchange, 200, new HealthResponse("ok"), objectMapper)));

        server.createContext("/auth/register", exchange -> handleRequest(exchange, "POST", objectMapper, () -> {
            RegisterRequest request = readJson(exchange, RegisterRequest.class, objectMapper);
            AuthService.RegistrationResult result = authService.register(request.username(), request.password(), request.photo());
            sendJson(exchange, 201, result, objectMapper);
        }));

        server.createContext("/auth/login", exchange -> handleRequest(exchange, "POST", objectMapper, () -> {
            LoginRequest request = readJson(exchange, LoginRequest.class, objectMapper);
            AuthService.LoginResult result = authService.login(request.username(), request.password());
            sendJson(exchange, 200, result, objectMapper);
        }));

        server.createContext("/user/leaderboard/offline", exchange -> handleRequest(exchange, "GET", objectMapper, () -> {
            List<UserService.UserSummary> leaderboard = userService.getOfflineLeaderboard();
            sendJson(exchange, 200, leaderboard, objectMapper);
        }));

        server.createContext("/user/leaderboard/online", exchange -> handleRequest(exchange, "GET", objectMapper, () -> {
            List<UserService.UserSummary> leaderboard = userService.getOnlineLeaderboard();
            sendJson(exchange, 200, leaderboard, objectMapper);
        }));

        server.createContext("/user/profile", exchange -> handleRequest(exchange, "GET", objectMapper, () -> {
            int userId = authenticate(exchange, tokenService);
            UserService.UserProfile profile = userService.getUserProfile(userId, userId);
            sendJson(exchange, 200, profile, objectMapper);
        }));

        server.createContext("/user/profile/update", exchange -> handleRequest(exchange, "PUT", objectMapper, () -> {
            int userId = authenticate(exchange, tokenService);
            UpdateProfileRequest request = readJson(exchange, UpdateProfileRequest.class, objectMapper);
            UserService.UpdateResult result = userService.updateUserProfile(userId, userId, request.username(), request.photo());
            sendJson(exchange, 200, result, objectMapper);
        }));

        server.createContext("/game", exchange -> handleRequest(exchange, "POST", objectMapper, () -> {
            int userId = authenticate(exchange, tokenService);
            GameRequest request = readJson(exchange, GameRequest.class, objectMapper);
            Game game = gameService.createGame(userId, request.userChoice(), request.computerChoice(), request.result());
            sendJson(exchange, 201, game, objectMapper);
        }));

        server.createContext("/game/history", exchange -> handleRequest(exchange, "GET", objectMapper, () -> {
            int userId = authenticate(exchange, tokenService);
            List<Game> games = gameService.getGamesForUser(userId, userId);
            sendJson(exchange, 200, games, objectMapper);
        }));

        server.start();
        System.out.printf("Java backend started on http://localhost:%d%n", port);
    }

    private static int resolvePort() {
        String raw = System.getenv("PORT");
        if (raw == null) {
            return 8080;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return 8080;
        }
    }

    private static int authenticate(HttpExchange exchange, TokenService tokenService) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new HttpStatusException(401, "Missing or invalid Authorization header");
        }
        String token = header.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new HttpStatusException(401, "Missing or invalid Authorization header");
        }
        try {
            return tokenService.extractUserId(token);
        } catch (IllegalArgumentException ex) {
            throw new HttpStatusException(401, "Invalid or expired token", ex);
        }
    }

    private static <T> T readJson(HttpExchange exchange, Class<T> type, ObjectMapper objectMapper) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            throw new HttpStatusException(400, "Request body is required");
        }
        return objectMapper.readValue(body, type);
    }

    private static void sendJson(HttpExchange exchange, int statusCode, Object body, ObjectMapper objectMapper) throws IOException {
        byte[] response = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private static void sendError(HttpExchange exchange, int statusCode, String message, ObjectMapper objectMapper) throws IOException {
        ErrorResponse error = new ErrorResponse(message);
        byte[] response = objectMapper.writeValueAsBytes(error);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private static void handleRequest(HttpExchange exchange, String method, ObjectMapper objectMapper, ThrowingRunnable runnable) throws IOException {
        try {
            if (!exchange.getRequestMethod().equalsIgnoreCase(method)) {
                throw new HttpStatusException(405, "Method Not Allowed");
            }
            runnable.run();
        } catch (HttpStatusException ex) {
            sendError(exchange, ex.statusCode(), ex.getMessage(), objectMapper);
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage(), objectMapper);
        } catch (IllegalStateException ex) {
            sendError(exchange, 409, ex.getMessage(), objectMapper);
        } catch (SecurityException ex) {
            sendError(exchange, 403, ex.getMessage(), objectMapper);
        } catch (JsonProcessingException ex) {
            sendError(exchange, 400, "Invalid JSON payload", objectMapper);
        } catch (Exception ex) {
            ex.printStackTrace();
            sendError(exchange, 500, "Internal server error", objectMapper);
        } finally {
            exchange.close();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private record HealthResponse(String status) {
    }

    private record ErrorResponse(String error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RegisterRequest(String username, String password, String photo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LoginRequest(String username, String password) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GameRequest(String userChoice, String computerChoice, String result) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UpdateProfileRequest(String username, String photo) {
    }

    private static final class HttpStatusException extends RuntimeException {
        private final int statusCode;

        private HttpStatusException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        private HttpStatusException(int statusCode, String message, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
        }

        private int statusCode() {
            return statusCode;
        }
    }
}
