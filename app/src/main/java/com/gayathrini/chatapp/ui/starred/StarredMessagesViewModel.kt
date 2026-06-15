package com.gayathrini.chatapp.ui.starred

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.messages.MessageRepository
import com.gayathrini.chatapp.domain.model.Message
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

data class StarredMessagesUiState(
    val isLoading: Boolean = true,
    val messages: List<Message> = emptyList(),
    val error: String? = null,
)

sealed interface StarredMessagesEffect {
    data class OpenConversation(val conversationId: String) : StarredMessagesEffect
}

/** The Starred messages view (TDD §6.24): an ephemeral list with unstar + open-in-chat. */
@HiltViewModel
class StarredMessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StarredMessagesUiState())
    val state: StateFlow<StarredMessagesUiState> = _state.asStateFlow()

    private val _effects = Channel<StarredMessagesEffect>(Channel.BUFFERED)
    val effects: Flow<StarredMessagesEffect> = _effects.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = messageRepository.starredMessages()) {
                is AppResult.Success -> _state.update { it.copy(isLoading = false, messages = result.data) }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    fun onMessageClick(message: Message) {
        viewModelScope.launch { _effects.send(StarredMessagesEffect.OpenConversation(message.conversationId)) }
    }

    /** Unstar [message] and drop it from this list (TDD §6.24). */
    fun unstar(message: Message) {
        _state.update { it.copy(messages = it.messages.filterNot { m -> m.id == message.id }) }
        viewModelScope.launch { messageRepository.unstar(message.conversationId, message.id) }
    }
}
