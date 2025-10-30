package com.zazeks.web;

import com.zazeks.api.InferenceService;
import com.zazeks.api.InferenceService.DetectionResult;
import com.zazeks.security.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/model")
public class ModelInferenceController {
    private final InferenceService inferenceService;
    private final AuthenticationService authenticationService;

    public ModelInferenceController(InferenceService inferenceService,
                                    AuthenticationService authenticationService) {
        this.inferenceService = inferenceService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/detect")
    public ResponseEntity<DetectionResult> detect(@RequestPart("file") MultipartFile file,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        Integer userId = null;
        if (authorization != null && !authorization.isBlank()) {
            userId = authenticationService.requireUser(authorization).getId();
        }
        return ResponseEntity.ok(inferenceService.detectGesture(file, userId));
    }
}
