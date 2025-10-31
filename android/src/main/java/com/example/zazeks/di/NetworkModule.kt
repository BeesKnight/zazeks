package com.example.zazeks.di

import com.example.zazeks.data.auth.AuthApi
import com.example.zazeks.data.results.GameResultsApi
import com.example.zazeks.data.user.UserApi
import com.example.zazeks.infra.auth.AuthInterceptor
import com.example.zazeks.infra.config.BackendConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .addInterceptor(authInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(backendConfig: BackendConfig, okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(backendConfig.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideGameResultsApi(retrofit: Retrofit): GameResultsApi =
        retrofit.create(GameResultsApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)
}
