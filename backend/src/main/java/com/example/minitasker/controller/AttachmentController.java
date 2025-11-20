package com.example.minitasker.controller;

import com.example.minitasker.dto.attachment.AttachmentResponse;
import com.example.minitasker.service.AttachmentService;
import java.io.IOException;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/tasks/{taskId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> listAttachments(@PathVariable("taskId") Long taskId) {
        return ResponseEntity.ok(attachmentService.listAttachments(taskId));
    }

    @PostMapping(value = "/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(@PathVariable("taskId") Long taskId,
                                                               @RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(attachmentService.uploadAttachment(taskId, file));
    }

    @GetMapping("/tasks/{taskId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable("taskId") Long taskId,
                                                       @PathVariable("attachmentId") Long attachmentId) {
        var download = attachmentService.downloadAttachment(taskId, attachmentId);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (download.getContentType() != null) {
            mediaType = MediaType.parseMediaType(download.getContentType());
        }
        String fileName = download.getFileName() != null ? download.getFileName() : "attachment";
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(download.getResource());
    }
}
