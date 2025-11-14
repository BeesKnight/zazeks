package com.example.minitasker.data.repository

import com.example.minitasker.data.api.ApiService
import com.example.minitasker.data.local.AuthPreferences
import com.example.minitasker.data.model.AuthResponse
import com.example.minitasker.data.model.LoginRequest
import com.example.minitasker.data.model.RegisterRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val apiService: ApiService,
    private val authPreferences: AuthPreferences
) {

    val tokenFlow: Flow<String?> = authPreferences.tokenFlow
    val roleFlow: Flow<String?> = authPreferences.roleFlow

    suspend fun register(email: String, password: String, fullName: String): AuthResponse {
        val response = apiService.register(RegisterRequest(email, password, fullName))
        authPreferences.saveAuth(response.token, response.email, response.fullName, response.role)
        return response
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val response = apiService.login(LoginRequest(email, password))
        authPreferences.saveAuth(response.token, response.email, response.fullName, response.role)
        return response
    }

    suspend fun logout() {
        authPreferences.clear()
    }

    suspend fun currentToken(): String? = tokenFlow.first()
}
