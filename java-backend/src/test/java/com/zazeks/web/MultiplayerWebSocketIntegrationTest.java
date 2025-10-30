package com.zazeks.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zazeks.app.Application;
import com.zazeks.database.InMemoryDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MultiplayerWebSocketIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryDatabase database;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setup() {
        database.reset();
    }

    @Test
    void multiplayerFlowProducesResult() throws Exception {
        int user1 = register("alpha", "secret");
        int user2 = register("beta", "secret");

        BlockingQueue<String> messages1 = new LinkedBlockingQueue<>();
        BlockingQueue<String> messages2 = new LinkedBlockingQueue<>();

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session1 = client.doHandshake(new CollectingHandler(messages1), null, URI.create(url()))
                .get(5, TimeUnit.SECONDS);
        WebSocketSession session2 = client.doHandshake(new CollectingHandler(messages2), null, URI.create(url()))
                .get(5, TimeUnit.SECONDS);

        session1.sendMessage(new TextMessage(joinPayload(user1)));
        session2.sendMessage(new TextMessage(joinPayload(user2)));

        awaitAction(messages1, "status");
        awaitAction(messages2, "status");
        JsonNode match1 = awaitAction(messages1, "match_found");
        JsonNode match2 = awaitAction(messages2, "match_found");
        assertThat(match1.path("match_id").asText()).isEqualTo(match2.path("match_id").asText());

        session1.sendMessage(new TextMessage("{\"action\":\"ready\"}"));
        session2.sendMessage(new TextMessage("{\"action\":\"ready\"}"));

        awaitAction(messages1, "battle_start");
        awaitAction(messages2, "battle_start");

        session1.sendMessage(new TextMessage("{\"action\":\"gesture\",\"gesture\":\"rock\"}"));
        session2.sendMessage(new TextMessage("{\"action\":\"gesture\",\"gesture\":\"scissors\"}"));

        JsonNode endMessage1 = awaitAction(messages1, "battle_end");
        JsonNode endMessage2 = awaitAction(messages2, "battle_end");
        assertThat(endMessage1.path("game_id").asInt()).isGreaterThan(0);
        assertThat(endMessage1.path("game_id").asInt()).isEqualTo(endMessage2.path("game_id").asInt());
        assertThat(endMessage1.path("winner").asText()).isEqualTo("alpha");
        assertThat(endMessage1.path("gestures").path(String.valueOf(user1)).asText()).isEqualTo("rock");
        assertThat(endMessage1.path("gestures").path(String.valueOf(user2)).asText()).isEqualTo("scissors");

        session1.close();
        session2.close();
    }

    private String url() {
        return "ws://localhost:" + port + "/ws/multiplayer";
    }

    private JsonNode awaitAction(BlockingQueue<String> queue, String action) throws Exception {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
        while (System.currentTimeMillis() < deadline) {
            String message = queue.poll(1, TimeUnit.SECONDS);
            if (message == null) {
                continue;
            }
            JsonNode node = objectMapper.readTree(message);
            if (action.equals(node.path("action").asText())) {
                return node;
            }
        }
        throw new AssertionError("Timed out waiting for action " + action);
    }

    private String joinPayload(int userId) {
        return "{\"action\":\"join\",\"user_id\":" + userId + "}";
    }

    private int register(String username, String password) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/register", body, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode json = objectMapper.readTree(response.getBody());
        return json.path("userId").asInt();
    }

    private static class CollectingHandler extends TextWebSocketHandler {
        private final BlockingQueue<String> queue;

        private CollectingHandler(BlockingQueue<String> queue) {
            this.queue = queue;
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            queue.add(message.getPayload());
        }
    }
}
