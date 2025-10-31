package com.zazeks.api;

import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.Game;
import com.zazeks.database.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceTest {
    private InMemoryDatabase database;
    private AdminService adminService;
    private User admin;
    private User user;

    @BeforeEach
    void setUp() {
        database = new InMemoryDatabase();
        adminService = new AdminService(database);
        admin = new User("admin", "hash", null);
        admin.setAdmin(true);
        database.saveUser(admin);
        user = new User("player", "hash", null);
        database.saveUser(user);
    }

    @Test
    void grantAdminElevatesUser() {
        adminService.grantAdmin(admin.getId(), user.getId());
        assertTrue(database.findUserById(user.getId()).orElseThrow().isAdmin());
    }

    @Test
    void revokeAdminRemovesPrivileges() {
        adminService.grantAdmin(admin.getId(), user.getId());
        adminService.revokeAdmin(admin.getId(), user.getId());
        assertFalse(database.findUserById(user.getId()).orElseThrow().isAdmin());
    }

    @Test
    void deleteUserRemovesDataButProtectsSelf() {
        assertThrows(IllegalStateException.class, () -> adminService.deleteUser(admin.getId(), admin.getId()));
        adminService.deleteUser(admin.getId(), user.getId());
        assertTrue(database.findUserById(user.getId()).isEmpty());
    }

    @Test
    void deleteGameRequiresExistingRecord() {
        Game game = new Game(admin.getId(), "rock", "paper", "loss");
        database.saveGame(game);
        adminService.deleteGame(admin.getId(), game.getId());
        assertTrue(database.findGameById(game.getId()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> adminService.deleteGame(admin.getId(), game.getId()));
    }

    @Test
    void listUsersReturnsSnapshots() {
        adminService.grantAdmin(admin.getId(), user.getId());
        List<AdminService.UserSnapshot> snapshots = adminService.listUsers(admin.getId());
        assertEquals(2, snapshots.size());
        AdminService.UserSnapshot second = snapshots.stream()
                .filter(snapshot -> snapshot.id() == user.getId())
                .findFirst()
                .orElseThrow();
        assertTrue(second.admin());
    }
}
