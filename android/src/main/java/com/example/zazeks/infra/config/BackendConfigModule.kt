package com.example.zazeks.infra.config

import android.content.Context
import com.example.zazeks.infra.ml.NeuralModelBridge
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.json.JSONException
import org.json.JSONObject

@Module
@InstallIn(SingletonComponent::class)
object BackendConfigModule {
    private const val CONFIG_PATH = "config/backend.json"
    private const val BASE_URL_KEY = "baseUrl"

    @Provides
    @Singleton
    fun provideBackendConfig(@ApplicationContext context: Context): BackendConfig {
        val content = context.assets.open(CONFIG_PATH).bufferedReader().use { it.readText() }
        val baseUrl = try {
            val json = JSONObject(content)
            json.optString(BASE_URL_KEY)
        } catch (exception: JSONException) {
            throw IllegalStateException("Invalid backend config JSON", exception)
        }
        if (baseUrl.isBlank()) {
            throw IllegalStateException("Invalid backend config: $BASE_URL_KEY is missing or blank")
        }
        return BackendConfig(baseUrl)
    }

    @Provides
    @Singleton
    fun provideNeuralModelBridge(config: BackendConfig): NeuralModelBridge =
        NeuralModelBridge.remoteHttp(config.baseUrl)
}
