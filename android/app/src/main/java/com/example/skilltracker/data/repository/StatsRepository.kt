package com.example.skilltracker.data.repository

import com.example.skilltracker.data.api.ApiClient
import com.example.skilltracker.data.dto.InactiveSkillDto
import com.example.skilltracker.data.dto.OverviewSkillBreakdownDto
import com.example.skilltracker.data.dto.SkillDailyStatsDto
import com.example.skilltracker.domain.model.InactiveSkill
import com.example.skilltracker.domain.model.OverviewSkillBreakdown
import com.example.skilltracker.domain.model.OverviewStats
import com.example.skilltracker.domain.model.SkillDailyStats
import com.example.skilltracker.domain.model.SkillDetailsStats
import com.example.skilltracker.domain.model.SkillStats

class StatsRepository {
    private val api = ApiClient.statsApi

    suspend fun getSkillStats(): List<SkillStats> = api.getSkillStats().map { dto ->
        SkillStats(dto.skillId, dto.skillName, dto.sessionCount, dto.totalDurationMinutes)
    }

    suspend fun getOverviewStats(from: String? = null, to: String? = null): OverviewStats {
        val dto = api.getOverviewStats(from, to)
        return OverviewStats(
            totalMinutes = dto.totalMinutes,
            skillsCount = dto.skillsCount,
            sessionsCount = dto.sessionsCount,
            bySkill = dto.bySkill.map(OverviewSkillBreakdownDto::toDomain),
            inactiveSkills = dto.inactiveSkills.map(InactiveSkillDto::toDomain)
        )
    }

    suspend fun getSkillDetails(skillId: Long, from: String? = null, to: String? = null): SkillDetailsStats {
        val dto = api.getSkillDetails(skillId, from, to)
        return SkillDetailsStats(
            skillId = dto.skillId,
            skillName = dto.skillName,
            totalMinutes = dto.totalMinutes,
            sessionsCount = dto.sessionsCount,
            averageDifficulty = dto.averageDifficulty,
            byDay = dto.byDay.map(SkillDailyStatsDto::toDomain)
        )
    }

    private fun OverviewSkillBreakdownDto.toDomain(): OverviewSkillBreakdown = OverviewSkillBreakdown(
        skillId = skillId,
        skillName = skillName,
        minutes = minutes,
        sessions = sessions
    )

    private fun InactiveSkillDto.toDomain(): InactiveSkill = InactiveSkill(
        skillId = skillId,
        skillName = skillName,
        daysSinceLastSession = daysSinceLastSession
    )

    private fun SkillDailyStatsDto.toDomain(): SkillDailyStats = SkillDailyStats(
        date = date,
        minutes = minutes
    )
}
