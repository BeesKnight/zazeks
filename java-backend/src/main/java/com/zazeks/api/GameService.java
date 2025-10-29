package com.zazeks.api;

import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.Game;
import com.zazeks.database.models.User;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Java-аналог модуля {@code backend/src/api/game.py}.
 * Содержит бизнес-логику сохранения и получения игр.
 */
public class GameService {
    private static final Duration DUPLICATE_THRESHOLD = Duration.ofSeconds(10);
    private static final Map<String, String> RESULT_TRANSLATION = Map.of(
            "win", "win",
            "loss", "loss",
            "draw", "draw",
            "победа", "win",
            "поражение", "loss",
            "ничья", "draw"
    );

    private final InMemoryDatabase database;

    public GameService(InMemoryDatabase database) {
        this.database = database;
    }

    public Game createGame(int userId, String userChoice, String computerChoice, String result) {
        User user = database.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String normalizedResult = normalizeResult(result);
        database.findDuplicateGame(userId, userChoice, computerChoice, normalizedResult, DUPLICATE_THRESHOLD)
                .ifPresent(game -> {
                    throw new IllegalStateException("Duplicate game submission detected. Please wait before submitting again.");
                });

        Game game = new Game(userId, userChoice, computerChoice, normalizedResult);
        database.saveGame(game);

        user.incrementGamesPlayed();
        if ("win".equals(normalizedResult)) {
            user.incrementWins();
        }
        database.saveUser(user);
        return game;
    }

    public Game getGameById(int gameId, int requestingUserId) {
        Game game = database.findGameById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
        if (game.getUserId() != requestingUserId) {
            throw new SecurityException("Not enough privileges");
        }
        return game;
    }

    public List<Game> getGamesForUser(int userId, int requestingUserId) {
        if (userId != requestingUserId) {
            throw new SecurityException("Not enough privileges");
        }
        database.findUserById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return database.findGamesByUser(userId);
    }

    public int addWin(int userId, int requestingUserId) {
        if (userId != requestingUserId) {
            throw new SecurityException("Not enough privileges");
        }
        User user = database.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.incrementWins();
        database.saveUser(user);
        return user.getWins();
    }

    private String normalizeResult(String result) {
        if (result == null) {
            throw new IllegalArgumentException("Result is required");
        }
        return RESULT_TRANSLATION.getOrDefault(result.toLowerCase(Locale.ROOT).trim(), result);
    }
}
