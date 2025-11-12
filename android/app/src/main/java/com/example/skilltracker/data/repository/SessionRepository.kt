package com.example.skilltracker.data.repository

import com.example.skilltracker.data.api.ApiClient
import com.example.skilltracker.data.dto.CreateSessionRequest
import com.example.skilltracker.data.dto.SessionDto
import com.example.skilltracker.data.dto.UpdateSessionRequest
import com.example.skilltracker.domain.model.Session
import java.time.Instant

class SessionRepository {
    private val api = ApiClient.sessionApi

    suspend fun getSessions(
        skillId: Long? = null,
        from: String? = null,
        to: String? = null
    ): List<Session> =
        api.getSessions(skillId, from, to).map { it.toDomain() }

    suspend fun getSession(id: Long): Session =
        api.getSession(id).toDomain()

    suspend fun createSession(
        skillId: Long,
        durationMinutes: Int,
        notes: String?,
        difficulty: Int?,
        source: String?,
        sessionDate: Instant?
    ): Session {
        val request = CreateSessionRequest(
            skillId = skillId,
            sessionDate = sessionDate.toRequestString(),
            durationMinutes = durationMinutes,
            notes = notes,
            difficulty = difficulty,
            source = source
        )
        val dto = api.createSession(request)
        return dto.toDomain()
    }

    suspend fun updateSession(
        id: Long,
        skillId: Long,
        durationMinutes: Int,
        notes: String?,
        difficulty: Int?,
        source: String?,
        sessionDate: Instant?
    ): Session {
        val request = UpdateSessionRequest(
            skillId = skillId,
            sessionDate = sessionDate.toRequestString(),
            durationMinutes = durationMinutes,
            notes = notes,
            difficulty = difficulty,
            source = source
        )
        val dto = api.updateSession(id, request)
        return dto.toDomain()
    }

    suspend fun deleteSession(id: Long) {
        api.deleteSession(id)
    }

    private fun SessionDto.toDomain(): Session = Session(
        id = id,
        skillId = skillId,
        sessionDate = sessionDate,
        durationMinutes = durationMinutes,
        notes = notes,
        difficulty = difficulty,
        source = source
    )

    private fun Instant?.toRequestString(): String? = this?.toString()
}
