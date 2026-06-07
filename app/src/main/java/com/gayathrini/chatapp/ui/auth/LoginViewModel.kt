package com.gayathrini.chatapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.auth.AuthRepository
import com.gayathrini.chatapp.ui.common.toLoginMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val isSubmitting: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null,
)

sealed interface LoginEffect {
    data object NavigateHome : LoginEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects: Flow<LoginEffect> = _effects.receiveAsFlow()

    fun onUsernameChange(value: String) =
        _state.update { it.copy(username = value, usernameError = null, formError = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, passwordError = null, formError = null) }

    fun togglePasswordVisibility() = _state.update { it.copy(showPassword = !it.showPassword) }

    fun submit() {
        val current = _state.value
        val usernameError = if (current.username.isBlank()) "Please enter your username." else null
        val passwordError = if (current.password.isBlank()) "Please enter your password." else null
        if (usernameError != null || passwordError != null) {
            _state.update { it.copy(usernameError = usernameError, passwordError = passwordError) }
            return
        }

        _state.update { it.copy(isSubmitting = true, formError = null) }
        viewModelScope.launch {
            when (val result = authRepository.login(current.username.trim(), current.password)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isSubmitting = false) }
                    _effects.send(LoginEffect.NavigateHome)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, formError = result.error.toLoginMessage())
                }
            }
        }
    }
}
