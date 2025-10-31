package com.example.zazeks.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazeks.data.auth.AuthRepository
import com.example.zazeks.data.auth.AuthRepositoryException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthViewState())
    val state: StateFlow<AuthViewState> = _state.asStateFlow()

    private val eventsChannel = Channel<AuthEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private var hasNavigated = false

    init {
        observeAuthState()
    }

    fun selectTab(tab: AuthTab) {
        _state.update {
            it.copy(
                selectedTab = tab,
                usernameError = null,
                passwordError = null,
                confirmPasswordError = null,
                generalError = null,
            )
        }
    }

    fun submitLogin(username: String, password: String) {
        val trimmedUsername = username.trim()
        val usernameError = validateUsername(trimmedUsername)
        val passwordError = validatePassword(password)
        if (usernameError != null || passwordError != null) {
            _state.update {
                it.copy(
                    usernameError = usernameError,
                    passwordError = passwordError,
                    generalError = null,
                )
            }
            return
        }
        _state.update {
            it.copy(
                isLoading = true,
                usernameError = null,
                passwordError = null,
                generalError = null,
            )
        }
        viewModelScope.launch {
            val result = authRepository.login(trimmedUsername, password)
            result.fold(
                onSuccess = {
                    _state.update { current -> current.copy(isLoading = false) }
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            generalError = resolveMessage(throwable, "Не удалось выполнить вход"),
                        )
                    }
                },
            )
        }
    }

    fun submitRegistration(username: String, password: String, confirmation: String) {
        val trimmedUsername = username.trim()
        val usernameError = validateUsername(trimmedUsername)
        val passwordError = validatePassword(password)
        val confirmationError = if (password == confirmation) null else "Пароли не совпадают"
        if (usernameError != null || passwordError != null || confirmationError != null) {
            _state.update {
                it.copy(
                    usernameError = usernameError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmationError,
                    generalError = null,
                )
            }
            return
        }
        _state.update {
            it.copy(
                isLoading = true,
                usernameError = null,
                passwordError = null,
                confirmPasswordError = null,
                generalError = null,
            )
        }
        viewModelScope.launch {
            val result = authRepository.register(trimmedUsername, password)
            result.fold(
                onSuccess = { registration ->
                    _state.update { current -> current.copy(isLoading = false) }
                    selectTab(AuthTab.LOGIN)
                    eventsChannel.trySend(AuthEvent.ShowMessage(registration.message))
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            generalError = resolveMessage(throwable, "Не удалось зарегистрироваться"),
                        )
                    }
                },
            )
        }
    }

    fun consumeError() {
        _state.update { it.copy(generalError = null) }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.isAuthenticated.collectLatest { isAuthenticated ->
                if (isAuthenticated) {
                    if (!hasNavigated) {
                        hasNavigated = true
                        eventsChannel.send(AuthEvent.NavigateToMain)
                    }
                } else {
                    hasNavigated = false
                }
            }
        }
    }

    private fun validateUsername(username: String): String? = when {
        username.isBlank() -> "Введите имя пользователя"
        !USERNAME_REGEX.matches(username) -> "4–15 символов: буквы, цифры или _"
        else -> null
    }

    private fun validatePassword(password: String): String? = when {
        password.isBlank() -> "Введите пароль"
        !USERNAME_REGEX.matches(password) -> "4–15 символов: буквы, цифры или _"
        else -> null
    }

    private fun resolveMessage(throwable: Throwable, defaultMessage: String): String = when (throwable) {
        is AuthRepositoryException -> throwable.message ?: defaultMessage
        else -> throwable.message ?: defaultMessage
    }

    companion object {
        private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{4,15}$")
    }
}

data class AuthViewState(
    val selectedTab: AuthTab = AuthTab.LOGIN,
    val isLoading: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null,
)

enum class AuthTab { LOGIN, REGISTER }

sealed interface AuthEvent {
    object NavigateToMain : AuthEvent
    data class ShowMessage(val message: String) : AuthEvent
}
