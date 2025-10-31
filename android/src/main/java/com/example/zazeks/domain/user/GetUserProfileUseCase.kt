package com.example.zazeks.domain.user

import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(userId: Int): UserProfile = repository.getProfile(userId)
}
