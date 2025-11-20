package com.example.minitasker.service;

import com.example.minitasker.dto.attachment.AttachmentResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {
    List<AttachmentResponse> listAttachments(Long taskId);
    AttachmentResponse uploadAttachment(Long taskId, MultipartFile file) throws IOException;
    AttachmentDownload downloadAttachment(Long taskId, Long attachmentId);
}
