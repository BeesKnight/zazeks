package com.zazeks.api.admin;

import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.Admin;
import com.zazeks.database.models.User;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class AdminService {
    private final InMemoryDatabase database;

    public AdminService(InMemoryDatabase database) {
        this.database = database;
    }

    public void deleteUser(int userId) {
        User user = database.findUserById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        if (user.isAdmin()) {
            throw new IllegalStateException("Cannot delete admin user without revoking admin rights");
        }
        database.deleteUser(userId);
    }

    public Admin addAdmin(int userId) {
        User user = database.findUserById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        if (user.isAdmin()) {
            throw new IllegalStateException("User is already an admin");
        }
        return database.saveAdmin(new Admin(userId));
    }

    public void removeAdmin(int userId) {
        if (database.findAdminByUserId(userId).isEmpty()) {
            throw new NoSuchElementException("Admin record not found for this user");
        }
        database.deleteAdminByUserId(userId);
    }

    public void deleteGame(int gameId) {
        if (database.findGameById(gameId).isEmpty()) {
            throw new NoSuchElementException("Game not found");
        }
        database.deleteGame(gameId);
    }

    public void deleteUserPhoto(int userId) {
        User user = database.findUserById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        user.setPhoto(null);
        database.saveUser(user);
    }

    public void changeUsername(int userId, String newUsername) {
        if (database.findUserByUsername(newUsername).isPresent()) {
            throw new IllegalStateException("Username already taken");
        }
        User user = database.findUserById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        user.setUsername(newUsername);
        database.saveUser(user);
    }
}
