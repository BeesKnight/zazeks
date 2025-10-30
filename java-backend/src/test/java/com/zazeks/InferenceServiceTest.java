package com.zazeks;

import com.zazeks.api.InferenceService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class InferenceServiceTest extends ServiceTestHarness {

    @Test
    void detectGesturePersistsMetadataAndReturnsResult() throws IOException {
        byte[] payload = new byte[] {1, 2, 3, 4, 5};
        MockMultipartFile file = new MockMultipartFile("file", "frame.jpg", "image/jpeg", payload);

        InferenceService.DetectionResult result = inferenceService.detectGesture(file, null);

        assertNotNull(result);
        assertNotNull(result.detectedAt());
        assertTrue(result.detectionId() > 0);
        assertTrue(result.durationMillis() >= 0);
        assertEquals(1, database.findAllDetectionMetadata().size());
    }

    @Test
    void detectGestureRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> inferenceService.detectGesture(file, null));
    }

    @Test
    void detectGestureWrapsIoExceptions() {
        MultipartFile failingFile = new MultipartFile() {
            @Override
            public String getName() {
                return "fail";
            }

            @Override
            public String getOriginalFilename() {
                return "fail.bin";
            }

            @Override
            public String getContentType() {
                return "application/octet-stream";
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return 10;
            }

            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("boom");
            }

            @Override
            public java.io.InputStream getInputStream() throws IOException {
                throw new IOException("boom");
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException {
                throw new IOException("boom");
            }
        };

        assertThrows(IllegalArgumentException.class, () -> inferenceService.detectGesture(failingFile, 1));
    }
}
