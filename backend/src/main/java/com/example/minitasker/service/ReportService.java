package com.example.minitasker.service;

import org.springframework.core.io.Resource;

public interface ReportService {
    Resource generateTasksCsv(Long projectId);
}
