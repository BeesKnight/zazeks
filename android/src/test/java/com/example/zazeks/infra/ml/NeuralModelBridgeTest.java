package com.example.zazeks.infra.ml;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class NeuralModelBridgeTest {
    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void remoteBridgeParsesPythonLikeResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"gesture\":\"Rock\",\"bbox\":[10,20,30,40],\"confidence\":0.91}")
            .addHeader("Content-Type", "application/json"));

        NeuralModelBridge bridge = NeuralModelBridge.remoteHttp(mockWebServer.url("/").toString());
        GameFrame frame = new GameFrame("data".getBytes(StandardCharsets.UTF_8), 640, 480, 1234L);

        DetectionResult result = bridge.detect(frame);

        assertEquals("Rock", result.getGesture());
        assertTrue(result.hasDetection());
        assertArrayEquals(new int[]{10, 20, 30, 40}, result.getBoundingBox());
        assertEquals(0.91, result.getConfidence(), 1e-6);
        assertEquals(640, result.getFrameWidth());
        assertEquals(480, result.getFrameHeight());

        okhttp3.mockwebserver.RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/model/detect", recordedRequest.getPath());
        String body = recordedRequest.getBody().readUtf8();
        assertTrue(body.contains("frame_640x480_1234.jpg"));
        String contentType = recordedRequest.getHeader("Content-Type");
        assertTrue(contentType != null && contentType.startsWith("multipart/form-data"));
    }
}
