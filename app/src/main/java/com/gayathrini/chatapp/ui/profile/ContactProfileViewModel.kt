package com.gayathrini.chatapp.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.navigation.Screen
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.data.user.UserRepository
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

/** UI state for the contact-profile screen (TDD §6.2). */
data class ContactProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val isStartingChat: Boolean = false,
    val isUpdatingBlock: Boolean = false,
    val error: String? = null,
) {
    val isBlocked: Boolean get() = user?.blocked == true
}

sealed interface ContactProfileEffect {
    data class OpenConversation(val conversationId: String) : ContactProfileEffect
}

@HiltViewModel
class ContactProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle[Screen.ContactProfile.ARG])

    private val _state = MutableStateFlow(ContactProfileUiState())
    val state: StateFlow<ContactProfileUiState> = _state.asStateFlow()

    private val _effects = Channel<ContactProfileEffect>(Channel.BUFFERED)
    val effects: Flow<ContactProfileEffect> = _effects.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = userRepository.getProfile(userId)) {
                is AppResult.Success -> _state.update { it.copy(isLoading = false, user = result.data) }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    /** Open (or create) the 1:1 conversation with this contact, then navigate to it. */
    fun startChat() {
        if (_state.value.isStartingChat) return
        _state.update { it.copy(isStartingChat = true, error = null) }
        viewModelScope.launch {
            when (val result = conversationRepository.createConversation(userId)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isStartingChat = false) }
                    _effects.send(ContactProfileEffect.OpenConversation(result.data.id))
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isStartingChat = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    /** Block or unblock this user (TDD §6.19), then reload the profile to refresh state/presence. */
    fun toggleBlock() {
        if (_state.value.isUpdatingBlock) return
        val blocked = _state.value.isBlocked
        _state.update { it.copy(isUpdatingBlock = true, error = null) }
        viewModelScope.launch {
            val result = if (blocked) userRepository.unblockUser(userId) else userRepository.blockUser(userId)
            when (result) {
                is AppResult.Success -> {
                    _state.update { it.copy(isUpdatingBlock = false) }
                    load() // refreshes `blocked` + hidden presence
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isUpdatingBlock = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}
