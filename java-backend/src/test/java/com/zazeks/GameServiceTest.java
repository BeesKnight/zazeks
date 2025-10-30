package com.zazeks;

import com.zazeks.database.models.Game;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest extends ServiceTestHarness {

    @Test
    void createGamePreventsDuplicatesWithinThreshold() {
        int userId = authService.register("dupes", "pass_123", null).userId();
        gameService.createGame(userId, "rock", "scissors", "win");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                gameService.createGame(userId, "rock", "scissors", "win"));
        assertTrue(ex.getMessage().contains("Duplicate"));
    }

    @Test
    void createGameAllowsNewSubmissionAfterThreshold() {
        int userId = authService.register("threshold", "pass_123", null).userId();
        Game oldGame = new Game(null, userId, "rock", "scissors", "win", Instant.now().minusSeconds(60));
        database.saveGame(oldGame);
        assertDoesNotThrow(() -> gameService.createGame(userId, "rock", "scissors", "win"));
    }

    @Test
    void getGamesRequiresOwnership() {
        int userId = authService.register("owner", "pass_123", null).userId();
        int otherId = authService.register("other", "pass_123", null).userId();
        assertThrows(SecurityException.class, () -> gameService.getGamesForUser(userId, otherId));
    }
}
