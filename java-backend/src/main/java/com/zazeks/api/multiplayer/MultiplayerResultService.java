package com.zazeks.api.multiplayer;

import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.MultiplayerGame;
import com.zazeks.database.models.MultiplayerSession;
import com.zazeks.database.models.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class MultiplayerResultService {
    private final InMemoryDatabase database;

    public MultiplayerResultService(InMemoryDatabase database) {
        this.database = database;
    }

    public MultiplayerGame saveMultiplayerGame(int requesterId,
                                               int player1Id,
                                               int player2Id,
                                               String player1Gesture,
                                               String player2Gesture,
                                               String result,
                                               String sessionId) {
        if (requesterId != player1Id && requesterId != player2Id) {
            throw new SecurityException("Not authorized to save this game result");
        }
        if (!"player1".equals(result) && !"player2".equals(result) && !"draw".equals(result)) {
            throw new IllegalArgumentException("Invalid result value");
        }
        User player1 = database.findUserById(player1Id)
                .orElseThrow(() -> new NoSuchElementException("Player 1 not found"));
        User player2 = database.findUserById(player2Id)
                .orElseThrow(() -> new NoSuchElementException("Player 2 not found"));
        String normalizedP1 = player1Gesture.toLowerCase(Locale.ROOT);
        String normalizedP2 = player2Gesture.toLowerCase(Locale.ROOT);
        MultiplayerGame game = new MultiplayerGame(null, player1Id, player2Id,
                normalizedP1, normalizedP2, result, Instant.now());
        database.saveMultiplayerGame(game);

        player1.incrementOnlineGames();
        player2.incrementOnlineGames();
        if ("player1".equals(result)) {
            player1.incrementOnlineWins();
        } else if ("player2".equals(result)) {
            player2.incrementOnlineWins();
        }
        database.saveUser(player1);
        database.saveUser(player2);

        if (sessionId != null && !sessionId.isBlank()) {
            Optional<MultiplayerSession> sessionOptional = database.findMultiplayerSessionById(sessionId);
            sessionOptional.ifPresent(session -> {
                session.setGestures(normalizedP1, normalizedP2);
                session.setOutcome(result, switch (result) {
                    case "player1" -> String.valueOf(player1Id);
                    case "player2" -> String.valueOf(player2Id);
                    default -> null;
                });
                session.updateStatus(MultiplayerSession.Status.COMPLETED);
                database.saveMultiplayerSession(session);
            });
        }
        return game;
    }
}
