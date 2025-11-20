package com.example.minitasker.service.impl;

import com.example.minitasker.dto.attachment.AttachmentDownload;
import com.example.minitasker.dto.attachment.AttachmentResponse;
import com.example.minitasker.exception.BadRequestException;
import com.example.minitasker.exception.ResourceNotFoundException;
import com.example.minitasker.model.Attachment;
import com.example.minitasker.model.Task;
import com.example.minitasker.model.User;
import com.example.minitasker.model.enums.Role;
import com.example.minitasker.repository.AttachmentRepository;
import com.example.minitasker.repository.TaskRepository;
import com.example.minitasker.service.AttachmentService;
import com.example.minitasker.util.SecurityUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final Path storagePath;

    public AttachmentServiceImpl(AttachmentRepository attachmentRepository,
                                 TaskRepository taskRepository,
                                 @Value("${minitasker.attachments-dir:/data/attachments}") String attachmentsDir) {
        this.attachmentRepository = attachmentRepository;
        this.taskRepository = taskRepository;
        this.storagePath = Paths.get(attachmentsDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storagePath);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize attachments directory", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> listAttachments(Long taskId) {
        Task task = loadTask(taskId);
        return attachmentRepository.findByTask(task).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public AttachmentResponse uploadAttachment(Long taskId, MultipartFile file) throws IOException {
        if (file.isEmpty() || StringUtils.isBlank(file.getOriginalFilename())) {
            throw new BadRequestException("File is empty");
        }
        Task task = loadTask(taskId);
        String originalName = file.getOriginalFilename();
        String extension = org.springframework.util.StringUtils.getFilenameExtension(originalName);
        String storageFileName = UUID.randomUUID()
                + (StringUtils.isNotBlank(extension) ? "." + extension : "");
        Path destination = storagePath.resolve(storageFileName);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        Attachment attachment = new Attachment();
        attachment.setTask(task);
        attachment.setFileName(originalName);
        attachment.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        attachment.setStorageFileName(storageFileName);
        Attachment saved = attachmentRepository.save(attachment);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDownload downloadAttachment(Long taskId, Long attachmentId) {
        Task task = loadTask(taskId);
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .filter(att -> att.getTask().getId().equals(task.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
        Path filePath = storagePath.resolve(attachment.getStorageFileName()).normalize();
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Attachment file not found");
        }
        Long contentLength = null;
        try {
            contentLength = resource.contentLength();
        } catch (IOException ignored) {
        }
        return new AttachmentDownload(
                resource,
                attachment.getContentType(),
                attachment.getFileName(),
                contentLength
        );
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

    private AttachmentResponse mapToDto(Attachment attachment) {
        AttachmentResponse response = new AttachmentResponse();
        response.setId(attachment.getId());
        response.setTaskId(attachment.getTask().getId());
        response.setFileName(attachment.getFileName());
        response.setContentType(attachment.getContentType());
        String downloadPath = "/api/tasks/" + attachment.getTask().getId() + "/attachments/" + attachment.getId();
        response.setUrl(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(downloadPath)
                .build()
                .toUriString());
        response.setDownloadUrl(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(downloadPath)
                .path("/download")
                .build()
                .toUriString());
        response.setUploadedAt(attachment.getUploadedAt());
        return response;
    }
}
