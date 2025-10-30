package com.zazeks.app;

import com.zazeks.api.AuthService;
import com.zazeks.api.GameService;
import com.zazeks.api.MultiplayerService;
import com.zazeks.api.UserService;
import com.zazeks.api.admin.AdminService;
import com.zazeks.api.inference.ModelInferenceService;
import com.zazeks.api.multiplayer.MultiplayerResultService;
import com.zazeks.config.Settings;
import com.zazeks.database.InMemoryDatabase;
import com.zazeks.security.PasswordService;
import com.zazeks.security.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public InMemoryDatabase inMemoryDatabase() {
        return new InMemoryDatabase();
    }

    @Bean
    public PasswordService passwordService() {
        return new PasswordService();
    }

    @Bean
    public TokenService tokenService() {
        return new TokenService();
    }

    @Bean
    public AuthService authService(InMemoryDatabase database, PasswordService passwordService, TokenService tokenService) {
        return new AuthService(database, passwordService, tokenService);
    }

    @Bean
    public UserService userService(InMemoryDatabase database) {
        return new UserService(database);
    }

    @Bean
    public GameService gameService(InMemoryDatabase database) {
        return new GameService(database);
    }

    @Bean
    public AdminService adminService(InMemoryDatabase database) {
        return new AdminService(database);
    }

    @Bean
    public MultiplayerResultService multiplayerResultService(InMemoryDatabase database) {
        return new MultiplayerResultService(database);
    }

    @Bean
    public ModelInferenceService modelInferenceService() {
        return new ModelInferenceService();
    }

    @Bean
    public MultiplayerService multiplayerService(InMemoryDatabase database, ObjectMapper objectMapper) {
        return new MultiplayerService(database, objectMapper);
    }

    @Bean
    public Settings settings() {
        return Settings.getInstance();
    }
}
