package com.example.zazeks.di

import com.example.zazeks.data.user.NetworkUserRepository
import com.example.zazeks.domain.user.GetUserProfileUseCase
import com.example.zazeks.domain.user.UpdateUserProfileUseCase
import com.example.zazeks.domain.user.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserModule {

    @Provides
    @Singleton
    fun provideUserRepository(repository: NetworkUserRepository): UserRepository = repository

    @Provides
    @Singleton
    fun provideGetUserProfileUseCase(repository: UserRepository): GetUserProfileUseCase =
        GetUserProfileUseCase(repository)

    @Provides
    @Singleton
    fun provideUpdateUserProfileUseCase(repository: UserRepository): UpdateUserProfileUseCase =
        UpdateUserProfileUseCase(repository)
}
