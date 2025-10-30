package com.zazeks.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zazeks.app.Application;
import com.zazeks.database.InMemoryDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthAndGameIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InMemoryDatabase database;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        database.reset();
    }

    @Test
    void duplicateSubmissionIsBlocked() throws Exception {
        register("alice", "secret");
        String token = login("alice", "secret");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> payload = new HashMap<>();
        payload.put("userChoice", "rock");
        payload.put("computerChoice", "scissors");
        payload.put("result", "win");

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> firstResponse = restTemplate.postForEntity("/games", entity, String.class);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode json = objectMapper.readTree(firstResponse.getBody());
        int gameId = json.path("id").asInt();
        assertThat(gameId).isGreaterThan(0);

        ResponseEntity<String> duplicateResponse = restTemplate.postForEntity("/games", entity, String.class);
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<String> gameResponse = restTemplate.exchange("/games/" + gameId, HttpMethod.GET, getEntity, String.class);
        assertThat(gameResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void register(String username, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/register", body, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String login(String username, String password) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/login", body, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode json = objectMapper.readTree(response.getBody());
        return json.path("accessToken").asText();
    }
}
