package com.example.zazeks.domain.user

data class UpdateUserProfileParams(
    val userId: Int,
    val username: String?,
    val photoBase64: String?,
)
