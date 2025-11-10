package com.example.skilltracker.data.repository

import com.example.skilltracker.data.api.ApiClient
import com.example.skilltracker.data.dto.CreateSessionRequest
import com.example.skilltracker.domain.model.Session
import java.time.Instant

class SessionRepository {
    private val api = ApiClient.sessionApi

    suspend fun getSessions(
        skillId: Long? = null,
        from: String? = null,
        to: String? = null
    ): List<Session> = api.getSessions(skillId, from, to).map { dto ->
        Session(
            id = dto.id,
            skillId = dto.skillId,
            sessionDate = dto.sessionDate,
            durationMinutes = dto.durationMinutes,
            notes = dto.notes,
            difficulty = dto.difficulty,
            source = dto.source
        )
    }

    suspend fun createSession(
        skillId: Long,
        durationMinutes: Int,
        notes: String?,
        difficulty: Int?,
        source: String?,
        sessionDate: String? = null
    ): Session {
        val request = CreateSessionRequest(
            skillId = skillId,
            sessionDate = sessionDate ?: Instant.now().toString(),
            durationMinutes = durationMinutes,
            notes = notes,
            difficulty = difficulty,
            source = source
        )
        val dto = api.createSession(request)
        return Session(
            id = dto.id,
            skillId = dto.skillId,
            sessionDate = dto.sessionDate,
            durationMinutes = dto.durationMinutes,
            notes = dto.notes,
            difficulty = dto.difficulty,
            source = dto.source
        )
    }
}
