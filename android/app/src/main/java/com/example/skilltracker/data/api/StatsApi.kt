package com.example.skilltracker.data.api

import com.example.skilltracker.data.dto.OverviewStatsDto
import com.example.skilltracker.data.dto.SkillDetailsStatsDto
import com.example.skilltracker.data.dto.SkillStatsDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StatsApi {
    @GET("api/stats/skills")
    suspend fun getSkillStats(
        @Query("skillId") skillId: Long? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): List<SkillStatsDto>

    @GET("api/stats/overview")
    suspend fun getOverviewStats(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): OverviewStatsDto

    @GET("api/stats/skills/{id}")
    suspend fun getSkillDetails(
        @Path("id") skillId: Long,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): SkillDetailsStatsDto
}
