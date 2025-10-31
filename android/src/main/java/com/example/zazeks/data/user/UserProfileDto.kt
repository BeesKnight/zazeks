package com.example.zazeks.data.user

data class UserProfileDto(
    val id: Int?,
    val username: String?,
    val photo: String?,
    val wins: Int?,
    val gamesPlayed: Int?,
    val onlineWins: Int?,
    val onlineGames: Int?,
)

data class UpdateUserRequest(
    val username: String?,
    val photo: String?,
)

data class UpdateUserResponse(
    val message: String?,
    val user: UserProfileDto?,
)
