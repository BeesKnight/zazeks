package com.example.skilltracker.data.repository

import com.example.skilltracker.data.api.ApiClient
import com.example.skilltracker.data.dto.CreateSkillRequest
import com.example.skilltracker.domain.model.Skill

class SkillRepository {
    private val api = ApiClient.skillApi

    suspend fun getSkills(): List<Skill> = api.getSkills().map { dto ->
        Skill(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            createdAt = dto.createdAt
        )
    }

    suspend fun createSkill(name: String, description: String?): Skill {
        val dto = api.createSkill(CreateSkillRequest(name, description))
        return Skill(dto.id, dto.name, dto.description, dto.createdAt)
    }
}
