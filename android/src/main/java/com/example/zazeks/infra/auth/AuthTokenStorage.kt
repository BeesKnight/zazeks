package com.example.zazeks.infra.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

@Singleton
class AuthTokenStorage @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val session: Flow<AuthSession?> = dataStore.data.map { preferences ->
        val token = preferences[ACCESS_TOKEN_KEY]
        if (token.isNullOrBlank()) {
            null
        } else {
            AuthSession(
                accessToken = token,
                tokenType = preferences[TOKEN_TYPE_KEY].orEmpty(),
                userId = preferences[USER_ID_KEY]
            )
        }
    }

    suspend fun persist(session: AuthSession) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = session.accessToken
            preferences[TOKEN_TYPE_KEY] = session.tokenType.ifBlank { DEFAULT_TOKEN_TYPE }
            session.userId?.let { preferences[USER_ID_KEY] = it } ?: preferences.remove(USER_ID_KEY)
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(TOKEN_TYPE_KEY)
            preferences.remove(USER_ID_KEY)
        }
    }

    suspend fun currentSession(): AuthSession? = session.firstOrNull()

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val TOKEN_TYPE_KEY = stringPreferencesKey("token_type")
        private val USER_ID_KEY = intPreferencesKey("user_id")
        private const val DEFAULT_TOKEN_TYPE = "Bearer"
    }
}

data class AuthSession(
    val accessToken: String,
    val tokenType: String,
    val userId: Int?,
)
