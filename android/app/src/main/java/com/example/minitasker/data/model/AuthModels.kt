package com.example.minitasker.data.model

data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val userId: Long,
    val email: String,
    val fullName: String,
    val role: String
)

data class UserDto(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: String,
    val createdAt: String
)
