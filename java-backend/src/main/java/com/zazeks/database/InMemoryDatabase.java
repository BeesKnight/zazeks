package com.zazeks.database;

import com.zazeks.database.models.Game;
import com.zazeks.database.models.MultiplayerGame;
import com.zazeks.database.models.User;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Простая реализация persistent-слоя, имитирующая SessionLocal из Python-модуля
 * {@code backend/src/database/session.py}. Вместо реальной БД используется набор
 * потокобезопасных коллекций с авто-инкрементными идентификаторами.
 */
public class InMemoryDatabase {
    private final Map<Integer, User> users = new ConcurrentHashMap<>();
    private final Map<Integer, Game> games = new ConcurrentHashMap<>();
    private final Map<Integer, MultiplayerGame> multiplayerGames = new ConcurrentHashMap<>();

    private final AtomicInteger userIdSequence = new AtomicInteger(0);
    private final AtomicInteger gameIdSequence = new AtomicInteger(0);
    private final AtomicInteger multiplayerIdSequence = new AtomicInteger(0);

    public User saveUser(User user) {
        if (user.getId() == null) {
            user.setId(userIdSequence.incrementAndGet());
        }
        users.put(user.getId(), user);
        return user;
    }

    public Optional<User> findUserById(int id) {
        return Optional.ofNullable(users.get(id));
    }

    public Optional<User> findUserByUsername(String username) {
        return users.values().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public List<User> findAllUsers() {
        return new ArrayList<>(users.values());
    }

    public Game saveGame(Game game) {
        if (game.getId() == null) {
            game.setId(gameIdSequence.incrementAndGet());
        }
        games.put(game.getId(), game);
        return game;
    }

    public Optional<Game> findGameById(int id) {
        return Optional.ofNullable(games.get(id));
    }

    public List<Game> findGamesByUser(int userId) {
        return games.values().stream()
                .filter(game -> game.getUserId() == userId)
                .sorted(Comparator.comparing(Game::getTimestamp))
                .collect(Collectors.toList());
    }

    public List<Game> findAllGames() {
        return new ArrayList<>(games.values());
    }

    public void deleteGame(int gameId) {
        games.remove(gameId);
    }

    public void deleteGamesByUser(int userId) {
        games.values().removeIf(game -> game.getUserId() == userId);
    }

    public Optional<Game> findDuplicateGame(int userId, String userChoice, String computerChoice, String result, Duration threshold) {
        Instant cutoff = Instant.now().minus(threshold);
        return games.values().stream()
                .filter(game -> game.getUserId() == userId)
                .filter(game -> game.getUserChoice().equals(userChoice))
                .filter(game -> game.getComputerChoice().equals(computerChoice))
                .filter(game -> game.getResult().equals(result))
                .filter(game -> game.getTimestamp().isAfter(cutoff))
                .findFirst();
    }

    public MultiplayerGame saveMultiplayerGame(MultiplayerGame game) {
        if (game.getId() == null) {
            game.setId(multiplayerIdSequence.incrementAndGet());
        }
        multiplayerGames.put(game.getId(), game);
        return game;
    }

    public Optional<MultiplayerGame> findMultiplayerGameById(int id) {
        return Optional.ofNullable(multiplayerGames.get(id));
    }

    public List<MultiplayerGame> findAllMultiplayerGames() {
        return new ArrayList<>(multiplayerGames.values());
    }

    public void deleteMultiplayerGamesByUser(int userId) {
        multiplayerGames.values().removeIf(game -> game.getPlayer1Id() == userId || game.getPlayer2Id() == userId);
    }

    public void deleteUser(int userId) {
        users.remove(userId);
        deleteGamesByUser(userId);
        deleteMultiplayerGamesByUser(userId);
    }

    public void reset() {
        users.clear();
        games.clear();
        multiplayerGames.clear();
        userIdSequence.set(0);
        gameIdSequence.set(0);
        multiplayerIdSequence.set(0);
    }
}
