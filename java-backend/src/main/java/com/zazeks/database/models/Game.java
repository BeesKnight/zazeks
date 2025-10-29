package com.zazeks.database.models;

import java.time.Instant;
import java.util.Objects;

/**
 * Java-аналог ORM-модели {@code Game} из {@code backend/src/database/models.py}.
 */
public class Game {
    private Integer id;
    private final int userId;
    private final String userChoice;
    private final String computerChoice;
    private final String result;
    private final Instant timestamp;

    public Game(Integer id, int userId, String userChoice, String computerChoice, String result, Instant timestamp) {
        this.id = id;
        this.userId = userId;
        this.userChoice = Objects.requireNonNull(userChoice);
        this.computerChoice = Objects.requireNonNull(computerChoice);
        this.result = Objects.requireNonNull(result);
        this.timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public Game(int userId, String userChoice, String computerChoice, String result) {
        this(null, userId, userChoice, computerChoice, result, Instant.now());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public String getUserChoice() {
        return userChoice;
    }

    public String getComputerChoice() {
        return computerChoice;
    }

    public String getResult() {
        return result;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
