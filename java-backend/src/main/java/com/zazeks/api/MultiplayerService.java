package com.zazeks.api;

import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.MultiplayerGame;
import com.zazeks.database.models.User;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Упрощённая HTTP-реализация очереди и синхронизации мультиигр.
 */
public class MultiplayerService {
    private static final Set<String> ALLOWED_GESTURES = Set.of("rock", "paper", "scissors");
    private final InMemoryDatabase database;
    private final Queue<Integer> waitingQueue = new ConcurrentLinkedQueue<>();
    private final Map<Integer, MatchState> matches = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> userToMatch = new ConcurrentHashMap<>();
    private final AtomicInteger matchIdSequence = new AtomicInteger();

    public MultiplayerService(InMemoryDatabase database) {
        this.database = database;
    }

    public MatchState joinQueue(int userId) {
        ensureUserExists(userId);
        Integer currentMatch = userToMatch.get(userId);
        if (currentMatch != null) {
            return matches.get(currentMatch);
        }
        waitingQueue.removeIf(id -> !database.findUserById(id).isPresent());
        Integer opponentId;
        while (true) {
            opponentId = waitingQueue.poll();
            if (opponentId == null || opponentId == userId) {
                break;
            }
            if (database.findUserById(opponentId).isPresent()) {
                break;
            }
        }
        if (opponentId == null || opponentId == userId) {
            waitingQueue.offer(userId);
            MatchState state = MatchState.waiting(userId);
            matches.put(state.id(), state);
            userToMatch.put(userId, state.id());
            return state;
        }
        Integer opponentMatchId = userToMatch.get(opponentId);
        if (opponentMatchId != null) {
            matches.remove(opponentMatchId);
        }
        int matchId = matchIdSequence.incrementAndGet();
        MatchState match = MatchState.ready(matchId, opponentId, userId);
        matches.put(matchId, match);
        userToMatch.put(userId, matchId);
        userToMatch.put(opponentId, matchId);
        return match;
    }

    public MatchState submitGesture(int userId, int matchId, String gesture) {
        ensureUserExists(userId);
        MatchState match = matches.get(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Match not found");
        }
        if (match.status() == MatchStatus.COMPLETED) {
            return match;
        }
        if (!match.containsPlayer(userId)) {
            throw new SecurityException("You are not part of this match");
        }
        String normalized = normalizeGesture(gesture);
        MatchState updated = match.recordGesture(userId, normalized);
        matches.put(matchId, updated);
        userToMatch.put(userId, matchId);
        if (updated.status() == MatchStatus.COMPLETED && match.status() != MatchStatus.COMPLETED) {
            persistResult(updated);
        }
        return updated;
    }

    public MatchState getMatch(int userId, int matchId) {
        MatchState match = matches.get(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Match not found");
        }
        if (!match.containsPlayer(userId)) {
            throw new SecurityException("You are not part of this match");
        }
        return match;
    }

    public void leaveQueue(int userId) {
        Integer matchId = userToMatch.remove(userId);
        if (matchId == null) {
            waitingQueue.remove(userId);
            return;
        }
        MatchState match = matches.get(matchId);
        if (match == null) {
            waitingQueue.remove(userId);
            return;
        }
        if (match.status() == MatchStatus.WAITING_FOR_OPPONENT) {
            matches.remove(matchId);
            waitingQueue.removeIf(id -> java.util.Objects.equals(id, userId) || java.util.Objects.equals(id, match.playerOneId()));
        }
    }

    private void ensureUserExists(int userId) {
        database.findUserById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private String normalizeGesture(String gesture) {
        if (gesture == null) {
            throw new IllegalArgumentException("Gesture is required");
        }
        String normalized = gesture.trim().toLowerCase();
        if (!ALLOWED_GESTURES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported gesture: " + gesture);
        }
        return normalized;
    }

    private void persistResult(MatchState match) {
        String winner = match.winner();
        if (winner == null) {
            return;
        }
        User playerOne = database.findUserById(match.playerOneId()).orElseThrow();
        User playerTwo = database.findUserById(match.playerTwoId()).orElseThrow();

        playerOne.incrementOnlineGames();
        playerTwo.incrementOnlineGames();
        switch (winner) {
            case "player1" -> playerOne.incrementOnlineWins();
            case "player2" -> playerTwo.incrementOnlineWins();
            default -> {
            }
        }
        database.saveUser(playerOne);
        database.saveUser(playerTwo);
        MultiplayerGame game = new MultiplayerGame(match.playerOneId(), match.playerTwoId(), match.playerOneGesture(), match.playerTwoGesture(), winner);
        database.saveMultiplayerGame(game);
    }

    public enum MatchStatus {
        WAITING_FOR_OPPONENT,
        READY,
        COMPLETED
    }

    public record MatchState(int id,
                             MatchStatus status,
                             int playerOneId,
                             Integer playerTwoId,
                             String playerOneGesture,
                             String playerTwoGesture,
                             String winner,
                             Instant updatedAt) {

        static MatchState waiting(int playerId) {
            int id = playerId * -1;
            return new MatchState(id, MatchStatus.WAITING_FOR_OPPONENT, playerId, null, null, null, null, Instant.now());
        }

        static MatchState ready(int id, int playerOneId, int playerTwoId) {
            return new MatchState(id, MatchStatus.READY, playerOneId, playerTwoId, null, null, null, Instant.now());
        }

        MatchState recordGesture(int playerId, String gesture) {
            if (status == MatchStatus.WAITING_FOR_OPPONENT) {
                return this;
            }
            boolean isPlayerOne = playerId == playerOneId;
            boolean isPlayerTwo = playerTwoId != null && playerId == playerTwoId;
            if (!isPlayerOne && !isPlayerTwo) {
                throw new SecurityException("User not part of this match");
            }
            String newPlayerOneGesture = isPlayerOne ? gesture : playerOneGesture;
            String newPlayerTwoGesture = isPlayerTwo ? gesture : playerTwoGesture;
            MatchStatus newStatus = status;
            String newWinner = winner;
            if (newPlayerOneGesture != null && newPlayerTwoGesture != null) {
                newStatus = MatchStatus.COMPLETED;
                newWinner = resolveWinner(newPlayerOneGesture, newPlayerTwoGesture);
            } else if (status == MatchStatus.READY) {
                newStatus = MatchStatus.READY;
            }
            return new MatchState(id, newStatus, playerOneId, playerTwoId, newPlayerOneGesture, newPlayerTwoGesture, newWinner, Instant.now());
        }

        boolean containsPlayer(int userId) {
            return userId == playerOneId || (playerTwoId != null && userId == playerTwoId);
        }

        private String resolveWinner(String p1, String p2) {
            if (p1.equals(p2)) {
                return "draw";
            }
            if ((p1.equals("rock") && p2.equals("scissors"))
                    || (p1.equals("scissors") && p2.equals("paper"))
                    || (p1.equals("paper") && p2.equals("rock"))) {
                return "player1";
            }
            return "player2";
        }
    }
}
