package com.example.minitasker.service.impl;

import com.example.minitasker.dto.PageResponse;
import com.example.minitasker.dto.project.ProjectRequest;
import com.example.minitasker.dto.project.ProjectResponse;
import com.example.minitasker.exception.ResourceNotFoundException;
import com.example.minitasker.model.Project;
import com.example.minitasker.model.User;
import com.example.minitasker.repository.ProjectRepository;
import com.example.minitasker.repository.UserRepository;
import com.example.minitasker.service.ProjectService;
import com.example.minitasker.util.SecurityUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getProjects(String nameFilter, Pageable pageable) {
        Page<Project> page;
        if (nameFilter != null && !nameFilter.isBlank()) {
            page = projectRepository.findByNameContainingIgnoreCase(nameFilter, pageable);
        } else {
            page = projectRepository.findAll(pageable);
        }
        return new PageResponse<>(
                page.map(this::mapToDto).getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long id) {
        Project project = loadProject(id);
        return mapToDto(project);
    }

    @Override
    public ProjectResponse createProject(ProjectRequest request) {
        User current = SecurityUtils.getCurrentUser();
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwner(current);
        Project saved = projectRepository.save(project);
        log.info("Project created: {} by {}", saved.getId(), current.getEmail());
        return mapToDto(saved);
    }

    @Override
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = loadProject(id);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        Project updated = projectRepository.save(project);
        log.info("Project updated: {}", updated.getId());
        return mapToDto(updated);
    }

    @Override
    public void deleteProject(Long id) {
        Project project = loadProject(id);
        projectRepository.delete(project);
        log.info("Project deleted: {}", project.getId());
    }

    private Project loadProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private ProjectResponse mapToDto(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setOwnerId(project.getOwner().getId());
        response.setOwnerName(project.getOwner().getFullName());
        response.setCreatedAt(project.getCreatedAt());
        return response;
    }
}
