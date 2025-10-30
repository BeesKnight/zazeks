package com.zazeks;

import com.zazeks.api.AuthService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest extends ServiceTestHarness {

    @Test
    void registrationRejectsInvalidUsernames() {
        assertThrows(IllegalArgumentException.class, () -> authService.register("abc", "valid_1", null));
        assertThrows(IllegalArgumentException.class, () -> authService.register("invalid!", "valid_1", null));
    }

    @Test
    void registrationRejectsInvalidPasswords() {
        assertThrows(IllegalArgumentException.class, () -> authService.register("valid_1", "bad", null));
        assertThrows(IllegalArgumentException.class, () -> authService.register("valid_1", "with-hyphen", null));
    }

    @Test
    void registrationRejectsDuplicateUsernamesIgnoringCase() {
        AuthService.RegistrationResult registration = authService.register("PlayerOne", "Pass_123", null);
        assertNotNull(registration);
        assertThrows(IllegalStateException.class, () -> authService.register("playerone", "Pass_123", null));
    }

    @Test
    void loginFailsForUnknownUser() {
        assertThrows(IllegalArgumentException.class, () -> authService.login("ghost", "pass"));
    }

    @Test
    void loginReturnsJwtContainingSubject() {
        int userId = authService.register("player1", "pass_123", null).userId();
        AuthService.LoginResult login = authService.login("player1", "pass_123");
        assertEquals("bearer", login.tokenType());
        assertEquals(userId, tokenService.extractUserId(login.accessToken()));
    }
}
