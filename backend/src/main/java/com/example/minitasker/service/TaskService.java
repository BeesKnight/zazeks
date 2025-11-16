package com.example.minitasker.service;

import com.example.minitasker.dto.PageResponse;
import com.example.minitasker.dto.task.TaskRequest;
import com.example.minitasker.dto.task.TaskResponse;
import com.example.minitasker.dto.task.TaskSummary;
import org.springframework.data.domain.Pageable;

public interface TaskService {
    PageResponse<TaskSummary> getTasks(Long projectId, String status, String priority, Long assigneeId, Pageable pageable);
    TaskResponse getTask(Long id);
    TaskResponse createTask(Long projectId, TaskRequest request);
    TaskResponse updateTask(Long taskId, TaskRequest request);
    void deleteTask(Long id);
}
