package com.example.minitasker.data.repository

import com.example.minitasker.data.api.ApiService
import com.example.minitasker.data.model.StatsDto

class StatsRepository(private val apiService: ApiService) {
    suspend fun loadStatusStats(projectId: Long): List<StatsDto> = apiService.statsByStatus(projectId)
    suspend fun loadPriorityStats(projectId: Long): List<StatsDto> = apiService.statsByPriority(projectId)
}
