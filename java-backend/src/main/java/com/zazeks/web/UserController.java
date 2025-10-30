package com.zazeks.web;

import com.zazeks.api.UserService;
import com.zazeks.api.UserService.UpdateResult;
import com.zazeks.api.UserService.UserProfile;
import com.zazeks.api.UserService.UserSummary;
import com.zazeks.database.models.User;
import com.zazeks.security.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final AuthenticationService authenticationService;

    public UserController(UserService userService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/leaderboard/offline")
    public ResponseEntity<List<UserSummary>> getOfflineLeaderboard() {
        return ResponseEntity.ok(userService.getOfflineLeaderboard());
    }

    @GetMapping("/leaderboard/online")
    public ResponseEntity<List<UserSummary>> getOnlineLeaderboard() {
        return ResponseEntity.ok(userService.getOnlineLeaderboard());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable("userId") int userId,
                                                       @RequestHeader("Authorization") String authorization) {
        User user = authenticationService.requireUser(authorization, userId);
        return ResponseEntity.ok(userService.getUserProfile(user.getId(), user.getId()));
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<Map<String, String>> getUserAvatar(@PathVariable("userId") int userId,
                                                              @RequestHeader("Authorization") String authorization) {
        authenticationService.requireUser(authorization);
        UserProfile profile = userService.getUserProfile(userId, userId);
        return ResponseEntity.ok(Map.of("photo", profile.photo()));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UpdateResult> updateProfile(@PathVariable("userId") int userId,
                                                       @RequestHeader("Authorization") String authorization,
                                                       @RequestBody UpdateRequest request) {
        authenticationService.requireUser(authorization, userId);
        UpdateResult result = userService.updateUserProfile(userId, userId, request.username(), request.photo());
        return ResponseEntity.ok(result);
    }

    public record UpdateRequest(String username, String photo) {}
}
