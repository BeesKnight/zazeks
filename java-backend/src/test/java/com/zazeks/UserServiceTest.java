package com.zazeks;

import com.zazeks.api.UserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest extends ServiceTestHarness {

    @Test
    void profileUpdateRequiresOwnership() {
        int ownerId = authService.register("owner_1", "pass_123", null).userId();
        int attackerId = authService.register("attacker", "pass_123", null).userId();
        assertThrows(SecurityException.class, () -> userService.updateUserProfile(ownerId, attackerId, "new_name", null));
    }

    @Test
    void updateRejectsDuplicateUsername() {
        int userId = authService.register("first_user", "pass_123", null).userId();
        authService.register("other_user", "pass_123", null);
        assertThrows(IllegalStateException.class, () -> userService.updateUserProfile(userId, userId, "other_user", null));
    }

    @Test
    void updateRejectsInvalidImagePayload() {
        int userId = authService.register("image_user", "pass_123", null).userId();
        String invalid = "not-base64";
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserProfile(userId, userId, null, invalid));
    }

    @Test
    void updateAppliesChangesWhenValid() {
        int userId = authService.register("profile_user", "pass_123", null).userId();
        String photo = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQA" +
                "AAABJRU5ErkJggg==";
        UserService.UpdateResult result = userService.updateUserProfile(userId, userId, "profile_user2", photo);
        assertEquals("profile_user2", result.user().username());
        assertEquals(photo, result.user().photo());
    }

    @Test
    void leaderboardOrdersByOfflineAndOnlineWins() {
        int alice = authService.register("alice_1", "pass_123", null).userId();
        int bob = authService.register("bob_2", "pass_123", null).userId();
        int carol = authService.register("carol_3", "pass_123", null).userId();

        gameService.createGame(alice, "rock", "scissors", "win");
        gameService.createGame(alice, "paper", "rock", "win");
        gameService.createGame(bob, "rock", "scissors", "win");

        database.findUserById(carol).ifPresent(user -> {
            user.incrementOnlineWins();
            user.incrementOnlineGames();
            user.incrementOnlineWins();
            user.incrementOnlineGames();
            database.saveUser(user);
        });

        var offline = userService.getOfflineLeaderboard();
        assertEquals("alice_1", offline.get(0).username());
        assertEquals("bob_2", offline.get(1).username());

        var online = userService.getOnlineLeaderboard();
        assertEquals("carol_3", online.get(0).username());
        assertEquals(2, online.get(0).onlineWins());
    }
}
