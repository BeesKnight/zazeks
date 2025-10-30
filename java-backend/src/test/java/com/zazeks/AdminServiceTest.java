package com.zazeks;

import com.zazeks.api.AdminService;
import com.zazeks.database.models.Game;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceTest extends ServiceTestHarness {

    @Test
    void addAdminRejectsDuplicates() {
        int userId = authService.register("admin_user", "pass_123", null).userId();
        AdminService admin = adminService;
        admin.addAdmin(userId);
        assertThrows(IllegalStateException.class, () -> admin.addAdmin(userId));
    }

    @Test
    void deleteUserFailsWhileAdmin() {
        int userId = authService.register("protected", "pass_123", null).userId();
        adminService.addAdmin(userId);
        assertThrows(IllegalStateException.class, () -> adminService.deleteUser(userId));
    }

    @Test
    void changeUsernameRequiresUniqueness() {
        int userId = authService.register("rename_me", "pass_123", null).userId();
        authService.register("taken_name", "pass_123", null);
        assertThrows(IllegalStateException.class, () -> adminService.changeUsername(userId, "taken_name"));
    }

    @Test
    void deleteGameRemovesRecords() {
        int userId = authService.register("player_admin", "pass_123", null).userId();
        Game game = new Game(null, userId, "rock", "scissors", "win", Instant.now());
        database.saveGame(game);
        int gameId = game.getId();
        adminService.deleteGame(gameId);
        assertTrue(database.findGameById(gameId).isEmpty());
    }
}
