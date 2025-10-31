package com.example.zazeks.di

import android.content.Context
import android.content.SharedPreferences
import com.example.zazeks.data.game.DefaultGameEngine
import com.example.zazeks.data.game.storage.SharedPreferencesGameStateRepository
import com.example.zazeks.di.IoDispatcher
import com.example.zazeks.domain.game.AbandonGameUseCase
import com.example.zazeks.domain.game.GameEngine
import com.example.zazeks.domain.game.GameStateRepository
import com.example.zazeks.domain.game.GetActiveGameSnapshotUseCase
import com.example.zazeks.domain.game.GetLastCompletedGameUseCase
import com.example.zazeks.domain.game.ObserveGameStateUseCase
import com.example.zazeks.domain.game.ConfirmRoundResultUseCase
import com.example.zazeks.domain.game.ResumeGameUseCase
import com.example.zazeks.domain.game.RestartRoundUseCase
import com.example.zazeks.domain.game.StartNewGameUseCase
import com.example.zazeks.domain.game.SubmitGestureUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
object GameModule {

    private const val PREFS_NAME = "game_state_preferences"

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideGameStateRepository(
        sharedPreferences: SharedPreferences,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): GameStateRepository = SharedPreferencesGameStateRepository(sharedPreferences, dispatcher)

    @Provides
    @Singleton
    fun provideGameEngine(
        repository: GameStateRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): GameEngine = DefaultGameEngine(repository, dispatcher)

    @Provides
    fun provideObserveGameStateUseCase(engine: GameEngine) = ObserveGameStateUseCase(engine)

    @Provides
    fun provideStartNewGameUseCase(engine: GameEngine) = StartNewGameUseCase(engine)

    @Provides
    fun provideResumeGameUseCase(engine: GameEngine) = ResumeGameUseCase(engine)

    @Provides
    fun provideSubmitGestureUseCase(engine: GameEngine) = SubmitGestureUseCase(engine)

    @Provides
    fun provideRestartRoundUseCase(engine: GameEngine) = RestartRoundUseCase(engine)

    @Provides
    fun provideConfirmRoundResultUseCase(engine: GameEngine) = ConfirmRoundResultUseCase(engine)

    @Provides
    fun provideAbandonGameUseCase(engine: GameEngine) = AbandonGameUseCase(engine)

    @Provides
    fun provideGetActiveGameSnapshotUseCase(repository: GameStateRepository) =
        GetActiveGameSnapshotUseCase(repository)

    @Provides
    fun provideGetLastCompletedGameUseCase(repository: GameStateRepository) =
        GetLastCompletedGameUseCase(repository)
}
