package com.gayathrini.chatapp.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.contacts.ContactRepository
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.domain.model.Contact
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

data class CreateGroupUiState(
    val title: String = "",
    val contacts: List<Contact> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isCreating: Boolean = false,
    val error: String? = null,
) {
    val canCreate: Boolean get() = title.isNotBlank() && selectedIds.isNotEmpty() && !isCreating
}

sealed interface CreateGroupEffect {
    data class Created(val conversationId: String) : CreateGroupEffect
}

/** Create a group (TDD §6.15): pick members from contacts + a title. */
@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateGroupUiState())
    val state: StateFlow<CreateGroupUiState> = _state.asStateFlow()

    private val _effects = Channel<CreateGroupEffect>(Channel.BUFFERED)
    val effects: Flow<CreateGroupEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            contactRepository.contacts.collect { list -> _state.update { it.copy(contacts = list) } }
        }
        viewModelScope.launch { contactRepository.refresh() }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value) }

    fun toggleMember(userId: String) = _state.update {
        val next = if (userId in it.selectedIds) it.selectedIds - userId else it.selectedIds + userId
        it.copy(selectedIds = next)
    }

    fun create() {
        val current = _state.value
        if (!current.canCreate) return
        _state.update { it.copy(isCreating = true, error = null) }
        viewModelScope.launch {
            when (val result = conversationRepository.createGroup(current.title.trim(), current.selectedIds.toList())) {
                is AppResult.Success -> _effects.send(CreateGroupEffect.Created(result.data.id))
                is AppResult.Failure -> _state.update {
                    it.copy(isCreating = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}
