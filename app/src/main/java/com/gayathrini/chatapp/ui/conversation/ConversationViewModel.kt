package com.gayathrini.chatapp.ui.conversation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.navigation.Screen
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.data.media.MediaReader
import com.gayathrini.chatapp.data.messages.MessageRepository
import com.gayathrini.chatapp.domain.model.Message
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

data class ConversationUiState(
    val peerName: String = "",
    val isLoading: Boolean = true,
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    val isUploading: Boolean = false,
    val error: String? = null,
    val pendingDelete: Boolean = false,
)

sealed interface ConversationEffect {
    data object NavigateBack : ConversationEffect
}

@HiltViewModel
class ConversationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val mediaReader: MediaReader,
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle[Screen.Conversation.ARG])

    private val _state = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = _state.asStateFlow()

    private val _effects = Channel<ConversationEffect>(Channel.BUFFERED)
    val effects: Flow<ConversationEffect> = _effects.receiveAsFlow()

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            messageRepository.messages(conversationId).collect { list ->
                _state.update { it.copy(messages = list, isLoading = false) }
            }
        }
        viewModelScope.launch {
            conversationRepository.conversations.collect { conversations ->
                conversations.firstOrNull { it.id == conversationId }?.let { conversation ->
                    _state.update { it.copy(peerName = conversation.peer.displayName) }
                }
            }
        }
        viewModelScope.launch { messageRepository.loadInitial(conversationId) }
        viewModelScope.launch { messageRepository.markRead(conversationId) }
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                messageRepository.poll(conversationId)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun onDraftChange(value: String) = _state.update { it.copy(draft = value) }

    fun sendText() {
        val text = _state.value.draft.trim()
        if (text.isEmpty()) return
        _state.update { it.copy(draft = "") }
        viewModelScope.launch { messageRepository.sendText(conversationId, text) }
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, error = null) }
            val payload = mediaReader.read(uri)
            if (payload == null) {
                _state.update { it.copy(isUploading = false, error = "We couldn't read that photo. Please try another.") }
                return@launch
            }
            when (val result = messageRepository.sendImage(conversationId, payload, uri.toString())) {
                is AppResult.Success -> _state.update { it.copy(isUploading = false) }
                is AppResult.Failure -> _state.update {
                    it.copy(isUploading = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    fun retry(clientId: String) {
        viewModelScope.launch { messageRepository.retry(conversationId, clientId) }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun requestDelete() = _state.update { it.copy(pendingDelete = true) }

    fun cancelDelete() = _state.update { it.copy(pendingDelete = false) }

    fun confirmDelete() {
        _state.update { it.copy(pendingDelete = false) }
        viewModelScope.launch {
            conversationRepository.deleteConversation(conversationId)
            _effects.send(ConversationEffect.NavigateBack)
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 3_000L
    }
}
