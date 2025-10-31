package com.example.zazeks.domain.user

interface UserRepository {
    suspend fun getProfile(userId: Int): UserProfile
    suspend fun updateProfile(params: UpdateUserProfileParams): UpdateUserProfileResult
}
