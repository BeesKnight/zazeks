package com.example.zazeks.domain.auth

data class LoginResult(
    val accessToken: String,
    val tokenType: String,
    val userId: Int,
)
