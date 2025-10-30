package com.zazeks.database.models;

import java.time.Instant;

public class MultiplayerSession {
    public enum Status {
        QUEUED,
        MATCHED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    private final String id;
    private final int player1Id;
    private final int player2Id;
    private final Instant createdAt;
    private Instant updatedAt;
    private Status status;
    private String player1Gesture;
    private String player2Gesture;
    private String lastResult;
    private String lastWinner;

    public MultiplayerSession(String id, int player1Id, int player2Id) {
        this.id = id;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.status = Status.MATCHED;
    }

    public String getId() {
        return id;
    }

    public int getPlayer1Id() {
        return player1Id;
    }

    public int getPlayer2Id() {
        return player2Id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Status getStatus() {
        return status;
    }

    public String getPlayer1Gesture() {
        return player1Gesture;
    }

    public String getPlayer2Gesture() {
        return player2Gesture;
    }

    public String getLastResult() {
        return lastResult;
    }

    public String getLastWinner() {
        return lastWinner;
    }

    public void updateStatus(Status status) {
        this.status = status;
        touch();
    }

    public void setGestures(String player1Gesture, String player2Gesture) {
        this.player1Gesture = player1Gesture;
        this.player2Gesture = player2Gesture;
        touch();
    }

    public void setOutcome(String lastResult, String lastWinner) {
        this.lastResult = lastResult;
        this.lastWinner = lastWinner;
        touch();
    }

    public void resetForReplay() {
        this.player1Gesture = null;
        this.player2Gesture = null;
        this.lastResult = null;
        this.lastWinner = null;
        updateStatus(Status.MATCHED);
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
