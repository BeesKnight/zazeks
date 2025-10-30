package com.zazeks.web;

import com.zazeks.api.AdminService;
import com.zazeks.database.models.DetectionMetadata;
import com.zazeks.database.models.MultiplayerSession;
import com.zazeks.security.AuthenticationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;
    private final AuthenticationService authenticationService;

    public AdminController(AdminService adminService, AuthenticationService authenticationService) {
        this.adminService = adminService;
        this.authenticationService = authenticationService;
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable("userId") int userId,
                                                          @RequestHeader("Authorization") String authorization) {
        authenticationService.requireAdmin(authorization);
        adminService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("msg", "User " + userId + " deleted successfully"));
    }

    @PostMapping("/admins")
    public ResponseEntity<Map<String, String>> addAdmin(@RequestHeader("Authorization") String authorization,
                                                        @RequestBody AdminRequest request) {
        authenticationService.requireAdmin(authorization);
        adminService.addAdmin(request.userId());
        return ResponseEntity.ok(Map.of("msg", "User " + request.userId() + " is now an admin"));
    }

    @DeleteMapping("/admins/{userId}")
    public ResponseEntity<Map<String, String>> deleteAdmin(@PathVariable("userId") int userId,
                                                           @RequestHeader("Authorization") String authorization) {
        authenticationService.requireAdmin(authorization);
        adminService.removeAdmin(userId);
        return ResponseEntity.ok(Map.of("msg", "Admin privileges revoked for user " + userId));
    }

    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<Map<String, String>> deleteGame(@PathVariable("gameId") int gameId,
                                                          @RequestHeader("Authorization") String authorization) {
        authenticationService.requireAdmin(authorization);
        adminService.deleteGame(gameId);
        return ResponseEntity.ok(Map.of("msg", "Game " + gameId + " deleted successfully"));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<MultiplayerSession>> listSessions(@RequestHeader("Authorization") String authorization) {
        authenticationService.requireAdmin(authorization);
        return ResponseEntity.ok(adminService.listMultiplayerSessions());
    }

    @GetMapping("/detections")
    public ResponseEntity<List<DetectionMetadata>> listDetections(@RequestHeader("Authorization") String authorization,
                                                                  @RequestParam(value = "userId", required = false) Integer userId) {
        authenticationService.requireAdmin(authorization);
        return ResponseEntity.ok(adminService.listDetections(userId));
    }

    @DeleteMapping("/users/{userId}/photo")
    public ResponseEntity<Map<String, String>> deleteUserPhoto(@PathVariable("userId") int userId,
                                                                @RequestHeader("Authorization") String authorization) {
        authenticationService.requireAdmin(authorization);
        adminService.deleteUserPhoto(userId);
        return ResponseEntity.ok(Map.of("msg", "Profile photo for user " + userId + " has been removed"));
    }

    @PutMapping("/users/{userId}/username")
    public ResponseEntity<Map<String, String>> changeUsername(@PathVariable("userId") int userId,
                                                               @RequestHeader("Authorization") String authorization,
                                                               @RequestBody UsernameRequest request) {
        authenticationService.requireAdmin(authorization);
        adminService.changeUsername(userId, request.newUsername());
        return ResponseEntity.ok(Map.of("msg", "Username changed successfully to " + request.newUsername()));
    }

    public record AdminRequest(int userId) {}

    public record UsernameRequest(@NotBlank String newUsername) {}
}
