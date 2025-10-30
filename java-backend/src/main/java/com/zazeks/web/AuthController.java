package com.zazeks.web;

import com.zazeks.api.AuthService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthService.RegistrationResult> register(@RequestBody RegistrationRequest request) {
        AuthService.RegistrationResult result = authService.register(request.username(), request.password(), request.photo());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthService.LoginResult> login(@RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.username(), request.password());
        return ResponseEntity.ok(result);
    }

    public record RegistrationRequest(@NotBlank String username, @NotBlank String password, String photo) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
}
