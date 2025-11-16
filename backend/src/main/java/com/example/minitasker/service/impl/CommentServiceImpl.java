package com.example.minitasker.service.impl;

import com.example.minitasker.dto.comment.CommentRequest;
import com.example.minitasker.dto.comment.CommentResponse;
import com.example.minitasker.exception.ResourceNotFoundException;
import com.example.minitasker.model.Comment;
import com.example.minitasker.model.Task;
import com.example.minitasker.model.User;
import com.example.minitasker.model.enums.Role;
import com.example.minitasker.repository.CommentRepository;
import com.example.minitasker.repository.TaskRepository;
import com.example.minitasker.service.CommentService;
import com.example.minitasker.util.SecurityUtils;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;

    public CommentServiceImpl(CommentRepository commentRepository, TaskRepository taskRepository) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long taskId) {
        Task task = loadTask(taskId);
        return commentRepository.findByTaskOrderByCreatedAtAsc(task).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponse addComment(Long taskId, CommentRequest request) {
        Task task = loadTask(taskId);
        User current = SecurityUtils.getCurrentUser();
        Comment comment = new Comment();
        comment.setTask(task);
        comment.setAuthor(current);
        comment.setText(request.getText());
        return mapToDto(commentRepository.save(comment));
    }

    private Task loadTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        User current = SecurityUtils.getCurrentUser();
        if (current.getRole() == Role.ADMIN) {
            return task;
        }
        boolean owner = task.getProject().getOwner().getId().equals(current.getId());
        boolean assigned = task.getAssignee() != null && task.getAssignee().getId().equals(current.getId());
        if (!owner && !assigned) {
            throw new ResourceNotFoundException("Task not found");
        }
        return task;
    }

    private CommentResponse mapToDto(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setTaskId(comment.getTask().getId());
        response.setAuthorId(comment.getAuthor().getId());
        response.setAuthorName(comment.getAuthor().getFullName());
        response.setText(comment.getText());
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }
}
