package com.zazeks.security;

import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticationService {
    private final TokenService tokenService;
    private final InMemoryDatabase database;

    public AuthenticationService(TokenService tokenService, InMemoryDatabase database) {
        this.tokenService = tokenService;
        this.database = database;
    }

    public User requireUser(String authorizationHeader) {
        int userId = extractUserId(authorizationHeader);
        return database.findUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public User requireUser(String authorizationHeader, int expectedUserId) {
        User user = requireUser(authorizationHeader);
        if (user.getId() != expectedUserId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enough privileges");
        }
        return user;
    }

    public User requireAdmin(String authorizationHeader) {
        User user = requireUser(authorizationHeader);
        if (!user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enough privileges");
        }
        return user;
    }

    private int extractUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        String token = authorizationHeader.substring(7);
        try {
            Claims claims = tokenService.parseClaims(token);
            Object subject = claims.get("sub");
            if (subject == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token missing subject");
            }
            return Integer.parseInt(subject.toString());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }
}
