package com.zazeks.api;

import com.zazeks.ml.MlClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/model")
public class MlProxyController {

    private static final Logger log = LoggerFactory.getLogger(MlProxyController.class);
    private final MlClient mlClient;

    public MlProxyController(MlClient mlClient) {
        this.mlClient = mlClient;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        boolean up = mlClient.health();
        return ResponseEntity.ok("{\"ml_up\":" + up + "}");
    }

    @PostMapping(value = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> detect(@RequestPart("file") MultipartFile file) {
        try {
            byte[] data = file.getBytes();
            if (data == null || data.length == 0) {
                return ResponseEntity.badRequest()
                        .body("{\"gesture\":\"Unknown\",\"bbox\":[0,0,0,0],\"confidence\":0.0}");
            }
            String json = mlClient.detect(data); // отдаём JSON как есть
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            log.error("proxy /model/detect failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"gesture\":\"Unknown\",\"bbox\":[0,0,0,0],\"confidence\":0.0}");
        }
    }
}
