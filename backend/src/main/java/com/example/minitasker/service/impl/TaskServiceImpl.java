package com.example.minitasker.service.impl;

import com.example.minitasker.dto.PageResponse;
import com.example.minitasker.dto.comment.CommentResponse;
import com.example.minitasker.dto.task.TaskRequest;
import com.example.minitasker.dto.task.TaskResponse;
import com.example.minitasker.dto.task.TaskSummary;
import com.example.minitasker.exception.BadRequestException;
import com.example.minitasker.exception.ResourceNotFoundException;
import com.example.minitasker.model.Comment;
import com.example.minitasker.model.Project;
import com.example.minitasker.model.Task;
import com.example.minitasker.model.User;
import com.example.minitasker.model.enums.Role;
import com.example.minitasker.model.enums.TaskPriority;
import com.example.minitasker.model.enums.TaskStatus;
import com.example.minitasker.repository.CommentRepository;
import com.example.minitasker.repository.ProjectRepository;
import com.example.minitasker.repository.TaskRepository;
import com.example.minitasker.repository.UserRepository;
import com.example.minitasker.service.TaskService;
import com.example.minitasker.util.SecurityUtils;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    public TaskServiceImpl(TaskRepository taskRepository,
                           ProjectRepository projectRepository,
                           UserRepository userRepository,
                           CommentRepository commentRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskSummary> getTasks(Long projectId, String status, String priority, Long assigneeId, Pageable pageable) {
        Project project = loadProjectForCurrentUser(projectId);
        Specification<Task> spec = Specification.where((root, query, cb) -> cb.equal(root.get("project"), project));
        if (status != null && !status.isBlank()) {
            TaskStatus taskStatus;
            try {
                taskStatus = TaskStatus.valueOf(status);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid status value: " + status);
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), taskStatus));
        }
        if (priority != null && !priority.isBlank()) {
            TaskPriority taskPriority;
            try {
                taskPriority = TaskPriority.valueOf(priority);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid priority value: " + priority);
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), taskPriority));
        }
        if (assigneeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId));
        }
        Page<Task> page = taskRepository.findAll(spec, pageable);
        return new PageResponse<>(
                page.map(this::mapToSummary).getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTask(Long id) {
        Task task = loadTaskForCurrentUser(id);
        return mapToResponse(task);
    }

    @Override
    public TaskResponse createTask(Long projectId, TaskRequest request) {
        Project project = loadProjectForCurrentUser(projectId);
        Task task = new Task();
        task.setProject(project);
        applyTaskRequest(task, request);
        Task saved = taskRepository.save(task);
        log.info("Task created: {} in project {}", saved.getId(), projectId);
        return mapToResponse(saved);
    }

    @Override
    public TaskResponse updateTask(Long taskId, TaskRequest request) {
        Task task = loadTaskForCurrentUser(taskId);
        applyTaskRequest(task, request);
        Task updated = taskRepository.save(task);
        log.info("Task updated: {}", updated.getId());
        return mapToResponse(updated);
    }

    @Override
    public void deleteTask(Long id) {
        Task task = loadTaskForCurrentUser(id);
        taskRepository.delete(task);
        log.info("Task deleted: {}", task.getId());
    }

    private void applyTaskRequest(Task task, TaskRequest request) {
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new BadRequestException("Assignee not found"));
            task.setAssignee(assignee);
        } else {
            task.setAssignee(null);
        }
        task.setDueDate(request.getDueDate());
    }

    private Task loadTaskForCurrentUser(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        ensureAccessToProject(task.getProject());
        return task;
    }

    private Project loadProjectForCurrentUser(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        ensureAccessToProject(project);
        return project;
    }

    private void ensureAccessToProject(Project project) {
        User current = SecurityUtils.getCurrentUser();
        if (current.getRole() == Role.ADMIN) {
            return;
        }
        boolean owner = project.getOwner().getId().equals(current.getId());
        boolean assignee = project.getTasks().stream()
                .map(Task::getAssignee)
                .filter(a -> a != null)
                .anyMatch(a -> a.getId().equals(current.getId()));
        if (!owner && !assignee) {
            throw new ResourceNotFoundException("Project not found");
        }
    }

    private TaskSummary mapToSummary(Task task) {
        TaskSummary summary = new TaskSummary();
        summary.setId(task.getId());
        summary.setTitle(task.getTitle());
        summary.setStatus(task.getStatus());
        summary.setPriority(task.getPriority());
        summary.setDueDate(task.getDueDate());
        summary.setAssigneeName(task.getAssignee() != null ? task.getAssignee().getFullName() : null);
        return summary;
    }

    private TaskResponse mapToResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setProjectId(task.getProject().getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        if (task.getAssignee() != null) {
            response.setAssigneeId(task.getAssignee().getId());
            response.setAssigneeName(task.getAssignee().getFullName());
        }
        response.setDueDate(task.getDueDate());
        response.setCreatedAt(task.getCreatedAt());
        List<Comment> comments = commentRepository.findByTaskOrderByCreatedAtAsc(task);
        List<CommentResponse> commentDtos = comments.stream().map(comment -> {
            CommentResponse dto = new CommentResponse();
            dto.setId(comment.getId());
            dto.setTaskId(task.getId());
            dto.setAuthorId(comment.getAuthor().getId());
            dto.setAuthorName(comment.getAuthor().getFullName());
            dto.setText(comment.getText());
            dto.setCreatedAt(comment.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
        response.setComments(commentDtos);
        response.setAttachments(task.getAttachments().stream().map(attachment -> {
            TaskResponse.AttachmentResponse dto = new TaskResponse.AttachmentResponse();
            dto.setId(attachment.getId());
            dto.setFileName(attachment.getFileName());
            dto.setContentType(attachment.getContentType());
            dto.setUploadedAt(attachment.getUploadedAt());
            dto.setUrl("/api/attachments/" + attachment.getId());
            return dto;
        }).collect(Collectors.toList()));
        return response;
    }
}
