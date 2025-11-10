package com.example.skilltracker.data.api

import com.example.skilltracker.data.dto.CreateSessionRequest
import com.example.skilltracker.data.dto.SessionDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SessionApi {
    @GET("api/sessions")
    suspend fun getSessions(
        @Query("skillId") skillId: Long? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): List<SessionDto>

    @GET("api/sessions/{id}")
    suspend fun getSession(@Path("id") id: Long): SessionDto

    @POST("api/sessions")
    suspend fun createSession(@Body request: CreateSessionRequest): SessionDto
}
