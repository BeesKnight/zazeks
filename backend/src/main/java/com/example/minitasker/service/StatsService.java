package com.example.minitasker.service;

import com.example.minitasker.dto.stats.KeyValueStat;
import java.util.List;

public interface StatsService {
    List<KeyValueStat> tasksByStatus(Long projectId);
    List<KeyValueStat> tasksByPriority(Long projectId);
}
