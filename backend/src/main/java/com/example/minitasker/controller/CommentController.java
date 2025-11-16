package com.example.minitasker.controller;

import com.example.minitasker.dto.comment.CommentRequest;
import com.example.minitasker.dto.comment.CommentResponse;
import com.example.minitasker.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> listComments(@PathVariable("taskId") Long taskId) {
        return ResponseEntity.ok(commentService.getComments(taskId));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@PathVariable("taskId") Long taskId,
                                                         @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.addComment(taskId, request));
    }
}
