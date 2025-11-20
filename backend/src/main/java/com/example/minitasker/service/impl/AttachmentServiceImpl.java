package com.example.minitasker.service.impl;

import com.example.minitasker.dto.attachment.AttachmentDownload;
import com.example.minitasker.dto.attachment.AttachmentResponse;
import com.example.minitasker.exception.BadRequestException;
import com.example.minitasker.exception.ResourceNotFoundException;
import com.example.minitasker.model.Attachment;
import com.example.minitasker.model.Task;
import com.example.minitasker.repository.AttachmentRepository;
import com.example.minitasker.repository.TaskRepository;
import com.example.minitasker.service.AttachmentService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final Path storagePath;

    public AttachmentServiceImpl(AttachmentRepository attachmentRepository,
                                 TaskRepository taskRepository,
                                 @Value("${minitasker.storage.upload-dir}") String uploadDir) {
        this.attachmentRepository = attachmentRepository;
        this.taskRepository = taskRepository;
        this.storagePath = Paths.get(uploadDir).toAbsolutePath().normalize();
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
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path destination = storagePath.resolve(filename);
        Files.copy(file.getInputStream(), destination);
        Attachment attachment = new Attachment();
        attachment.setTask(task);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        attachment.setFilePath(destination.toString());
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
        return new AttachmentDownload(
                new FileSystemResource(Paths.get(attachment.getFilePath())),
                attachment.getContentType(),
                attachment.getFileName()
        );
    }

    private Task loadTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private AttachmentResponse mapToDto(Attachment attachment) {
        AttachmentResponse response = new AttachmentResponse();
        response.setId(attachment.getId());
        response.setTaskId(attachment.getTask().getId());
        response.setFileName(attachment.getFileName());
        response.setContentType(attachment.getContentType());
        response.setUrl("/api/tasks/" + attachment.getTask().getId() + "/attachments/" + attachment.getId());
        response.setUploadedAt(attachment.getUploadedAt());
        return response;
    }
}
