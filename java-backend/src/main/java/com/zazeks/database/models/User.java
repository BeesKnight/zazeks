package com.zazeks.database.models;

import java.util.Objects;

/**
 * Java-аналог ORM-модели {@code User} из {@code backend/src/database/models.py}.
 */
public class User {
    private Integer id;
    private String username;
    private String passwordHash;
    private String photo;
    private int wins;
    private int gamesPlayed;
    private int onlineWins;
    private int onlineGames;
    private boolean admin;

    public User(String username, String passwordHash, String photo) {
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.photo = photo;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public int getWins() {
        return wins;
    }

    public void incrementWins() {
        this.wins += 1;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void incrementGamesPlayed() {
        this.gamesPlayed += 1;
    }

    public int getOnlineWins() {
        return onlineWins;
    }

    public void incrementOnlineWins() {
        this.onlineWins += 1;
    }

    public int getOnlineGames() {
        return onlineGames;
    }

    public void incrementOnlineGames() {
        this.onlineGames += 1;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
