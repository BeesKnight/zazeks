package com.zazeks.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zazeks.api.AdminService;
import com.zazeks.api.AuthService;
import com.zazeks.api.GameService;
import com.zazeks.api.InferenceService;
import com.zazeks.api.MultiplayerService;
import com.zazeks.api.UserService;
import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.Game;
import com.zazeks.security.PasswordService;
import com.zazeks.security.TokenService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
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
        AdminService adminService = new AdminService(database);
        MultiplayerService multiplayerService = new MultiplayerService(database);
        InferenceService inferenceService = new InferenceService();

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

        server.createContext("/admin/users", exchange -> handleRequest(exchange, "GET", objectMapper, () -> {
            int adminId = authenticate(exchange, tokenService);
            List<AdminService.UserSnapshot> users = adminService.listUsers(adminId);
            sendJson(exchange, 200, users, objectMapper);
        }));

        server.createContext("/admin/users/grant", exchange -> handleRequest(exchange, "POST", objectMapper, () -> {
            int adminId = authenticate(exchange, tokenService);
            AdminUserRequest request = readJson(exchange, AdminUserRequest.class, objectMapper);
            adminService.grantAdmin(adminId, request.userId());
            sendJson(exchange, 200, new MessageResponse("Admin rights granted"), objectMapper);
        }));

        server.createContext("/admin/users/revoke", exchange -> handleRequest(exchange, "POST", objectMapper, () -> {
            int adminId = authenticate(exchange, tokenService);
            AdminUserRequest request = readJson(exchange, AdminUserRequest.class, objectMapper);
            adminService.revokeAdmin(adminId, request.userId());
            sendJson(exchange, 200, new MessageResponse("Admin rights revoked"), objectMapper);
        }));

        server.createContext("/admin/users/delete", exchange -> handleRequest(exchange, "POST", objectMapper, () -> {
            int adminId = authenticate(exchange, tokenService);
            AdminUserRequest request = readJson(exchange, AdminUserRequest.class, objectMapper);
            adminService.deleteUser(adminId, request.userId());
            sendJson(exchange, 200, new MessageResponse("User deleted"), objectMapper);
        }));

        server.createContext("/admin/games", exchange -> handleRequest(exchange, "GET", objectMapper, () -> {
            int adminId = authenticate(exchange, tokenService);
            List<Game> allGames = adminService.listAllGames(adminId);
            sendJson(exchange, 200, allGames, objectMapper);
        }));

        server.createContext("/admin/games/delete", exchange -> handleRequest(exchange, "POST", objectMapper, () -> {
            int adminId = authenticate(exchange, tokenService);
            AdminGameRequest request = readJson(exchange, AdminGameRequest.class, objectMapper);
            adminService.deleteGame(adminId, request.gameId());
            sendJson(exchange, 200, new MessageResponse("Game deleted"), objectMapper);
        }));

        server.createContext("/multiplayer/queue/join", exchange -> handleRequest(exchange, "POST", objectMapper, () -> {
            int userId = authenticate(exchange, tokenService);
            MultiplayerService.MatchState state = multiplayerService.joinQueue(userId);
            sendJson(exchange, 200, MatchResponse.from(state), objectMapper);
        }));

        server.createContext("/multiplayer/queue/leave", exchange -> handleRequest(exchange, "POST", objectMapper, () -> {
            int userId = authenticate(exchange, tokenService);
            multiplayerService.leaveQueue(userId);
            sendJson(exchange, 200, new MessageResponse("Left queue"), objectMapper);
        }));

        server.createContext("/multiplayer/match", exchange -> handleRequest(exchange, "GET", objectMapper, () -> {
            int userId = authenticate(exchange, tokenService);
            int matchId = requireQueryInt(exchange, "matchId");
            MultiplayerService.MatchState state = multiplayerService.getMatch(userId, matchId);
            sendJson(exchange, 200, MatchResponse.from(state), objectMapper);
        }));

        server.createContext("/multiplayer/match/gesture", exchange -> handleRequest(exchange, "POST", objectMapper, () -> {
            int userId = authenticate(exchange, tokenService);
            GestureRequest request = readJson(exchange, GestureRequest.class, objectMapper);
            MultiplayerService.MatchState state = multiplayerService.submitGesture(userId, request.matchId(), request.gesture());
            sendJson(exchange, 200, MatchResponse.from(state), objectMapper);
        }));

        server.createContext("/model/detect", exchange -> handleRequest(exchange, "POST", objectMapper, () -> {
            int userId = authenticate(exchange, tokenService);
            DetectRequest request = readJson(exchange, DetectRequest.class, objectMapper);
            InferenceService.DetectionResult result = inferenceService.detect(request.image());
            sendJson(exchange, 200, result, objectMapper);
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

    private static int requireQueryInt(HttpExchange exchange, String key) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Missing query parameter: " + key);
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                try {
                    return Integer.parseInt(kv[1]);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid integer for parameter: " + key);
                }
            }
        }
        throw new IllegalArgumentException("Missing query parameter: " + key);
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdminUserRequest(int userId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdminGameRequest(int gameId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GestureRequest(int matchId, String gesture) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DetectRequest(String image) {
    }

    private record MessageResponse(String message) {
    }

    private record MatchResponse(int matchId,
                                 MultiplayerService.MatchStatus status,
                                 int playerOneId,
                                 Integer playerTwoId,
                                 String playerOneGesture,
                                 String playerTwoGesture,
                                 String winner,
                                 Instant updatedAt) {
        static MatchResponse from(MultiplayerService.MatchState state) {
            return new MatchResponse(state.id(), state.status(), state.playerOneId(), state.playerTwoId(), state.playerOneGesture(), state.playerTwoGesture(), state.winner(), state.updatedAt());
        }
    }
}
