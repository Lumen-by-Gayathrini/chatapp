package com.gayathrini.chatapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.auth.AuthRepository
import com.gayathrini.chatapp.ui.common.toRegisterMessage
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

data class RegisterUiState(
    val displayName: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    val isSubmitting: Boolean = false,
    val displayNameError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val formError: String? = null,
)

sealed interface RegisterEffect {
    data object NavigateHome : RegisterEffect
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    private val _effects = Channel<RegisterEffect>(Channel.BUFFERED)
    val effects: Flow<RegisterEffect> = _effects.receiveAsFlow()

    fun onDisplayNameChange(value: String) =
        _state.update { it.copy(displayName = value, displayNameError = null, formError = null) }

    fun onUsernameChange(value: String) =
        _state.update { it.copy(username = value, usernameError = null, formError = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, passwordError = null, formError = null) }

    fun onConfirmPasswordChange(value: String) =
        _state.update { it.copy(confirmPassword = value, confirmPasswordError = null, formError = null) }

    fun togglePasswordVisibility() = _state.update { it.copy(showPassword = !it.showPassword) }

    fun submit() {
        val current = _state.value
        val displayNameError = if (current.displayName.isBlank()) "Please enter your name." else null
        val usernameError = if (current.username.isBlank()) "Please choose a username." else null
        val passwordError = when {
            current.password.isBlank() -> "Please choose a password."
            current.password.length < 6 -> "Use at least 6 characters."
            else -> null
        }
        val confirmError = if (current.password != current.confirmPassword) "The passwords do not match." else null

        if (displayNameError != null || usernameError != null || passwordError != null || confirmError != null) {
            _state.update {
                it.copy(
                    displayNameError = displayNameError,
                    usernameError = usernameError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError,
                )
            }
            return
        }

        _state.update { it.copy(isSubmitting = true, formError = null) }
        viewModelScope.launch {
            val result = authRepository.register(
                username = current.username.trim(),
                password = current.password,
                displayName = current.displayName.trim(),
            )
            when (result) {
                is AppResult.Success -> {
                    _state.update { it.copy(isSubmitting = false) }
                    _effects.send(RegisterEffect.NavigateHome)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, formError = result.error.toRegisterMessage())
                }
            }
        }
    }
}
