package com.example.zazeks.data.user

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {
    @GET("users/{id}")
    suspend fun getUserProfile(@Path("id") userId: Int): UserProfileDto

    @PUT("users/{id}")
    suspend fun updateUserProfile(
        @Path("id") userId: Int,
        @Body request: UpdateUserRequest,
    ): UpdateUserResponse
}
