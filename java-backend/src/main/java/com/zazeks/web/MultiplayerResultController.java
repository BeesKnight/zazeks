package com.zazeks.web;

import com.zazeks.api.multiplayer.MultiplayerResultService;
import com.zazeks.database.models.MultiplayerGame;
import com.zazeks.security.AuthenticationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/multiplayer")
public class MultiplayerResultController {
    private final MultiplayerResultService multiplayerResultService;
    private final AuthenticationService authenticationService;

    public MultiplayerResultController(MultiplayerResultService multiplayerResultService,
                                       AuthenticationService authenticationService) {
        this.multiplayerResultService = multiplayerResultService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/result")
    public ResponseEntity<Map<String, Object>> saveResult(@RequestHeader("Authorization") String authorization,
                                                           @RequestBody MultiplayerResultRequest request) {
        int requesterId = authenticationService.requireUser(authorization).getId();
        MultiplayerGame game = multiplayerResultService.saveMultiplayerGame(
                requesterId,
                request.player1Id(),
                request.player2Id(),
                request.player1Gesture(),
                request.player2Gesture(),
                request.result()
        );
        return ResponseEntity.ok(Map.of(
                "msg", "Multiplayer game result saved successfully",
                "game_id", game.getId()
        ));
    }

    public record MultiplayerResultRequest(int player1Id,
                                           int player2Id,
                                           @NotBlank String player1Gesture,
                                           @NotBlank String player2Gesture,
                                           @NotBlank String result) {}
}
