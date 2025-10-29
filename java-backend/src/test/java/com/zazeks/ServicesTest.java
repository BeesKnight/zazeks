package com.zazeks;

import com.zazeks.api.AuthService;
import com.zazeks.api.GameService;
import com.zazeks.api.UserService;
import com.zazeks.api.UserService.UserProfile;
import com.zazeks.api.UserService.UserSummary;
import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.Game;
import com.zazeks.security.PasswordService;
import com.zazeks.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Набор модульных тестов, проверяющих корректность Java-реализаций
 * относительно поведения исходных Python-сервисов.
 */
class ServicesTest {
    private InMemoryDatabase database;
    private PasswordService passwordService;
    private TokenService tokenService;
    private AuthService authService;
    private GameService gameService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        database = new InMemoryDatabase();
        passwordService = new PasswordService();
        tokenService = new TokenService();
        authService = new AuthService(database, passwordService, tokenService);
        gameService = new GameService(database);
        userService = new UserService(database);
    }

    @Test
    void registerAndLoginUser() {
        AuthService.RegistrationResult registration = authService.register("player1", "pass_123", null);
        assertNotNull(registration);
        assertEquals(1, registration.userId());

        AuthService.LoginResult login = authService.login("player1", "pass_123");
        assertEquals("bearer", login.tokenType());
        assertEquals(1, login.userId());
        assertNotNull(login.accessToken());
    }

    @Test
    void loginFailsWithWrongPassword() {
        authService.register("player1", "pass_123", null);
        assertThrows(IllegalArgumentException.class, () -> authService.login("player1", "wrong"));
    }

    @Test
    void createGameUpdatesStatisticsAndPreventsDuplicates() {
        int userId = authService.register("player1", "pass_123", null).userId();
        Game first = gameService.createGame(userId, "rock", "scissors", "win");
        assertEquals("win", first.getResult());

        UserProfile profile = userService.getUserProfile(userId, userId);
        assertEquals(1, profile.wins());
        assertEquals(1, profile.gamesPlayed());

        assertThrows(IllegalStateException.class, () -> gameService.createGame(userId, "rock", "scissors", "win"));
    }

    @Test
    void leaderboardSortsByWins() {
        int user1 = authService.register("alice", "pass_123", null).userId();
        int user2 = authService.register("bob_1", "pass_123", null).userId();

        gameService.createGame(user1, "rock", "scissors", "win");
        gameService.createGame(user1, "paper", "rock", "win");
        gameService.createGame(user2, "rock", "scissors", "win");

        List<UserSummary> leaderboard = userService.getOfflineLeaderboard();
        assertEquals("alice", leaderboard.get(0).username());
        assertEquals("bob_1", leaderboard.get(1).username());
    }

    @Test
    void updateUserProfileValidatesImageSize() {
        int userId = authService.register("player1", "pass_123", null).userId();
        String smallPng = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg==";
        UserService.UpdateResult result = userService.updateUserProfile(userId, userId, "player1_renamed", smallPng);
        assertEquals("player1_renamed", result.user().username());

        String largeImage = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(new byte[(int) (5 * 1024 * 1024) + 1]);
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserProfile(userId, userId, null, largeImage));
    }

    @Test
    void addWinRequiresOwnership() {
        int userId = authService.register("player1", "pass_123", null).userId();
        int otherUser = authService.register("player2", "pass_123", null).userId();
        assertThrows(SecurityException.class, () -> gameService.addWin(userId, otherUser));
    }
}
