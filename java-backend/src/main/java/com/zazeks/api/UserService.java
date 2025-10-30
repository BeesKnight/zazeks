package com.zazeks.api;

import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.User;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Java-аналог модуля {@code backend/src/api/user.py}.
 */
@Service
public class UserService {
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    private final InMemoryDatabase database;

    public UserService(InMemoryDatabase database) {
        this.database = database;
    }

    public List<UserSummary> getOfflineLeaderboard() {
        return database.findAllUsers().stream()
                .sorted(Comparator.comparingInt(User::getWins).reversed())
                .map(user -> new UserSummary(user.getUsername(), user.getPhoto(), user.getWins(), user.getOnlineWins()))
                .collect(Collectors.toList());
    }

    public List<UserSummary> getOnlineLeaderboard() {
        return database.findAllUsers().stream()
                .sorted(Comparator.comparingInt(User::getOnlineWins).reversed())
                .map(user -> new UserSummary(user.getUsername(), user.getPhoto(), user.getWins(), user.getOnlineWins()))
                .collect(Collectors.toList());
    }

    public UserProfile getUserProfile(int userId, int requestingUserId) {
        if (userId != requestingUserId) {
            throw new SecurityException("You are not allowed to view this profile");
        }
        User user = database.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserProfile(user.getId(), user.getUsername(), user.getPhoto(), user.getWins(), user.getGamesPlayed(), user.getOnlineWins(), user.getOnlineGames());
    }

    public UpdateResult updateUserProfile(int userId, int requestingUserId, String username, String photo) {
        User user = database.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (userId != requestingUserId) {
            throw new SecurityException("Not enough privileges");
        }
        if (photo != null) {
            validatePhoto(photo);
            user.setPhoto(photo);
        }
        if (username != null) {
            database.findUserByUsername(username).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new IllegalStateException("Username already taken");
                }
            });
            user.setUsername(username);
        }
        database.saveUser(user);
        return new UpdateResult("User updated successfully", getUserProfile(userId, requestingUserId));
    }

    private void validatePhoto(String base64Image) {
        try {
            String[] parts = base64Image.split(",", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid image format");
            }
            String header = parts[0].toLowerCase(Locale.ROOT);
            if (!header.startsWith("data:image/")) {
                throw new IllegalArgumentException("Invalid image format");
            }
            String mimeType = header.substring("data:image/".length(), header.indexOf(";"));
            if (!mimeType.equals("png") && !mimeType.equals("jpeg") && !mimeType.equals("jpg")) {
                throw new IllegalArgumentException("Only PNG and JPG images are allowed");
            }
            byte[] decoded = Base64.getDecoder().decode(parts[1]);
            if (decoded.length > MAX_IMAGE_SIZE_BYTES) {
                throw new IllegalArgumentException("Image size exceeds 5MB");
            }
            if (ImageIO.read(new ByteArrayInputStream(decoded)) == null) {
                throw new IllegalArgumentException("Invalid image format");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid image format", e);
        }
    }

    public record UserSummary(String username, String photo, int wins, int onlineWins) {}

    public record UserProfile(Integer id, String username, String photo, int wins, int gamesPlayed, int onlineWins, int onlineGames) {}

    public record UpdateResult(String message, UserProfile user) {}
}
