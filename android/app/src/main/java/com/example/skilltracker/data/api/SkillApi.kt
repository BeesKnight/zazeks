package com.example.skilltracker.data.api

import com.example.skilltracker.data.dto.CreateSkillRequest
import com.example.skilltracker.data.dto.SkillDto
import com.example.skilltracker.data.dto.UpdateSkillRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SkillApi {
    @GET("api/skills")
    suspend fun getSkills(
        @Query("includeArchived") includeArchived: Boolean? = null
    ): List<SkillDto>

    @GET("api/skills/{id}")
    suspend fun getSkill(@Path("id") id: Long): SkillDto

    @POST("api/skills")
    suspend fun createSkill(@Body request: CreateSkillRequest): SkillDto

    @PUT("api/skills/{id}")
    suspend fun updateSkill(@Path("id") id: Long, @Body request: UpdateSkillRequest): SkillDto

    @PATCH("api/skills/{id}/archive")
    suspend fun archiveSkill(@Path("id") id: Long): SkillDto
}
