package com.zazeks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zazeks.api.AdminService;
import com.zazeks.api.AuthService;
import com.zazeks.api.GameService;
import com.zazeks.api.InferenceService;
import com.zazeks.api.MultiplayerService;
import com.zazeks.api.UserService;
import com.zazeks.database.InMemoryDatabase;
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
        inferenceService = new InferenceService(database);
    }

    @AfterEach
    void tearDownHarness() {
        multiplayerService.shutdown();
        database.reset();
    }
}
