package com.example.minitasker.repository;

import com.example.minitasker.model.Comment;
import com.example.minitasker.model.Task;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTaskOrderByCreatedAtAsc(Task task);
}
