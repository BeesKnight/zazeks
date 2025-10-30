package com.zazeks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zazeks.support.StubWebSocketSession;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MultiplayerServiceTest extends ServiceTestHarness {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void playersMatchFromQueueAndCreateSession() throws IOException {
        int userA = authService.register("queue_a", "pass_123", null).userId();
        int userB = authService.register("queue_b", "pass_123", null).userId();
        StubWebSocketSession sessionA = new StubWebSocketSession();
        StubWebSocketSession sessionB = new StubWebSocketSession();

        sendJoin(sessionA, userA);
        sendJoin(sessionB, userB);

        assertEquals(2, sessionA.getSentMessages().size());
        assertEquals(2, sessionB.getSentMessages().size());

        JsonNode matchMessage = mapper.readTree(sessionA.getLastMessage().getPayload());
        assertEquals("match_found", matchMessage.get("action").asText());
        assertEquals(1, database.findAllMultiplayerSessions().size());
    }

    @Test
    void gesturesResolveBattleAndUpdateOnlineStats() throws Exception {
        int userA = authService.register("battle_a", "pass_123", null).userId();
        int userB = authService.register("battle_b", "pass_123", null).userId();
        StubWebSocketSession sessionA = new StubWebSocketSession();
        StubWebSocketSession sessionB = new StubWebSocketSession();

        sendJoin(sessionA, userA);
        sendJoin(sessionB, userB);

        sendReady(sessionA);
        sendReady(sessionB);

        sendGesture(sessionA, "rock");
        sendGesture(sessionB, "scissors");

        Object match = extractActiveMatch();
        Method conclude = multiplayerService.getClass().getDeclaredMethod("concludeBattle", match.getClass());
        conclude.setAccessible(true);
        conclude.invoke(multiplayerService, match);

        JsonNode last = mapper.readTree(sessionA.getLastMessage().getPayload());
        assertEquals("battle_end", last.get("action").asText());
        assertEquals("battle_a", last.get("winner").asText());

        assertEquals(1, database.findUserById(userA).orElseThrow().getOnlineWins());
        assertEquals(1, database.findUserById(userA).orElseThrow().getOnlineGames());
        assertEquals(0, database.findUserById(userB).orElseThrow().getOnlineWins());
        assertEquals(1, database.findUserById(userB).orElseThrow().getOnlineGames());

        multiplayerService.handleDisconnect(sessionA, CloseStatus.NORMAL);
    }

    private void sendJoin(StubWebSocketSession session, int userId) throws IOException {
        multiplayerService.handleMessage(session, mapper.writeValueAsString(Map.of(
                "action", "join",
                "user_id", userId
        )));
    }

    private void sendReady(StubWebSocketSession session) throws IOException {
        multiplayerService.handleMessage(session, mapper.writeValueAsString(Map.of(
                "action", "ready"
        )));
    }

    private void sendGesture(StubWebSocketSession session, String gesture) throws IOException {
        multiplayerService.handleMessage(session, mapper.writeValueAsString(Map.of(
                "action", "gesture",
                "gesture", gesture
        )));
    }

    private Object extractActiveMatch() throws Exception {
        Field field = multiplayerService.getClass().getDeclaredField("activeMatches");
        field.setAccessible(true);
        Map<?, ?> matches = (Map<?, ?>) field.get(multiplayerService);
        assertFalse(matches.isEmpty(), "Expected an active match");
        return matches.values().iterator().next();
    }
}
