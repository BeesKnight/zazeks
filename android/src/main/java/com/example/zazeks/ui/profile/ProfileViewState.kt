package com.example.zazeks.ui.profile

import com.example.zazeks.domain.user.UserProfile

data class ProfileViewState(
    val isLoading: Boolean = true,
    val loadError: ProfileViewModel.ProfileError? = null,
    val profile: UserProfile? = null,
    val isEditing: Boolean = false,
    val usernameInput: String = "",
    val pendingAvatarBase64: String? = null,
    val isSaving: Boolean = false,
)
