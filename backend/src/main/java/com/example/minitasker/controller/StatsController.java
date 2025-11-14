package com.example.minitasker.controller;

import com.example.minitasker.dto.stats.KeyValueStat;
import com.example.minitasker.service.StatsService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/tasks-by-status")
    public ResponseEntity<List<KeyValueStat>> tasksByStatus(@RequestParam Long projectId) {
        return ResponseEntity.ok(statsService.tasksByStatus(projectId));
    }

    @GetMapping("/tasks-by-priority")
    public ResponseEntity<List<KeyValueStat>> tasksByPriority(@RequestParam Long projectId) {
        return ResponseEntity.ok(statsService.tasksByPriority(projectId));
    }
}
