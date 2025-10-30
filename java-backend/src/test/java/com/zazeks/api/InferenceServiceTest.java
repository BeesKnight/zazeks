package com.zazeks.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class InferenceServiceTest {
    private InferenceService inferenceService;

    @BeforeEach
    void setUp() {
        inferenceService = new InferenceService();
    }

    @Test
    void classifiesRedAsRock() throws IOException {
        String payload = createDataUri(new Color(255, 32, 32));
        InferenceService.DetectionResult result = inferenceService.detect(payload);
        assertEquals("rock", result.gesture());
        assertTrue(result.confidence() > 0.4);
        assertNotNull(result.box());
    }

    @Test
    void classifiesGreenAsPaper() throws IOException {
        String payload = createDataUri(new Color(32, 255, 32));
        InferenceService.DetectionResult result = inferenceService.detect(payload);
        assertEquals("paper", result.gesture());
    }

    @Test
    void classifiesBlueAsScissors() throws IOException {
        String payload = createDataUri(new Color(32, 32, 255));
        InferenceService.DetectionResult result = inferenceService.detect(payload);
        assertEquals("scissors", result.gesture());
    }

    private String createDataUri(Color color) throws IOException {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(color);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        return "data:image/png;base64," + base64;
    }
}
