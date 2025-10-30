package com.zazeks.database;

import com.zazeks.database.models.Admin;
import com.zazeks.database.models.DetectionMetadata;
import com.zazeks.database.models.Game;
import com.zazeks.database.models.MultiplayerGame;
import com.zazeks.database.models.MultiplayerSession;
import com.zazeks.database.models.User;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Простая реализация persistent-слоя, имитирующая SessionLocal из Python-модуля
 * {@code backend/src/database/session.py}. Вместо реальной БД используется набор
 * потокобезопасных коллекций с авто-инкрементными идентификаторами.
 */
@Component
public class InMemoryDatabase {
    private final Map<Integer, User> users = new ConcurrentHashMap<>();
    private final Map<Integer, Game> games = new ConcurrentHashMap<>();
    private final Map<Integer, MultiplayerGame> multiplayerGames = new ConcurrentHashMap<>();
    private final Map<Integer, Admin> admins = new ConcurrentHashMap<>();
    private final Map<String, MultiplayerSession> multiplayerSessions = new ConcurrentHashMap<>();
    private final Map<Long, DetectionMetadata> detectionMetadata = new ConcurrentHashMap<>();

    private final AtomicInteger userIdSequence = new AtomicInteger(0);
    private final AtomicInteger gameIdSequence = new AtomicInteger(0);
    private final AtomicInteger multiplayerIdSequence = new AtomicInteger(0);
    private final AtomicInteger adminIdSequence = new AtomicInteger(0);
    private final AtomicLong detectionIdSequence = new AtomicLong(0);

    public User saveUser(User user) {
        if (user.getId() == null) {
            user.setId(userIdSequence.incrementAndGet());
        }
        users.put(user.getId(), user);
        return user;
    }

    public void deleteUser(int userId) {
        users.remove(userId);
        games.values().removeIf(game -> game.getUserId() == userId);
        multiplayerGames.values().removeIf(game -> game.getPlayer1Id() == userId || game.getPlayer2Id() == userId);
        admins.values().removeIf(admin -> admin.getUserId() == userId);
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

    public void deleteGame(int gameId) {
        games.remove(gameId);
    }

    public List<Game> findGamesByUser(int userId) {
        return games.values().stream()
                .filter(game -> game.getUserId() == userId)
                .sorted(Comparator.comparing(Game::getTimestamp))
                .collect(Collectors.toList());
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

    public List<MultiplayerGame> findMultiplayerGamesByUser(int userId) {
        return multiplayerGames.values().stream()
                .filter(game -> game.getPlayer1Id() == userId || game.getPlayer2Id() == userId)
                .sorted(Comparator.comparing(MultiplayerGame::getTimestamp))
                .collect(Collectors.toList());
    }

    public Optional<MultiplayerGame> findMultiplayerGameById(int id) {
        return Optional.ofNullable(multiplayerGames.get(id));
    }

    public MultiplayerSession saveMultiplayerSession(MultiplayerSession session) {
        session.touch();
        multiplayerSessions.put(session.getId(), session);
        return session;
    }

    public Optional<MultiplayerSession> findMultiplayerSessionById(String sessionId) {
        return Optional.ofNullable(multiplayerSessions.get(sessionId));
    }

    public List<MultiplayerSession> findMultiplayerSessionsByUser(int userId) {
        return multiplayerSessions.values().stream()
                .filter(session -> session.getPlayer1Id() == userId || session.getPlayer2Id() == userId)
                .sorted(Comparator.comparing(MultiplayerSession::getUpdatedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<MultiplayerSession> findAllMultiplayerSessions() {
        return new ArrayList<>(multiplayerSessions.values());
    }

    public void deleteMultiplayerSession(String sessionId) {
        multiplayerSessions.remove(sessionId);
    }

    public DetectionMetadata saveDetectionMetadata(DetectionMetadata metadata) {
        if (metadata.getId() == null) {
            metadata.setId(detectionIdSequence.incrementAndGet());
        }
        detectionMetadata.put(metadata.getId(), metadata);
        return metadata;
    }

    public List<DetectionMetadata> findDetectionMetadataByUser(Integer userId) {
        return detectionMetadata.values().stream()
                .filter(meta -> userId == null || userId.equals(meta.getUserId()))
                .sorted(Comparator.comparing(DetectionMetadata::getDetectedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<DetectionMetadata> findAllDetectionMetadata() {
        return new ArrayList<>(detectionMetadata.values());
    }

    public Admin saveAdmin(Admin admin) {
        if (admin.getId() == null) {
            admin.setId(adminIdSequence.incrementAndGet());
        }
        admins.put(admin.getUserId(), admin);
        findUserById(admin.getUserId()).ifPresent(user -> {
            user.setAdmin(true);
            users.put(user.getId(), user);
        });
        return admin;
    }

    public Optional<Admin> findAdminByUserId(int userId) {
        return Optional.ofNullable(admins.get(userId));
    }

    public void deleteAdminByUserId(int userId) {
        admins.remove(userId);
        findUserById(userId).ifPresent(user -> {
            user.setAdmin(false);
            users.put(user.getId(), user);
        });
    }

    public void reset() {
        users.clear();
        games.clear();
        multiplayerGames.clear();
        admins.clear();
        multiplayerSessions.clear();
        detectionMetadata.clear();
        userIdSequence.set(0);
        gameIdSequence.set(0);
        multiplayerIdSequence.set(0);
        adminIdSequence.set(0);
        detectionIdSequence.set(0);
    }
}
