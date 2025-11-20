package com.example.minitasker.dto.attachment;

import org.springframework.core.io.Resource;

public class AttachmentDownload {
    private final Resource resource;
    private final String contentType;
    private final String fileName;

    public AttachmentDownload(Resource resource, String contentType, String fileName) {
        this.resource = resource;
        this.contentType = contentType;
        this.fileName = fileName;
    }

    public Resource getResource() {
        return resource;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileName() {
        return fileName;
    }
}
