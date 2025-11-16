package com.example.minitasker.service;

import com.example.minitasker.dto.PageResponse;
import com.example.minitasker.dto.project.ProjectRequest;
import com.example.minitasker.dto.project.ProjectResponse;
import org.springframework.data.domain.Pageable;

public interface ProjectService {
    PageResponse<ProjectResponse> getProjects(String nameFilter, Pageable pageable);
    ProjectResponse getProject(Long id);
    ProjectResponse createProject(ProjectRequest request);
    ProjectResponse updateProject(Long id, ProjectRequest request);
    void deleteProject(Long id);
}
