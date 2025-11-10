package com.example.skilltracker.data.repository

import com.example.skilltracker.data.api.ApiClient
import com.example.skilltracker.domain.model.SkillStats

class StatsRepository {
    private val api = ApiClient.statsApi

    suspend fun getSkillStats(): List<SkillStats> = api.getSkillStats().map { dto ->
        SkillStats(dto.skillId, dto.skillName, dto.sessionCount, dto.totalDurationMinutes)
    }
}
