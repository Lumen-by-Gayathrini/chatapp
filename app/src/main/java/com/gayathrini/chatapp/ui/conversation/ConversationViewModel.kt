package com.gayathrini.chatapp.ui.conversation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.navigation.Screen
import com.gayathrini.chatapp.core.notifications.MessageNotifier
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.data.media.MediaReader
import com.gayathrini.chatapp.data.messages.MessageRepository
import com.gayathrini.chatapp.data.user.UserRepository
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.ui.common.toUserMessage
import java.time.Instant
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
    val peerOnline: Boolean = false,
    val peerLastSeenAt: Instant? = null,
    val isPeerTyping: Boolean = false,
    /** Group context (TDD §6.15): drives sender-name labels in the thread. */
    val isGroup: Boolean = false,
    val senderNames: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val messages: List<Message> = emptyList(),
    /** Disappearing-messages TTL in seconds (TDD §6.25); null = off. */
    val disappearingTtlSeconds: Int? = null,
    val draft: String = "",
    /** The message being replied to (TDD §6.12); shows a quote banner above the composer. */
    val replyingTo: Message? = null,
    /** The message being edited (TDD §6.21); the composer shows an "Editing" banner and saves. */
    val editing: Message? = null,
    /** The message being forwarded (TDD §6.13); shows a target-conversation picker. */
    val forwarding: Message? = null,
    val forwardTargets: List<Conversation> = emptyList(),
    /** The message pending deletion (TDD §6.14); shows a scope chooser. */
    val pendingMessageDelete: Message? = null,
    val isUploading: Boolean = false,
    val error: String? = null,
    val pendingDelete: Boolean = false,
    /** Backward pagination (TDD §6.8): older history may exist; a page load is in flight. */
    val hasMoreHistory: Boolean = true,
    val isLoadingMore: Boolean = false,
)

sealed interface ConversationEffect {
    data object NavigateBack : ConversationEffect
}

@HiltViewModel
class ConversationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val notifier: MessageNotifier,
    private val mediaReader: MediaReader,
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle[Screen.Conversation.ARG])
    private var peerId: String? = null
    private var lastTypingPingAt = 0L

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
                    peerId = conversation.peer?.id // null for groups → no presence/typing line
                    _state.update {
                        it.copy(
                            peerName = conversation.displayName,
                            peerOnline = conversation.peer?.online ?: false,
                            peerLastSeenAt = conversation.peer?.lastSeenAt,
                            isGroup = conversation.isGroup,
                            senderNames = conversation.participants.associate { u -> u.id to u.displayName },
                            disappearingTtlSeconds = conversation.disappearingTtlSeconds,
                        )
                    }
                }
                // Other conversations are forward targets (TDD §6.13).
                _state.update { it.copy(forwardTargets = conversations.filter { c -> c.id != conversationId }) }
            }
        }
        viewModelScope.launch { messageRepository.loadInitial(conversationId) }
        viewModelScope.launch { messageRepository.markRead(conversationId) }
        viewModelScope.launch { refreshPeerPresence() }
    }

    fun startPolling() {
        // Viewing this thread → suppress its notifications and clear any already posted (TDD §6.6).
        notifier.setActiveConversation(conversationId)
        notifier.cancel(conversationId)
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                when (val result = messageRepository.poll(conversationId)) {
                    is AppResult.Success -> _state.update {
                        it.copy(isPeerTyping = peerId != null && peerId in result.data)
                    }
                    is AppResult.Failure -> Unit
                }
                refreshPeerPresence()
            }
        }
    }

    /** Refresh the peer's live presence for the top bar (TDD §6.5); best-effort. */
    private suspend fun refreshPeerPresence() {
        val id = peerId ?: return
        when (val result = userRepository.getProfile(id)) {
            is AppResult.Success -> _state.update {
                it.copy(peerOnline = result.data.online, peerLastSeenAt = result.data.lastSeenAt)
            }
            is AppResult.Failure -> Unit // keep the last-known presence
        }
    }

    fun stopPolling() {
        notifier.setActiveConversation(null)
        pollingJob?.cancel()
        pollingJob = null
    }

    /** Load the next older page of history (TDD §6.8); called when the list scrolls to the top. */
    fun loadOlder() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMoreHistory || current.messages.isEmpty()) return
        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            when (val result = messageRepository.loadOlder(conversationId)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoadingMore = false, hasMoreHistory = result.data)
                }
                is AppResult.Failure -> _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun onDraftChange(value: String) {
        _state.update { it.copy(draft = value) }
        // Throttled typing heartbeat (TDD §6.10): at most once per interval while there's text.
        if (value.isNotBlank()) {
            val now = System.currentTimeMillis()
            if (now - lastTypingPingAt >= TYPING_PING_INTERVAL_MS) {
                lastTypingPingAt = now
                viewModelScope.launch { messageRepository.sendTyping(conversationId) }
            }
        }
    }

    /** Enter reply mode for [message] (TDD §6.12); the composer shows a quote banner. */
    fun onReplyTo(message: Message) = _state.update { it.copy(replyingTo = message) }

    fun cancelReply() = _state.update { it.copy(replyingTo = null) }

    /** Begin forwarding [message] (TDD §6.13); shows the target-conversation picker. */
    fun onForward(message: Message) = _state.update { it.copy(forwarding = message) }

    fun cancelForward() = _state.update { it.copy(forwarding = null) }

    /** Forward the pending message into [target], then close the picker. */
    fun onForwardTargetPicked(target: Conversation) {
        val message = _state.value.forwarding ?: return
        _state.update { it.copy(forwarding = null) }
        viewModelScope.launch { messageRepository.forwardMessage(target.id, message) }
    }

    /** Enter edit mode for [message] (TDD §6.21); prefills the composer with its text. */
    fun onEditRequest(message: Message) =
        _state.update { it.copy(editing = message, draft = message.text.orEmpty(), replyingTo = null) }

    fun cancelEdit() = _state.update { it.copy(editing = null, draft = "") }

    /** Open the delete-scope chooser for [message] (TDD §6.14). */
    fun onDeleteRequest(message: Message) = _state.update { it.copy(pendingMessageDelete = message) }

    fun cancelMessageDelete() = _state.update { it.copy(pendingMessageDelete = null) }

    fun deleteForMe() {
        val m = _state.value.pendingMessageDelete ?: return
        _state.update { it.copy(pendingMessageDelete = null) }
        viewModelScope.launch { messageRepository.deleteForMe(conversationId, m.id) }
    }

    fun deleteForEveryone() {
        val m = _state.value.pendingMessageDelete ?: return
        _state.update { it.copy(pendingMessageDelete = null) }
        viewModelScope.launch { messageRepository.deleteForEveryone(conversationId, m.id) }
    }

    /** Consume the pending reply target's id, clearing reply mode. */
    private fun consumeReplyToId(): String? {
        val id = _state.value.replyingTo?.id
        if (id != null) _state.update { it.copy(replyingTo = null) }
        return id
    }

    fun sendText() {
        val text = _state.value.draft.trim()
        if (text.isEmpty()) return
        // Saving an edit (TDD §6.21) takes precedence over sending a new message.
        val editing = _state.value.editing
        if (editing != null) {
            _state.update { it.copy(draft = "", editing = null) }
            viewModelScope.launch { messageRepository.editMessage(conversationId, editing.id, text) }
            return
        }
        val replyToId = consumeReplyToId()
        _state.update { it.copy(draft = "") }
        viewModelScope.launch { messageRepository.sendText(conversationId, text, replyToId) }
    }

    /** Send a bundled sticker (TDD §6.20). */
    fun sendSticker(stickerId: String) {
        val replyToId = consumeReplyToId()
        viewModelScope.launch { messageRepository.sendSticker(conversationId, stickerId, replyToId) }
    }

    fun onPhotoPicked(uri: Uri) {
        val replyToId = consumeReplyToId()
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, error = null) }
            val payload = mediaReader.read(uri)
            if (payload == null) {
                _state.update { it.copy(isUploading = false, error = "We couldn't read that photo. Please try another.") }
                return@launch
            }
            when (val result = messageRepository.sendImage(conversationId, payload, uri.toString(), replyToId)) {
                is AppResult.Success -> _state.update { it.copy(isUploading = false) }
                is AppResult.Failure -> _state.update {
                    it.copy(isUploading = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    fun onFilePicked(uri: Uri) {
        val replyToId = consumeReplyToId()
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, error = null) }
            val payload = mediaReader.read(uri)
            if (payload == null) {
                _state.update { it.copy(isUploading = false, error = "We couldn't read that file. Please try another.") }
                return@launch
            }
            when (val result = messageRepository.sendFile(conversationId, payload, replyToId)) {
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

    /** Set/clear disappearing-messages TTL on this conversation (TDD §6.25); null turns it off. */
    fun setDisappearing(ttlSeconds: Int?) {
        viewModelScope.launch { conversationRepository.setDisappearing(conversationId, ttlSeconds) }
    }

    /** Toggle the star on a message (TDD §6.24): starred → unstar, otherwise star. */
    fun onStar(message: Message) {
        viewModelScope.launch {
            if (message.starred) {
                messageRepository.unstar(conversationId, message.id)
            } else {
                messageRepository.star(conversationId, message.id)
            }
        }
    }

    /** Toggle an emoji reaction on a message (TDD §6.11): same emoji removes, otherwise sets it. */
    fun onReact(message: Message, emoji: String) {
        viewModelScope.launch {
            if (message.myReactionEmoji == emoji) {
                messageRepository.unreact(conversationId, message.id)
            } else {
                messageRepository.react(conversationId, message.id, emoji)
            }
        }
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
        const val TYPING_PING_INTERVAL_MS = 3_000L
    }
}
