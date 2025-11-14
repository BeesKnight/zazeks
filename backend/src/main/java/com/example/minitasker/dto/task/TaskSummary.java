package com.example.minitasker.dto.task;

import com.example.minitasker.model.enums.TaskPriority;
import com.example.minitasker.model.enums.TaskStatus;
import java.time.OffsetDateTime;

public class TaskSummary {
    private Long id;
    private String title;
    private TaskStatus status;
    private TaskPriority priority;
    private OffsetDateTime dueDate;
    private String assigneeName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public OffsetDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(OffsetDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }
}
