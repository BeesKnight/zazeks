package com.example.zazeks.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazeks.domain.user.GetUserProfileUseCase
import com.example.zazeks.domain.user.UpdateUserProfileParams
import com.example.zazeks.domain.user.UpdateUserProfileUseCase
import com.example.zazeks.domain.user.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfile: GetUserProfileUseCase,
    private val updateUserProfile: UpdateUserProfileUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val userId: Int = savedStateHandle.get<Int>(ProfileFragment.ARG_USER_ID) ?: INVALID_USER_ID

    private val mutableState = MutableLiveData(ProfileViewState())
    val state: LiveData<ProfileViewState> = mutableState

    private val mutableEvents = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = mutableEvents.asSharedFlow()

    init {
        if (userId == INVALID_USER_ID) {
            mutableState.value = ProfileViewState(
                isLoading = false,
                loadError = ProfileError.InvalidUser,
            )
        } else {
            loadProfile()
        }
    }

    fun loadProfile() {
        if (userId == INVALID_USER_ID) return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, loadError = null) }
            runCatching { getUserProfile(userId) }
                .onSuccess { profile ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            loadError = null,
                            profile = profile,
                            usernameInput = profile.username,
                            pendingAvatarBase64 = null,
                            isEditing = false,
                        )
                    }
                }
                .onFailure { error ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            loadError = ProfileError.Load(error.message),
                        )
                    }
                }
        }
    }

    fun startEditing() {
        val profile = state.value?.profile ?: return
        updateState {
            it.copy(
                isEditing = true,
                usernameInput = profile.username,
                pendingAvatarBase64 = null,
            )
        }
    }

    fun cancelEditing() {
        val profile = state.value?.profile
        updateState {
            it.copy(
                isEditing = false,
                usernameInput = profile?.username.orEmpty(),
                pendingAvatarBase64 = null,
                isSaving = false,
            )
        }
    }

    fun onUsernameChanged(value: String) {
        updateState { it.copy(usernameInput = value) }
    }

    fun onAvatarSelected(base64: String) {
        updateState { it.copy(pendingAvatarBase64 = base64) }
    }

    fun saveChanges() {
        val current = state.value ?: return
        val profile = current.profile ?: return
        if (!current.isEditing || current.isSaving) return

        val username = current.usernameInput.trim().takeIf { it.isNotBlank() && it != profile.username }
        val photo = current.pendingAvatarBase64
        if (username == null && photo == null) {
            cancelEditing()
            return
        }
        updateState { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                updateUserProfile(
                    UpdateUserProfileParams(
                        userId = profile.id,
                        username = username,
                        photoBase64 = photo,
                    )
                )
            }
                .onSuccess { result ->
                    updateState {
                        it.copy(
                            profile = result.profile,
                            usernameInput = result.profile.username,
                            pendingAvatarBase64 = null,
                            isEditing = false,
                            isSaving = false,
                        )
                    }
                    mutableEvents.emit(ProfileEvent.ShowMessage(result.message))
                    mutableEvents.emit(ProfileEvent.ProfileUpdated(result.profile))
                }
                .onFailure { error ->
                    updateState { it.copy(isSaving = false) }
                    val message = error.message ?: ""
                    mutableEvents.emit(ProfileEvent.ShowError(message))
                }
        }
    }

    private fun updateState(transform: (ProfileViewState) -> ProfileViewState) {
        val current = mutableState.value ?: ProfileViewState()
        mutableState.value = transform(current)
    }

    sealed class ProfileError {
        data object InvalidUser : ProfileError()
        data class Load(val message: String?) : ProfileError()
    }

    companion object {
        private const val INVALID_USER_ID = -1
    }
}

sealed interface ProfileEvent {
    data class ShowMessage(val message: String?) : ProfileEvent
    data class ShowError(val message: String) : ProfileEvent
    data class ProfileUpdated(val profile: UserProfile) : ProfileEvent
}
