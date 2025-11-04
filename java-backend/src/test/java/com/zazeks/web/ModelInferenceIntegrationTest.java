package com.zazeks.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zazeks.app.Application;
import com.zazeks.database.InMemoryDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class ModelInferenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryDatabase database;

    @BeforeEach
    void resetDatabase() {
        database.reset();
    }

    @Test
    void detectEndpointReturnsModelPrediction() throws Exception {
        Path imagePath = Path.of("../model/test_model/images.jpg").toAbsolutePath().normalize();
        byte[] payload = Files.readAllBytes(imagePath);
        MockMultipartFile file = new MockMultipartFile("file", "images.jpg", MediaType.IMAGE_JPEG_VALUE, payload);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/model/detect").file(file))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.path("gesture").asText()).isEqualTo("Rock");
        assertThat(json.path("confidence").asDouble()).isGreaterThan(0.85);
        JsonNode bbox = json.path("bbox");
        assertThat(bbox.isArray()).isTrue();
        assertThat(bbox.size()).isEqualTo(4);
        assertThat(database.findAllDetectionMetadata()).hasSize(1);
    }
}
