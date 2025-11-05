package com.zazeks.ml;

import com.zazeks.config.MlProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
public class MlClient {

    private static final Logger log = LoggerFactory.getLogger(MlClient.class);

    private final RestTemplate rest;
    private final String baseUrl;

    public MlClient(RestTemplateBuilder builder, MlProperties props) {
        this.baseUrl = props.getBaseUrl();
        this.rest = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
        log.info("ML base URL: {}", baseUrl);
    }

    /** Проксируем JPEG в Python ML: POST /model/detect, multipart form-data. Возвращаем JSON как String. */
    public String detect(byte[] jpegBytes) {
        ByteArrayResource file = new ByteArrayResource(jpegBytes) {
            @Override public String getFilename() { return "frame.jpg"; }
            @Override public long contentLength() { return jpegBytes.length; }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);

        String url = baseUrl + "/model/detect";
        ResponseEntity<String> resp = rest.postForEntity(url, req, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RestClientException("ML detect failed: " + resp.getStatusCode());
        }
        return resp.getBody();
    }

    /** Быстрый healthcheck. */
    public boolean health() {
        try {
            String url = baseUrl + "/health";
            ResponseEntity<String> r = rest.getForEntity(url, String.class);
            return r.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
