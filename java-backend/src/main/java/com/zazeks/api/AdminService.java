package com.zazeks.api;

import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.Game;
import com.zazeks.database.models.User;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Административные операции: управление правами, пользователями и историями игр.
 */
public class AdminService {
    private final InMemoryDatabase database;

    public AdminService(InMemoryDatabase database) {
        this.database = database;
    }

    public void grantAdmin(int requesterId, int targetUserId) {
        User requester = requireAdmin(requesterId);
        User target = database.findUserById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));
        if (target.isAdmin()) {
            throw new IllegalStateException("User is already an administrator");
        }
        target.setAdmin(true);
        database.saveUser(target);
        database.saveUser(requester);
    }

    public void revokeAdmin(int requesterId, int targetUserId) {
        requireAdmin(requesterId);
        User target = database.findUserById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));
        if (!target.isAdmin()) {
            throw new IllegalStateException("User is not an administrator");
        }
        if (target.getId() == requesterId) {
            throw new IllegalStateException("Administrators cannot revoke their own rights");
        }
        target.setAdmin(false);
        database.saveUser(target);
    }

    public void deleteUser(int requesterId, int targetUserId) {
        requireAdmin(requesterId);
        if (requesterId == targetUserId) {
            throw new IllegalStateException("Administrators cannot delete themselves");
        }
        database.findUserById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));
        database.deleteUser(targetUserId);
    }

    public void deleteGame(int requesterId, int gameId) {
        requireAdmin(requesterId);
        database.findGameById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
        database.deleteGame(gameId);
    }

    public List<UserSnapshot> listUsers(int requesterId) {
        requireAdmin(requesterId);
        return database.findAllUsers().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(user -> new UserSnapshot(user.getId(), user.getUsername(), user.isAdmin(), user.getWins(), user.getGamesPlayed(), user.getOnlineWins(), user.getOnlineGames()))
                .collect(Collectors.toList());
    }

    public List<Game> listAllGames(int requesterId) {
        requireAdmin(requesterId);
        return database.findAllGames();
    }

    private User requireAdmin(int userId) {
        User user = database.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.isAdmin()) {
            throw new SecurityException("Administrator privileges required");
        }
        return user;
    }

    public record UserSnapshot(int id, String username, boolean admin, int wins, int gamesPlayed, int onlineWins, int onlineGames) {}
}
