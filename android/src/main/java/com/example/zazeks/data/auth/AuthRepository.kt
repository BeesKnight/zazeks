package com.example.zazeks.data.auth

import com.example.zazeks.domain.auth.LoginResult
import com.example.zazeks.domain.auth.RegistrationResult
import com.example.zazeks.infra.auth.AuthSession
import com.example.zazeks.infra.auth.AuthTokenStorage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStorage: AuthTokenStorage,
) {

    val isAuthenticated: Flow<Boolean> = tokenStorage.session.map { session -> session != null }

    suspend fun register(username: String, password: String): Result<RegistrationResult> =
        runCatching {
            api.register(RegisterRequest(username, password)).toDomain()
        }.mapError("Не удалось зарегистрироваться")

    suspend fun login(username: String, password: String): Result<LoginResult> =
        runCatching {
            val result = api.login(LoginRequest(username, password))
            val session = AuthSession(result.accessToken, result.tokenType, result.userId)
            tokenStorage.persist(session)
            result.toDomain()
        }.mapError("Не удалось выполнить вход")

    suspend fun logout() {
        tokenStorage.clear()
    }

    private fun RegistrationResponse.toDomain() = RegistrationResult(userId = userId, message = message)

    private fun LoginResultDto.toDomain() = LoginResult(
        accessToken = accessToken,
        tokenType = tokenType,
        userId = userId,
    )

    private fun <T> Result<T>.mapError(defaultMessage: String): Result<T> = fold(
        onSuccess = { Result.success(it) },
        onFailure = { throwable -> Result.failure(mapException(throwable, defaultMessage)) },
    )

    private fun mapException(throwable: Throwable, defaultMessage: String): Exception = when (throwable) {
        is HttpException -> {
            val errorBody = throwable.response()?.errorBody()?.string().orEmpty()
            val message = errorBody.takeIf { it.isNotBlank() } ?: defaultMessage
            AuthRepositoryException(message, throwable)
        }
        is IOException -> AuthRepositoryException("Проверьте подключение к сети", throwable)
        else -> AuthRepositoryException(throwable.message ?: defaultMessage, throwable)
    }
}

class AuthRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
