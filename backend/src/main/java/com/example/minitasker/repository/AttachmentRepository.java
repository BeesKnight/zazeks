package com.example.minitasker.repository;

import com.example.minitasker.model.Attachment;
import com.example.minitasker.model.Task;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTask(Task task);
}
