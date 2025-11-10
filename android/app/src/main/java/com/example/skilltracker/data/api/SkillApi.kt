package com.example.skilltracker.data.api

import com.example.skilltracker.data.dto.CreateSkillRequest
import com.example.skilltracker.data.dto.SkillDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SkillApi {
    @GET("api/skills")
    suspend fun getSkills(): List<SkillDto>

    @GET("api/skills/{id}")
    suspend fun getSkill(@Path("id") id: Long): SkillDto

    @POST("api/skills")
    suspend fun createSkill(@Body request: CreateSkillRequest): SkillDto
}
