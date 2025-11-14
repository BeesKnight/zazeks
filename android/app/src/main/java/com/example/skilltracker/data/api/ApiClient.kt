package com.example.skilltracker.data.api

import android.os.Build
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiClient {
    private const val EMULATOR_BASE_URL = "http://10.0.2.2:8080/"
    private const val DEVICE_BASE_URL = "http://127.0.0.1:8080/"

    private val baseUrl: String
        get() = if (isRunningOnEmulator()) EMULATOR_BASE_URL else DEVICE_BASE_URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
        .build()

    val skillApi: SkillApi = retrofit.create(SkillApi::class.java)
    val sessionApi: SessionApi = retrofit.create(SessionApi::class.java)
    val statsApi: StatsApi = retrofit.create(StatsApi::class.java)

    private fun isRunningOnEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT
        val model = Build.MODEL
        val brand = Build.BRAND
        val device = Build.DEVICE
        return fingerprint.contains("generic", ignoreCase = true) ||
            fingerprint.contains("emulator", ignoreCase = true) ||
            model.contains("google_sdk", ignoreCase = true) ||
            model.contains("droid4x", ignoreCase = true) ||
            model.contains("emulator", ignoreCase = true) ||
            model.contains("android sdk built for x86", ignoreCase = true) ||
            brand.startsWith("generic") && device.startsWith("generic") ||
            "google_sdk" == Build.PRODUCT
    }
}
