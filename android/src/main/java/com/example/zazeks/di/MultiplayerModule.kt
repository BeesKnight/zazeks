package com.example.zazeks.di

import android.content.SharedPreferences
import com.example.zazeks.data.multiplayer.MultiplayerStatsRepository
import com.example.zazeks.data.multiplayer.SharedPreferencesMultiplayerStatsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MultiplayerModule {

    @Provides
    @Singleton
    fun provideMultiplayerStatsRepository(
        sharedPreferences: SharedPreferences,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): MultiplayerStatsRepository = SharedPreferencesMultiplayerStatsRepository(sharedPreferences, dispatcher)
}
