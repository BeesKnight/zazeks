package com.zazeks.api.multiplayer;

import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.MultiplayerGame;
import com.zazeks.database.models.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;

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
                                               String result) {
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
        MultiplayerGame game = new MultiplayerGame(null, player1Id, player2Id,
                player1Gesture.toLowerCase(), player2Gesture.toLowerCase(), result, Instant.now());
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
        return game;
    }
}
