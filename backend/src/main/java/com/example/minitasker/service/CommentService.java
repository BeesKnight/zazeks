package com.example.minitasker.service;

import com.example.minitasker.dto.comment.CommentRequest;
import com.example.minitasker.dto.comment.CommentResponse;
import java.util.List;

public interface CommentService {
    List<CommentResponse> getComments(Long taskId);
    CommentResponse addComment(Long taskId, CommentRequest request);
}
