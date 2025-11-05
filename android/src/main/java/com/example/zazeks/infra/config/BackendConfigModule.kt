package com.example.zazeks.infra.config

import android.content.Context
import com.example.zazeks.infra.ml.NeuralModelBridge
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackendConfigModule {

    @Provides
    @Singleton
    fun provideNeuralModelBridge(@ApplicationContext context: Context): NeuralModelBridge {
        // NeuralModelBridge сам прочитает baseUrl из assets/config/backend.json
        return NeuralModelBridge(context)
    }
}
