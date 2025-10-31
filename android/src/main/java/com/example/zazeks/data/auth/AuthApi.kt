package com.example.zazeks.data.auth

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegistrationResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResultDto
}
