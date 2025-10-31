package com.example.zazeks.domain.user

import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(params: UpdateUserProfileParams): UpdateUserProfileResult =
        repository.updateProfile(params)
}
