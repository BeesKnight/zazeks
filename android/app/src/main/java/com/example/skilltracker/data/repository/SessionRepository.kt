package com.example.skilltracker.data.repository

import com.example.skilltracker.data.api.ApiClient
import com.example.skilltracker.data.dto.CreateSessionRequest
import com.example.skilltracker.domain.model.Session
import java.time.Instant

class SessionRepository {
    private val api = ApiClient.sessionApi

    suspend fun getSessions(): List<Session> = api.getSessions().map { dto ->
        Session(dto.id, dto.skillId, dto.sessionDate, dto.durationMinutes, dto.notes)
    }

    suspend fun createSession(skillId: Long, durationMinutes: Int, notes: String?): Session {
        val request = CreateSessionRequest(
            skillId = skillId,
            sessionDate = Instant.now().toString(),
            durationMinutes = durationMinutes,
            notes = notes
        )
        val dto = api.createSession(request)
        return Session(dto.id, dto.skillId, dto.sessionDate, dto.durationMinutes, dto.notes)
    }
}
