package com.example.skilltracker.data.repository

import com.example.skilltracker.data.api.ApiClient
import com.example.skilltracker.data.dto.CreateSkillRequest
import com.example.skilltracker.data.dto.SkillDto
import com.example.skilltracker.data.dto.UpdateSkillRequest
import com.example.skilltracker.domain.model.Skill

class SkillRepository {
    private val api = ApiClient.skillApi

    suspend fun getSkills(includeArchived: Boolean = false): List<Skill> {
        val include = if (includeArchived) true else null
        return api.getSkills(include).map { it.toDomain() }
    }

    suspend fun createSkill(name: String, description: String?, category: String?, color: String?): Skill {
        val dto = api.createSkill(CreateSkillRequest(name, description, category, color))
        return dto.toDomain()
    }

    suspend fun updateSkill(
        id: Long,
        name: String?,
        description: String?,
        category: String?,
        color: String?,
        archived: Boolean?
    ): Skill {
        val request = UpdateSkillRequest(name, description, category, color, archived)
        val dto = api.updateSkill(id, request)
        return dto.toDomain()
    }

    suspend fun archiveSkill(id: Long): Skill = api.archiveSkill(id).toDomain()

    private fun SkillDto.toDomain(): Skill = Skill(
        id = id,
        name = name,
        description = description,
        category = category,
        color = color,
        archived = archived,
        createdAt = createdAt
    )
}
