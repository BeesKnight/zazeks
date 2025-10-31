package com.example.zazeks.domain.user

data class UserProfile(
    val id: Int,
    val username: String,
    val photo: String?,
    val wins: Int,
    val gamesPlayed: Int,
    val onlineWins: Int,
    val onlineGames: Int,
) {
    val totalWins: Int get() = wins + onlineWins
    val totalGames: Int get() = gamesPlayed + onlineGames

    val winRate: Float?
        get() = if (totalGames > 0) totalWins.toFloat() / totalGames.toFloat() else null
}
