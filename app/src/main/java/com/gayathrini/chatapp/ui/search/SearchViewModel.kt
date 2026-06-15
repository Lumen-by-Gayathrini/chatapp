package com.gayathrini.chatapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.data.search.SearchRepository
import com.gayathrini.chatapp.data.search.SearchResults
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.domain.model.User
import com.gayathrini.chatapp.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val results: SearchResults = SearchResults(),
    val error: String? = null,
)

sealed interface SearchEffect {
    data class OpenConversation(val conversationId: String) : SearchEffect
}

/** Global search across messages, users, and groups (TDD §6.17). */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val _effects = Channel<SearchEffect>(Channel.BUFFERED)
    val effects: Flow<SearchEffect> = _effects.receiveAsFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { runSearch(it) }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        queryFlow.value = value
    }

    private suspend fun runSearch(query: String) {
        val q = query.trim()
        if (q.isBlank()) {
            _state.update { it.copy(results = SearchResults(), isSearching = false, error = null, hasSearched = false) }
            return
        }
        _state.update { it.copy(isSearching = true, error = null) }
        when (val result = searchRepository.search(q)) {
            is AppResult.Success -> _state.update {
                it.copy(isSearching = false, hasSearched = true, results = result.data)
            }
            is AppResult.Failure -> _state.update {
                it.copy(isSearching = false, hasSearched = true, error = result.error.toUserMessage())
            }
        }
    }

    fun onMessageClick(message: Message) {
        viewModelScope.launch { _effects.send(SearchEffect.OpenConversation(message.conversationId)) }
    }

    fun onGroupClick(conversation: Conversation) {
        viewModelScope.launch { _effects.send(SearchEffect.OpenConversation(conversation.id)) }
    }

    /** Tapping a user result opens (creating if needed) the direct conversation with them. */
    fun onUserClick(user: User) {
        viewModelScope.launch {
            when (val result = conversationRepository.createConversation(user.id)) {
                is AppResult.Success -> _effects.send(SearchEffect.OpenConversation(result.data.id))
                is AppResult.Failure -> _state.update { it.copy(error = result.error.toUserMessage()) }
            }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
    }
}
