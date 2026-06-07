package com.gayathrini.chatapp.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsUiState(
    val isLoading: Boolean = true,
    val conversations: List<Conversation> = emptyList(),
    val error: String? = null,
    val pendingDelete: Conversation? = null,
)

sealed interface ConversationsEffect {
    data class OpenConversation(val conversationId: String) : ConversationsEffect
}

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConversationsUiState())
    val state: StateFlow<ConversationsUiState> = _state.asStateFlow()

    private val _effects = Channel<ConversationsEffect>(Channel.BUFFERED)
    val effects: Flow<ConversationsEffect> = _effects.receiveAsFlow()

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            conversationRepository.conversations.collect { list ->
                _state.update { it.copy(conversations = list, isLoading = false) }
            }
        }
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(error = null) }
        viewModelScope.launch {
            when (val result = conversationRepository.refresh()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> _state.update {
                    it.copy(error = result.error.toUserMessage(), isLoading = false)
                }
            }
        }
    }

    /** Starts foreground polling (TDD §7.7). Idempotent; the screen calls this on ON_RESUME. */
    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                conversationRepository.sync()
            }
        }
    }

    /** Stops polling; the screen calls this on ON_PAUSE / dispose. */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun onConversationClick(conversation: Conversation) {
        viewModelScope.launch { _effects.send(ConversationsEffect.OpenConversation(conversation.id)) }
    }

    fun createConversation(peerUserId: String) {
        viewModelScope.launch {
            when (val result = conversationRepository.createConversation(peerUserId)) {
                is AppResult.Success -> _effects.send(ConversationsEffect.OpenConversation(result.data.id))
                is AppResult.Failure -> _state.update { it.copy(error = result.error.toUserMessage()) }
            }
        }
    }

    fun requestDelete(conversation: Conversation) = _state.update { it.copy(pendingDelete = conversation) }

    fun cancelDelete() = _state.update { it.copy(pendingDelete = null) }

    fun confirmDelete() {
        val target = _state.value.pendingDelete ?: return
        _state.update { it.copy(pendingDelete = null) }
        viewModelScope.launch { conversationRepository.deleteConversation(target.id) }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 5_000L
    }
}
