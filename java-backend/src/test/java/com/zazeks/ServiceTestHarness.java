package com.zazeks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zazeks.api.AdminService;
import com.zazeks.api.AuthService;
import com.zazeks.api.GameService;
import com.zazeks.api.InferenceService;
import com.zazeks.api.MultiplayerService;
import com.zazeks.api.UserService;
import com.zazeks.database.InMemoryDatabase;
import com.zazeks.ml.GestureDetector;
import com.zazeks.security.PasswordService;
import com.zazeks.security.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Shared fixture for service-layer unit tests.
 */
public abstract class ServiceTestHarness {
    protected InMemoryDatabase database;
    protected PasswordService passwordService;
    protected TokenService tokenService;
    protected AuthService authService;
    protected GameService gameService;
    protected UserService userService;
    protected MultiplayerService multiplayerService;
    protected AdminService adminService;
    protected InferenceService inferenceService;
    protected GestureDetector gestureDetector;

    @BeforeEach
    void setUpHarness() {
        database = new InMemoryDatabase();
        passwordService = new PasswordService();
        tokenService = new TokenService();
        authService = new AuthService(database, passwordService, tokenService);
        gameService = new GameService(database);
        userService = new UserService(database);
        multiplayerService = new MultiplayerService(database, new ObjectMapper());
        adminService = new AdminService(database);
        gestureDetector = createDetector();
        inferenceService = new InferenceService(database, gestureDetector);
    }

    protected GestureDetector createDetector() {
        return imageBytes -> new GestureDetector.Detection(
                "Stub",
                0.99,
                new GestureDetector.BoundingBox(1.0, 2.0, 3.0, 4.0)
        );
    }

    @AfterEach
    void tearDownHarness() {
        multiplayerService.shutdown();
        database.reset();
    }
}
