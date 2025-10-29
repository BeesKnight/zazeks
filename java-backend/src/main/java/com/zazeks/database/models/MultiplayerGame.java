package com.zazeks.database.models;

import java.time.Instant;
import java.util.Objects;

/**
 * Java-аналог ORM-модели {@code MultiplayerGame} из {@code backend/src/database/models.py}.
 */
public class MultiplayerGame {
    private Integer id;
    private final int player1Id;
    private final int player2Id;
    private final String player1Gesture;
    private final String player2Gesture;
    private final String result;
    private final Instant timestamp;

    public MultiplayerGame(Integer id,
                            int player1Id,
                            int player2Id,
                            String player1Gesture,
                            String player2Gesture,
                            String result,
                            Instant timestamp) {
        this.id = id;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.player1Gesture = Objects.requireNonNull(player1Gesture);
        this.player2Gesture = Objects.requireNonNull(player2Gesture);
        this.result = Objects.requireNonNull(result);
        this.timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public MultiplayerGame(int player1Id, int player2Id, String player1Gesture, String player2Gesture, String result) {
        this(null, player1Id, player2Id, player1Gesture, player2Gesture, result, Instant.now());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getPlayer1Id() {
        return player1Id;
    }

    public int getPlayer2Id() {
        return player2Id;
    }

    public String getPlayer1Gesture() {
        return player1Gesture;
    }

    public String getPlayer2Gesture() {
        return player2Gesture;
    }

    public String getResult() {
        return result;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
