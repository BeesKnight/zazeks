package com.zazeks.web;

import com.zazeks.api.GameService;
import com.zazeks.database.models.Game;
import com.zazeks.security.AuthenticationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/games")
public class GameController {
    private final GameService gameService;
    private final AuthenticationService authenticationService;

    public GameController(GameService gameService, AuthenticationService authenticationService) {
        this.gameService = gameService;
        this.authenticationService = authenticationService;
    }

    @PostMapping
    public ResponseEntity<Game> createGame(@RequestHeader("Authorization") String authorization,
                                           @RequestBody GameRequest request) {
        int userId = authenticationService.requireUser(authorization).getId();
        Game game = gameService.createGame(userId, request.userChoice(), request.computerChoice(), request.result());
        return ResponseEntity.ok(game);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<Game> getGame(@PathVariable int gameId,
                                         @RequestHeader("Authorization") String authorization) {
        int userId = authenticationService.requireUser(authorization).getId();
        return ResponseEntity.ok(gameService.getGameById(gameId, userId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Game>> getGamesForUser(@PathVariable int userId,
                                                       @RequestHeader("Authorization") String authorization) {
        authenticationService.requireUser(authorization, userId);
        return ResponseEntity.ok(gameService.getGamesForUser(userId, userId));
    }

    @PutMapping("/add-win/{userId}")
    public ResponseEntity<Integer> addWin(@PathVariable int userId,
                                           @RequestHeader("Authorization") String authorization) {
        authenticationService.requireUser(authorization, userId);
        int wins = gameService.addWin(userId, userId);
        return ResponseEntity.ok(wins);
    }

    public record GameRequest(@NotBlank String userChoice,
                              @NotBlank String computerChoice,
                              @NotBlank String result) {}
}
