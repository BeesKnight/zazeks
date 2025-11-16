package com.example.minitasker.data.repository

import android.content.Context
import com.example.minitasker.data.api.ApiService
import com.example.minitasker.data.api.AuthInterceptor
import com.example.minitasker.data.local.AuthPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ServiceLocator {

    private const val BASE_URL = "http://127.0.0.1:8080"

    fun provideAuthPreferences(context: Context): AuthPreferences = AuthPreferences(context)

    fun provideApiService(context: Context): ApiService {
        val authPreferences = provideAuthPreferences(context)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authPreferences))
            .addInterceptor(logging)
            .build()
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return retrofit.create(ApiService::class.java)
    }

    fun provideAuthRepository(context: Context): AuthRepository {
        val preferences = provideAuthPreferences(context)
        val api = provideApiService(context)
        return AuthRepository(api, preferences)
    }

    fun provideProjectRepository(context: Context): ProjectRepository {
        return ProjectRepository(provideApiService(context))
    }

    fun provideTaskRepository(context: Context): TaskRepository {
        return TaskRepository(context, provideApiService(context))
    }

    fun provideStatsRepository(context: Context): StatsRepository {
        return StatsRepository(provideApiService(context))
    }
}
