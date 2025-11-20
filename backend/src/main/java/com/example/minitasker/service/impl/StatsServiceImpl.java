package com.example.minitasker.service.impl;

import com.example.minitasker.dto.stats.KeyValueStat;
import com.example.minitasker.exception.ResourceNotFoundException;
import com.example.minitasker.model.Project;
import com.example.minitasker.model.enums.TaskPriority;
import com.example.minitasker.model.enums.TaskStatus;
import com.example.minitasker.repository.ProjectRepository;
import com.example.minitasker.repository.TaskRepository;
import com.example.minitasker.service.StatsService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public StatsServiceImpl(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public List<KeyValueStat> tasksByStatus(Long projectId) {
        Project project = loadProject(projectId);
        List<KeyValueStat> stats = new ArrayList<>();
        for (TaskStatus status : TaskStatus.values()) {
            long count = taskRepository.countByProjectAndStatus(project, status);
            stats.add(new KeyValueStat(status.name(), count));
        }
        return stats;
    }

    @Override
    public List<KeyValueStat> tasksByPriority(Long projectId) {
        Project project = loadProject(projectId);
        List<KeyValueStat> stats = new ArrayList<>();
        for (TaskPriority priority : TaskPriority.values()) {
            long count = taskRepository.countByProjectAndPriority(project, priority);
            stats.add(new KeyValueStat(priority.name(), count));
        }
        return stats;
    }

    private Project loadProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }
}
