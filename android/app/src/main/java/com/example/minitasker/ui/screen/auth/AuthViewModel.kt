package com.example.minitasker.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minitasker.data.repository.AuthRepository
import com.example.minitasker.data.repository.NetworkResult
import com.example.minitasker.data.repository.safeCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val fullName: String = "",
    val isRegister: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun updateEmail(value: String) {
        _state.value = _state.value.copy(email = value)
    }

    fun updatePassword(value: String) {
        _state.value = _state.value.copy(password = value)
    }

    fun updateFullName(value: String) {
        _state.value = _state.value.copy(fullName = value)
    }

    fun toggleMode() {
        _state.value = _state.value.copy(isRegister = !_state.value.isRegister, error = null)
    }

    fun reset() {
        _state.value = AuthUiState()
    }

    fun submit() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val result = if (_state.value.isRegister) {
                safeCall {
                    authRepository.register(
                        _state.value.email,
                        _state.value.password,
                        _state.value.fullName
                    )
                }
            } else {
                safeCall { authRepository.login(_state.value.email, _state.value.password) }
            }
            _state.value = when (result) {
                is NetworkResult.Success -> _state.value.copy(loading = false, isAuthenticated = true)
                is NetworkResult.Error -> _state.value.copy(loading = false, error = result.message)
                NetworkResult.Loading -> _state.value.copy(loading = true)
            }
        }
    }
}
