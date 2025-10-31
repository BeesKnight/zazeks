package com.example.zazeks.data.user

import android.util.Base64
import com.example.zazeks.domain.user.UpdateUserProfileParams
import com.example.zazeks.domain.user.UpdateUserProfileResult
import com.example.zazeks.domain.user.UserProfile
import com.example.zazeks.domain.user.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUserRepository @Inject constructor(
    private val api: UserApi,
) : UserRepository {

    override suspend fun getProfile(userId: Int): UserProfile =
        api.getUserProfile(userId).toDomain()

    override suspend fun updateProfile(params: UpdateUserProfileParams): UpdateUserProfileResult {
        validatePhoto(params.photoBase64)
        val response = api.updateUserProfile(
            params.userId,
            UpdateUserRequest(
                username = params.username?.takeIf { it.isNotBlank() },
                photo = params.photoBase64?.takeIf { it.isNotBlank() },
            )
        )
        val profileDto = response.user ?: throw UserRepositoryException("Отсутствуют данные профиля")
        return UpdateUserProfileResult(
            message = response.message ?: "Профиль обновлён",
            profile = profileDto.toDomain(),
        )
    }

    private fun validatePhoto(photo: String?) {
        if (photo.isNullOrBlank()) return
        val parts = photo.split(",", limit = 2)
        if (parts.size != 2) {
            throw UserRepositoryException("Некорректный формат изображения")
        }
        val header = parts[0].lowercase()
        if (!header.startsWith("data:image/")) {
            throw UserRepositoryException("Некорректный формат изображения")
        }
        val mimeType = header.substringAfter("data:image/").substringBefore(";")
        if (mimeType != "png" && mimeType != "jpeg" && mimeType != "jpg") {
            throw UserRepositoryException("Допускаются только PNG или JPG изображения")
        }
        val decoded = try {
            Base64.decode(parts[1], Base64.DEFAULT)
        } catch (error: IllegalArgumentException) {
            throw UserRepositoryException("Некорректный формат изображения", error)
        }
        if (decoded.size > MAX_IMAGE_SIZE_BYTES) {
            throw UserRepositoryException("Размер изображения не должен превышать 5 МБ")
        }
    }

    private fun UserProfileDto.toDomain(): UserProfile = UserProfile(
        id = id ?: throw UserRepositoryException("Некорректные данные профиля"),
        username = username.orEmpty(),
        photo = photo,
        wins = wins ?: 0,
        gamesPlayed = gamesPlayed ?: 0,
        onlineWins = onlineWins ?: 0,
        onlineGames = onlineGames ?: 0,
    )

    companion object {
        private const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
    }
}

class UserRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
