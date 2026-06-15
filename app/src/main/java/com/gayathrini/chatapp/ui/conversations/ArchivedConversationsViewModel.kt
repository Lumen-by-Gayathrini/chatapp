package com.gayathrini.chatapp.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.domain.model.Conversation
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

data class ArchivedConversationsUiState(
    val isLoading: Boolean = true,
    val conversations: List<Conversation> = emptyList(),
    val error: String? = null,
)

sealed interface ArchivedConversationsEffect {
    data class OpenConversation(val conversationId: String) : ArchivedConversationsEffect
}

/** The Archived chats view (TDD §6.23): an ephemeral list with unarchive + open. */
@HiltViewModel
class ArchivedConversationsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ArchivedConversationsUiState())
    val state: StateFlow<ArchivedConversationsUiState> = _state.asStateFlow()

    private val _effects = Channel<ArchivedConversationsEffect>(Channel.BUFFERED)
    val effects: Flow<ArchivedConversationsEffect> = _effects.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = conversationRepository.archivedConversations()) {
                is AppResult.Success -> _state.update { it.copy(isLoading = false, conversations = result.data) }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    fun onConversationClick(conversation: Conversation) {
        viewModelScope.launch { _effects.send(ArchivedConversationsEffect.OpenConversation(conversation.id)) }
    }

    /** Unarchive [conversation] and drop it from this list (it returns to the main list). */
    fun unarchive(conversation: Conversation) {
        _state.update { it.copy(conversations = it.conversations.filterNot { c -> c.id == conversation.id }) }
        viewModelScope.launch { conversationRepository.unarchive(conversation.id) }
    }
}
