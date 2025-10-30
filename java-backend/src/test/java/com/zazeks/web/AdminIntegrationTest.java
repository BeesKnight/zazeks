package com.zazeks.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zazeks.app.Application;
import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.Admin;
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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryDatabase database;

    @BeforeEach
    void setup() {
        database.reset();
    }

    @Test
    void adminEndpointsEnforceRoles() throws Exception {
        int adminId = register("admin", "secret");
        database.saveAdmin(new Admin(adminId));
        String adminToken = login("admin", "secret");

        int userId = register("player", "secret");
        String userToken = login("player", "secret");

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userToken);
        ResponseEntity<String> forbidden = restTemplate.exchange(
                "/admin/users/" + userId,
                HttpMethod.DELETE,
                new HttpEntity<>(userHeaders),
                String.class
        );
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> renamePayload = Map.of("newUsername", "champion");
        ResponseEntity<String> renameResponse = restTemplate.exchange(
                "/admin/users/" + userId + "/username",
                HttpMethod.PUT,
                new HttpEntity<>(renamePayload, adminHeaders),
                String.class
        );
        assertThat(renameResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpHeaders profileHeaders = new HttpHeaders();
        profileHeaders.setBearerAuth(userToken);
        ResponseEntity<String> profileResponse = restTemplate.exchange(
                "/users/" + userId,
                HttpMethod.GET,
                new HttpEntity<>(profileHeaders),
                String.class
        );
        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode profileJson = objectMapper.readTree(profileResponse.getBody());
        assertThat(profileJson.path("username").asText()).isEqualTo("champion");

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/admin/users/" + userId,
                HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders),
                String.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(database.findUserById(userId)).isEmpty();
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
