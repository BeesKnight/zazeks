package com.example.zazeks.infra.config

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Конфиг бэкенда, инжектится через Hilt.
 * Закрывает ошибку: [Dagger/MissingBinding] BackendConfig cannot be provided…
 *
 * Источник baseUrl: assets/config/backend.json  => { "baseUrl": "http://45.155.207.118:8082" }
 */
@Singleton
class BackendConfig @Inject constructor(
    @ApplicationContext context: Context
) {
    /** HTTP база для REST (без завершающего /) */
    val baseUrl: String

    /** WS база для сокетов (если нужно где-то) */
    val wsBaseUrl: String

    init {
        var url = "http://127.0.0.1:8082"
        try {
            context.assets.open("config/backend.json").use { ins ->
                BufferedReader(InputStreamReader(ins)).use { br ->
                    val sb = StringBuilder()
                    var line: String?
                    while (true) {
                        line = br.readLine() ?: break
                        sb.append(line)
                    }
                    val json = JSONObject(sb.toString())
                    url = json.optString("baseUrl", url)
                }
            }
        } catch (_: Exception) {
            // оставляем дефолт
        }
        // нормализуем
        val normalized = if (url.endsWith("/")) url.dropLast(1) else url
        baseUrl = normalized
        wsBaseUrl = when {
            normalized.startsWith("https://") -> "wss://" + normalized.removePrefix("https://")
            normalized.startsWith("http://")  -> "ws://"  + normalized.removePrefix("http://")
            else -> "ws://$normalized"
        }
    }

    override fun toString(): String = "BackendConfig(baseUrl=$baseUrl, wsBaseUrl=$wsBaseUrl)"
}
