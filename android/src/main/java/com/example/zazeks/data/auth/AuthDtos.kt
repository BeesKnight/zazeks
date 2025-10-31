package com.example.zazeks.data.auth

import com.squareup.moshi.Json

data class RegisterRequest(
    val username: String,
    val password: String,
)

data class RegistrationResponse(
    @Json(name = "userId") val userId: Int,
    val message: String,
)

data class LoginRequest(
    val username: String,
    val password: String,
)

data class LoginResultDto(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "tokenType") val tokenType: String,
    @Json(name = "userId") val userId: Int,
)
