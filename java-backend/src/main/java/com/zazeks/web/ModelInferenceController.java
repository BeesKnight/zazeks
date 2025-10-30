package com.zazeks.web;

import com.zazeks.api.inference.ModelInferenceService;
import com.zazeks.api.inference.ModelInferenceService.DetectionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/model")
public class ModelInferenceController {
    private final ModelInferenceService modelInferenceService;

    public ModelInferenceController(ModelInferenceService modelInferenceService) {
        this.modelInferenceService = modelInferenceService;
    }

    @PostMapping("/detect")
    public ResponseEntity<DetectionResult> detect(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(modelInferenceService.detectGesture(file));
    }
}
