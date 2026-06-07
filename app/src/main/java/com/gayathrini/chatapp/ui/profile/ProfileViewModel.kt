package com.gayathrini.chatapp.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.auth.AuthRepository
import com.gayathrini.chatapp.data.media.MediaReader
import com.gayathrini.chatapp.data.profile.ProfileRepository
import com.gayathrini.chatapp.domain.model.User
import com.gayathrini.chatapp.ui.common.toUserMessage
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

data class ProfileUiState(
    val isLoading: Boolean = true,
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val savedConfirmation: Boolean = false,
    val error: String? = null,
    val showLogoutConfirm: Boolean = false,
)

sealed interface ProfileEffect {
    data object LoggedOut : ProfileEffect
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val mediaReader: MediaReader,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            when (val result = profileRepository.load()) {
                is AppResult.Success -> applyUser(result.data)
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    fun onDisplayNameChange(value: String) =
        _state.update { it.copy(displayName = value, savedConfirmation = false) }

    fun save() {
        val name = _state.value.displayName.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(error = "Please enter your name.") }
            return
        }
        _state.update { it.copy(isSaving = true, error = null, savedConfirmation = false) }
        viewModelScope.launch {
            when (val result = profileRepository.updateDisplayName(name)) {
                is AppResult.Success -> {
                    applyUser(result.data)
                    _state.update { it.copy(isSaving = false, savedConfirmation = true) }
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isSaving = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    fun onAvatarPicked(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true, error = null) }
            val payload = mediaReader.read(uri)
            if (payload == null) {
                _state.update { it.copy(isUploadingAvatar = false, error = "We couldn't read that photo.") }
                return@launch
            }
            when (val result = profileRepository.updateAvatar(payload)) {
                is AppResult.Success -> {
                    applyUser(result.data)
                    _state.update { it.copy(isUploadingAvatar = false) }
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isUploadingAvatar = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun requestLogout() = _state.update { it.copy(showLogoutConfirm = true) }

    fun cancelLogout() = _state.update { it.copy(showLogoutConfirm = false) }

    fun confirmLogout() {
        _state.update { it.copy(showLogoutConfirm = false) }
        viewModelScope.launch {
            authRepository.logout()
            _effects.send(ProfileEffect.LoggedOut)
        }
    }

    private fun applyUser(user: User) {
        _state.update {
            it.copy(
                isLoading = false,
                username = user.username,
                displayName = user.displayName,
                avatarUrl = user.avatarUrl,
            )
        }
    }
}
