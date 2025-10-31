package com.example.zazeks.data.results

import retrofit2.http.GET
import retrofit2.http.Path

interface GameResultsApi {
    @GET("games/user/{userId}")
    suspend fun getUserGames(@Path("userId") userId: Int): List<GameResultDto>
}
