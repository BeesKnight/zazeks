package com.example.zazeks.di

import com.example.zazeks.data.results.GameResultsApi
import com.example.zazeks.data.results.NetworkGameResultsRepository
import com.example.zazeks.domain.results.GameResultsRepository
import com.example.zazeks.domain.results.GetUserGameRoundsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ResultsModule {

    @Provides
    @Singleton
    fun provideGameResultsRepository(api: GameResultsApi): GameResultsRepository =
        NetworkGameResultsRepository(api)

    @Provides
    fun provideGetUserGameRoundsUseCase(repository: GameResultsRepository) =
        GetUserGameRoundsUseCase(repository)
}
