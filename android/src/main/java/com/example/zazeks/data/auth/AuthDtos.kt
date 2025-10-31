package com.example.zazeks.data.auth

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val username: String,
    val password: String,
)

data class RegistrationResponse(
    @SerializedName("userId") val userId: Int,
    val message: String,
)

data class LoginRequest(
    val username: String,
    val password: String,
)

data class LoginResultDto(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("tokenType") val tokenType: String,
    @SerializedName("userId") val userId: Int,
)
