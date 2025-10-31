package com.example.zazeks.data.results

import com.squareup.moshi.Json

data class GameResultDto(
    @Json(name = "id") val id: Int?,
    @Json(name = "userId") val userId: Int,
    @Json(name = "userChoice") val userChoice: String?,
    @Json(name = "computerChoice") val computerChoice: String?,
    @Json(name = "result") val result: String?,
    @Json(name = "timestamp") val timestamp: String?
)
