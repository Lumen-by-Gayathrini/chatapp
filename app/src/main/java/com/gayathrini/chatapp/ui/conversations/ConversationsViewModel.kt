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
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ConversationsUiState(
    val isLoading: Boolean = true,
    val conversations: List<Conversation> = emptyList(),
    val error: String? = null,
    val pendingDelete: Conversation? = null,
    /** The conversation whose mute-duration picker is open (TDD §6.18); null when closed. */
    val mutePickerFor: Conversation? = null,
)

/** Mute durations offered by the picker (TDD §6.18). ALWAYS maps to an open-ended mute. */
enum class MuteDuration(val label: String) {
    EIGHT_HOURS("8 hours"),
    ONE_WEEK("1 week"),
    ALWAYS("Always"),
}

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
                _state.update { it.copy(conversations = sortPinnedFirst(list), isLoading = false) }
            }
        }
        refresh()
    }

    /**
     * Pinned conversations float to the top (TDD §6.22), most-recently-pinned first; the rest keep
     * their recency order. The pin is per-participant, so this is the local user's view.
     */
    private fun sortPinnedFirst(list: List<Conversation>): List<Conversation> =
        list.sortedWith(
            compareByDescending<Conversation> { it.isPinned }
                .thenByDescending {
                    (if (it.isPinned) it.pinnedAt else it.lastMessageAt)?.toEpochMilli() ?: 0L
                },
        )

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

    // ─── Mute (TDD §6.18) ────────────────────────────────────────────────────
    fun requestMute(conversation: Conversation) = _state.update { it.copy(mutePickerFor = conversation) }

    fun cancelMute() = _state.update { it.copy(mutePickerFor = null) }

    fun confirmMute(duration: MuteDuration) {
        val target = _state.value.mutePickerFor ?: return
        _state.update { it.copy(mutePickerFor = null) }
        val until = when (duration) {
            MuteDuration.EIGHT_HOURS -> Instant.now().plus(8, ChronoUnit.HOURS)
            MuteDuration.ONE_WEEK -> Instant.now().plus(7, ChronoUnit.DAYS)
            MuteDuration.ALWAYS -> null
        }
        viewModelScope.launch { conversationRepository.mute(target.id, until) }
    }

    fun unmute(conversation: Conversation) {
        viewModelScope.launch { conversationRepository.unmute(conversation.id) }
    }

    // ─── Pin (TDD §6.22) ─────────────────────────────────────────────────────
    fun pin(conversation: Conversation) {
        viewModelScope.launch { conversationRepository.pin(conversation.id) }
    }

    fun unpin(conversation: Conversation) {
        viewModelScope.launch { conversationRepository.unpin(conversation.id) }
    }

    /** Archive a conversation out of the main list (TDD §6.23). */
    fun archive(conversation: Conversation) {
        viewModelScope.launch { conversationRepository.archive(conversation.id) }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 5_000L
    }
}
