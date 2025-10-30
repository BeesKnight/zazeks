package com.zazeks.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.MultiplayerGame;
import com.zazeks.database.models.MultiplayerSession;
import com.zazeks.database.models.User;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class MultiplayerService {
    private final InMemoryDatabase database;
    private final ObjectMapper objectMapper;
    private final ConcurrentLinkedQueue<PlayerConnection> waitingPlayers = new ConcurrentLinkedQueue<>();
    private final Map<String, MultiplayerMatch> activeMatches = new ConcurrentHashMap<>();
    private final Map<String, PlayerConnection> sessionToPlayer = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public MultiplayerService(InMemoryDatabase database, ObjectMapper objectMapper) {
        this.database = database;
        this.objectMapper = objectMapper;
    }

    public void handleMessage(WebSocketSession session, String payload) throws IOException {
        JsonNode node = objectMapper.readTree(payload);
        String action = node.path("action").asText();
        switch (action) {
            case "join" -> handleJoin(session, node);
            case "signal" -> handleSignal(session, node);
            case "ready" -> handleReady(session);
            case "unready" -> handleUnready(session);
            case "gesture" -> handleGesture(session, node);
            case "play_again" -> handlePlayAgain(session);
            default -> session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "action", "error",
                    "message", "Unknown action"
            ))));
        }
    }

    public void handleDisconnect(WebSocketSession session, CloseStatus status) {
        PlayerConnection player = sessionToPlayer.remove(session.getId());
        if (player == null) {
            waitingPlayers.removeIf(p -> p.session.getId().equals(session.getId()));
            return;
        }
        waitingPlayers.remove(player);
        Optional<MultiplayerMatch> matchOptional = activeMatches.values().stream()
                .filter(match -> match.contains(player))
                .findFirst();
        matchOptional.ifPresent(match -> {
            match.markDisconnected(player);
            String disconnectMessage = toJson(Map.of(
                    "action", "disconnect",
                    "message", "Игрок " + player.userId + " отключился."
            ));
            match.broadcastExcept(player, disconnectMessage);
            updateSession(match, sessionState -> sessionState.updateStatus(MultiplayerSession.Status.CANCELLED));
            activeMatches.remove(match.id());
        });
    }

    private void handleJoin(WebSocketSession session, JsonNode node) throws IOException {
        int userId = node.path("user_id").asInt(-1);
        if (userId <= 0) {
            session.sendMessage(new TextMessage(toJson(Map.of(
                    "action", "error",
                    "message", "user_id is required"
            ))));
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        PlayerConnection connection = new PlayerConnection(userId, session);
        sessionToPlayer.put(session.getId(), connection);
        waitingPlayers.add(connection);
        session.sendMessage(new TextMessage(toJson(Map.of(
                "action", "status",
                "message", "Вы в очереди на игру."
        ))));
        attemptMatchmaking();
    }

    private void attemptMatchmaking() {
        PlayerConnection player1 = waitingPlayers.poll();
        PlayerConnection player2 = waitingPlayers.poll();
        if (player1 == null) {
            return;
        }
        if (player2 == null) {
            waitingPlayers.add(player1);
            return;
        }
        MultiplayerMatch match = MultiplayerMatch.create(player1, player2);
        MultiplayerSession session = new MultiplayerSession(match.id(), player1.userId, player2.userId);
        database.saveMultiplayerSession(session);
        match.attachSession(session);
        activeMatches.put(match.id(), match);
        String matchMessage = toJson(Map.of(
                "action", "match_found",
                "match_id", match.id(),
                "players", List.of(player1.userId, player2.userId)
        ));
        match.broadcast(matchMessage);
    }

    private void handleSignal(WebSocketSession session, JsonNode node) throws IOException {
        PlayerConnection sender = sessionToPlayer.get(session.getId());
        if (sender == null) {
            return;
        }
        MultiplayerMatch match = findMatchByPlayer(sender);
        if (match == null) {
            return;
        }
        String data = node.path("data").toString();
        match.broadcastExcept(sender, toJson(Map.of(
                "action", "signal",
                "data", objectMapper.readTree(data)
        )));
    }

    private void handleReady(WebSocketSession session) throws IOException {
        PlayerConnection player = sessionToPlayer.get(session.getId());
        if (player == null) {
            return;
        }
        MultiplayerMatch match = findMatchByPlayer(player);
        if (match == null) {
            return;
        }
        match.markReady(player);
        if (match.allReady() && !match.isBattleStarted()) {
            match.setBattleStarted(true);
            match.setConcluded(false);
            updateSession(match, sessionState -> sessionState.updateStatus(MultiplayerSession.Status.IN_PROGRESS));
            String startMessage = toJson(Map.of(
                    "action", "battle_start",
                    "duration", 10
            ));
            match.broadcast(startMessage);
            scheduleBlackout(match);
        }
    }

    private void handleUnready(WebSocketSession session) throws IOException {
        PlayerConnection player = sessionToPlayer.get(session.getId());
        if (player == null) {
            return;
        }
        MultiplayerMatch match = findMatchByPlayer(player);
        if (match == null) {
            return;
        }
        match.markUnready(player);
        String message = toJson(Map.of(
                "action", "player_unready",
                "user_id", player.userId
        ));
        match.broadcast(message);
        updateSession(match, sessionState -> sessionState.updateStatus(MultiplayerSession.Status.MATCHED));
    }

    private void handleGesture(WebSocketSession session, JsonNode node) {
        PlayerConnection player = sessionToPlayer.get(session.getId());
        if (player == null) {
            return;
        }
        MultiplayerMatch match = findMatchByPlayer(player);
        if (match == null) {
            return;
        }
        String gesture = node.path("gesture").asText("none");
        String lastValid = node.path("lastValidGesture").asText(null);
        match.setGesture(player, gesture, lastValid);
    }

    private void handlePlayAgain(WebSocketSession session) throws IOException {
        PlayerConnection player = sessionToPlayer.get(session.getId());
        if (player == null) {
            return;
        }
        MultiplayerMatch match = findMatchByPlayer(player);
        if (match == null) {
            return;
        }
        match.markPlayAgain(player);
        match.broadcastExcept(player, toJson(Map.of(
                "action", "opponent_play_again",
                "message", "Игрок " + player.userId + " хочет сыграть ещё."
        )));
        if (match.readyForReplay()) {
            match.resetForReplay();
            match.broadcast(toJson(Map.of(
                    "action", "replay",
                    "message", "Начните новую битву, нажмите 'Готов'."
            )));
            updateSession(match, MultiplayerSession::resetForReplay);
        }
    }

    private void scheduleBlackout(MultiplayerMatch match) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            match.broadcast(toJson(Map.of(
                    "action", "blackout",
                    "duration", 3
            )));
            scheduler.schedule(() -> concludeBattle(match), 5, TimeUnit.SECONDS);
        }, 7, TimeUnit.SECONDS);
        match.setScheduledTask(future);
    }

    private void concludeBattle(MultiplayerMatch match) {
        if (match.isConcluded()) {
            return;
        }
        match.setConcluded(true);
        match.ensureGestures();
        PlayerConnection player1 = match.player1();
        PlayerConnection player2 = match.player2();
        String g1 = match.getGesture(player1);
        String g2 = match.getGesture(player2);
        String result = determineResult(g1, g2);
        String winnerId = switch (result) {
            case "win" -> String.valueOf(player1.userId);
            case "loss" -> String.valueOf(player2.userId);
            default -> "draw";
        };
        MultiplayerGame savedGame = saveMatch(match, g1, g2, winnerId);
        String winnerName = "draw";
        if (!"draw".equals(winnerId)) {
            try {
                winnerName = database.findUserById(Integer.parseInt(winnerId))
                        .map(User::getUsername)
                        .orElse(winnerId);
            } catch (NumberFormatException ignored) {
                winnerName = winnerId;
            }
        }
        match.broadcast(toJson(Map.of(
                "action", "battle_end",
                "winner", winnerName,
                "gestures", Map.of(
                        String.valueOf(player1.userId), g1,
                        String.valueOf(player2.userId), g2
                ),
                "game_id", savedGame.getId()
        )));
        match.setBattleStarted(false);
        updateSession(match, sessionState -> {
            sessionState.setGestures(normalizeGesture(g1), normalizeGesture(g2));
            String normalizedWinner = "draw".equals(winnerId) ? null : winnerId;
            sessionState.setOutcome(determineWinnerKey(match, winnerId), normalizedWinner);
            sessionState.updateStatus(MultiplayerSession.Status.COMPLETED);
        });
    }

    private String determineWinnerKey(MultiplayerMatch match, String winnerId) {
        if ("draw".equals(winnerId)) {
            return "draw";
        }
        if (String.valueOf(match.player1().userId).equals(winnerId)) {
            return "player1";
        }
        if (String.valueOf(match.player2().userId).equals(winnerId)) {
            return "player2";
        }
        return winnerId;
    }

    private MultiplayerGame saveMatch(MultiplayerMatch match, String g1, String g2, String winnerId) {
        String resultKey;
        if ("draw".equals(winnerId)) {
            resultKey = "draw";
        } else if (String.valueOf(match.player1().userId).equals(winnerId)) {
            resultKey = "player1";
        } else {
            resultKey = "player2";
        }
        MultiplayerGame saved = database.saveMultiplayerGame(new MultiplayerGame(
                match.player1().userId,
                match.player2().userId,
                normalizeGesture(g1),
                normalizeGesture(g2),
                resultKey
        ));
        updateOnlineStats(match.player1().userId, resultKey.equals("player1"));
        updateOnlineStats(match.player2().userId, resultKey.equals("player2"));
        return saved;
    }

    private void updateOnlineStats(int userId, boolean isWinner) {
        database.findUserById(userId).ifPresent(user -> {
            user.incrementOnlineGames();
            if (isWinner) {
                user.incrementOnlineWins();
            }
            database.saveUser(user);
        });
    }

    private MultiplayerMatch findMatchByPlayer(PlayerConnection player) {
        return activeMatches.values().stream()
                .filter(match -> match.contains(player))
                .findFirst()
                .orElse(null);
    }

    private String determineResult(String gesture1, String gesture2) {
        String g1 = gesture1 == null ? "none" : gesture1.trim().toLowerCase(Locale.ROOT);
        String g2 = gesture2 == null ? "none" : gesture2.trim().toLowerCase(Locale.ROOT);
        if (!"none".equals(g1) && "none".equals(g2)) {
            return "win";
        }
        if ("none".equals(g1) && !"none".equals(g2)) {
            return "loss";
        }
        if (g1.equals(g2)) {
            return "draw";
        }
        if (("rock".equals(g1) && "scissors".equals(g2)) ||
                ("scissors".equals(g1) && "paper".equals(g2)) ||
                ("paper".equals(g1) && "rock".equals(g2))) {
            return "win";
        }
        return "loss";
    }

    private String normalizeGesture(String gesture) {
        if (gesture == null) {
            return "none";
        }
        String trimmed = gesture.trim();
        return trimmed.isEmpty() ? "none" : trimmed.toLowerCase(Locale.ROOT);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void updateSession(MultiplayerMatch match, Consumer<MultiplayerSession> consumer) {
        MultiplayerSession session = match.session();
        if (session != null) {
            consumer.accept(session);
            database.saveMultiplayerSession(session);
        }
    }

    public record PlayerConnection(int userId, WebSocketSession session) {}

    private static class MultiplayerMatch {
        private final PlayerConnection player1;
        private final PlayerConnection player2;
        private final Map<Integer, Boolean> ready = new ConcurrentHashMap<>();
        private final Map<Integer, String> gestures = new ConcurrentHashMap<>();
        private final Set<Integer> playAgain = ConcurrentHashMap.newKeySet();
        private volatile boolean battleStarted;
        private volatile boolean concluded = false;
        private volatile ScheduledFuture<?> scheduledTask;
        private final String id;
        private volatile MultiplayerSession session;

        private MultiplayerMatch(PlayerConnection player1, PlayerConnection player2) {
            this.player1 = player1;
            this.player2 = player2;
            this.id = player1.userId + "_" + player2.userId;
        }

        public static MultiplayerMatch create(PlayerConnection p1, PlayerConnection p2) {
            MultiplayerMatch match = new MultiplayerMatch(p1, p2);
            match.ready.put(p1.userId, false);
            match.ready.put(p2.userId, false);
            return match;
        }

        public String id() {
            return id;
        }

        public PlayerConnection player1() {
            return player1;
        }

        public PlayerConnection player2() {
            return player2;
        }

        public void attachSession(MultiplayerSession session) {
            this.session = session;
        }

        public MultiplayerSession session() {
            return session;
        }

        public boolean contains(PlayerConnection player) {
            return player1.session.getId().equals(player.session.getId()) ||
                    player2.session.getId().equals(player.session.getId());
        }

        public void broadcast(String message) {
            sendSafe(player1.session, message);
            sendSafe(player2.session, message);
        }

        public void broadcastExcept(PlayerConnection excluded, String message) {
            if (!player1.session.getId().equals(excluded.session.getId())) {
                sendSafe(player1.session, message);
            }
            if (!player2.session.getId().equals(excluded.session.getId())) {
                sendSafe(player2.session, message);
            }
        }

        private void sendSafe(WebSocketSession session, String message) {
            synchronized (session) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException ignored) {
                }
            }
        }

        public void markReady(PlayerConnection player) {
            ready.put(player.userId, true);
        }

        public void markUnready(PlayerConnection player) {
            ready.put(player.userId, false);
        }

        public boolean allReady() {
            return ready.values().stream().allMatch(Boolean::booleanValue);
        }

        public boolean isBattleStarted() {
            return battleStarted;
        }

        public void setBattleStarted(boolean battleStarted) {
            this.battleStarted = battleStarted;
        }

        public void setConcluded(boolean concluded) {
            this.concluded = concluded;
        }

        public boolean isConcluded() {
            return concluded;
        }

        public void setGesture(PlayerConnection player, String gesture, String lastValid) {
            if (!"none".equalsIgnoreCase(gesture)) {
                gestures.put(player.userId, gesture);
            } else if (lastValid != null && !"none".equalsIgnoreCase(lastValid)) {
                gestures.put(player.userId, lastValid);
            } else {
                gestures.putIfAbsent(player.userId, "none");
            }
        }

        public void markPlayAgain(PlayerConnection player) {
            playAgain.add(player.userId);
        }

        public boolean readyForReplay() {
            return playAgain.size() == 2;
        }

        public void resetForReplay() {
            ready.replaceAll((k, v) -> false);
            gestures.clear();
            playAgain.clear();
            battleStarted = false;
            concluded = false;
        }

        public void ensureGestures() {
            gestures.putIfAbsent(player1.userId, "none");
            gestures.putIfAbsent(player2.userId, "none");
        }

        public String getGesture(PlayerConnection player) {
            return gestures.getOrDefault(player.userId, "none");
        }

        public void setScheduledTask(ScheduledFuture<?> scheduledTask) {
            this.scheduledTask = scheduledTask;
        }

        public void markDisconnected(PlayerConnection player) {
            Optional.ofNullable(scheduledTask).ifPresent(task -> task.cancel(true));
            ready.put(player.userId, false);
            battleStarted = false;
            concluded = true;
        }
    }
}
