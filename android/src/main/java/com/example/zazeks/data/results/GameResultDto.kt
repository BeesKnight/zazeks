package com.example.zazeks.data.results

import com.google.gson.annotations.SerializedName

data class GameResultDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("userId") val userId: Int,
    @SerializedName("userChoice") val userChoice: String?,
    @SerializedName("computerChoice") val computerChoice: String?,
    @SerializedName("result") val result: String?,
    @SerializedName("timestamp") val timestamp: String?
)
