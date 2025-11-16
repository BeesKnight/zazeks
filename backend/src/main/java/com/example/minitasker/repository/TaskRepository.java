package com.example.minitasker.repository;

import com.example.minitasker.model.Project;
import com.example.minitasker.model.Task;
import com.example.minitasker.model.User;
import com.example.minitasker.model.enums.TaskPriority;
import com.example.minitasker.model.enums.TaskStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    Page<Task> findByProject(Project project, Pageable pageable);
    List<Task> findByProject(Project project);
    Optional<Task> findByIdAndProject(Long id, Project project);
    Page<Task> findByProjectAndAssignee(Project project, User assignee, Pageable pageable);
    long countByProjectAndStatus(Project project, TaskStatus status);
    long countByProjectAndPriority(Project project, TaskPriority priority);
    long countByProjectAndDueDateBefore(Project project, OffsetDateTime dueDate);
}
