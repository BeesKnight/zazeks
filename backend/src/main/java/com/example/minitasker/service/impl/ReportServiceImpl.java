package com.example.minitasker.service.impl;

import com.example.minitasker.exception.ResourceNotFoundException;
import com.example.minitasker.model.Project;
import com.example.minitasker.model.Task;
import com.example.minitasker.model.enums.Role;
import com.example.minitasker.repository.ProjectRepository;
import com.example.minitasker.repository.TaskRepository;
import com.example.minitasker.service.ReportService;
import com.example.minitasker.util.SecurityUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ReportServiceImpl(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public Resource generateTasksCsv(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        ensureAccess(project);
        List<Task> tasks = taskRepository.findByProject(project);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8)) {
            writer.println("Task ID,Title,Status,Priority,Assignee,Due Date");
            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            tasks.forEach(task -> writer.printf("%d,%s,%s,%s,%s,%s%n",
                    task.getId(),
                    escape(task.getTitle()),
                    task.getStatus().name(),
                    task.getPriority().name(),
                    task.getAssignee() != null ? escape(task.getAssignee().getFullName()) : "",
                    task.getDueDate() != null ? task.getDueDate().format(formatter) : ""));
        }
        return new InputStreamResource(new ByteArrayInputStream(outputStream.toByteArray())) {
            @Override
            public String getFilename() {
                return "tasks_project_" + projectId + ".csv";
            }
        };
    }

    private void ensureAccess(Project project) {
        var current = SecurityUtils.getCurrentUser();
        if (current.getRole() == Role.ADMIN) {
            return;
        }
        boolean owner = project.getOwner().getId().equals(current.getId());
        boolean assigned = project.getTasks().stream()
                .anyMatch(task -> task.getAssignee() != null && task.getAssignee().getId().equals(current.getId()));
        if (!owner && !assigned) {
            throw new ResourceNotFoundException("Project not found");
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.replace("\"", "\"\"");
        return '"' + sanitized + '"';
    }
}
