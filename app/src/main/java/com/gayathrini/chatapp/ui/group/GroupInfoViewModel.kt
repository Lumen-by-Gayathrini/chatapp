package com.gayathrini.chatapp.ui.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.navigation.Screen
import com.gayathrini.chatapp.data.contacts.ContactRepository
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.data.local.SessionStore
import com.gayathrini.chatapp.domain.model.Contact
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

data class GroupInfoUiState(
    val conversation: Conversation? = null,
    val currentUserId: String? = null,
    val isAdmin: Boolean = false,
    val titleDraft: String = "",
    val addableContacts: List<Contact> = emptyList(),
    val showAddMember: Boolean = false,
    val error: String? = null,
)

sealed interface GroupInfoEffect {
    data object Left : GroupInfoEffect
}

/** View/manage a group (TDD §6.15): members, admin actions, title, leave. */
@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val conversationRepository: ConversationRepository,
    private val contactRepository: ContactRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle[Screen.GroupInfo.ARG])

    private val _state = MutableStateFlow(GroupInfoUiState())
    val state: StateFlow<GroupInfoUiState> = _state.asStateFlow()

    private val _effects = Channel<GroupInfoEffect>(Channel.BUFFERED)
    val effects: Flow<GroupInfoEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val me = sessionStore.currentUserId()
            _state.update { it.copy(currentUserId = me) }

            launch {
                conversationRepository.conversations.collect { list ->
                    val conv = list.firstOrNull { it.id == conversationId }
                    _state.update { s ->
                        s.copy(
                            conversation = conv,
                            isAdmin = conv?.isAdmin(me) == true,
                            titleDraft = if (s.titleDraft.isBlank()) conv?.title.orEmpty() else s.titleDraft,
                        )
                    }
                    recomputeAddable()
                }
            }
            launch {
                contactRepository.contacts.collect { recomputeAddable(it) }
            }
            contactRepository.refresh()
        }
    }

    private var lastContacts: List<Contact> = emptyList()

    private fun recomputeAddable(contacts: List<Contact> = lastContacts) {
        lastContacts = contacts
        val memberIds = _state.value.conversation?.participants?.map { it.id }?.toSet() ?: emptySet()
        _state.update { it.copy(addableContacts = contacts.filter { c -> c.user.id !in memberIds }) }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(titleDraft = value) }

    fun saveTitle() {
        val title = _state.value.titleDraft.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            when (val r = conversationRepository.updateGroup(conversationId, title = title, description = null)) {
                is AppResult.Success -> Unit // cache flow refreshes the title
                is AppResult.Failure -> _state.update { it.copy(error = r.error.toUserMessage()) }
            }
        }
    }

    fun openAddMember() = _state.update { it.copy(showAddMember = true) }
    fun dismissAddMember() = _state.update { it.copy(showAddMember = false) }

    fun addMember(userId: String) {
        _state.update { it.copy(showAddMember = false) }
        viewModelScope.launch {
            when (val r = conversationRepository.addMember(conversationId, userId)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> _state.update { it.copy(error = r.error.toUserMessage()) }
            }
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            when (val r = conversationRepository.removeMember(conversationId, userId)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> _state.update { it.copy(error = r.error.toUserMessage()) }
            }
        }
    }

    fun leave() {
        val me = _state.value.currentUserId ?: return
        viewModelScope.launch {
            when (conversationRepository.removeMember(conversationId, me)) {
                is AppResult.Success -> _effects.send(GroupInfoEffect.Left)
                is AppResult.Failure -> _state.update { it.copy(error = "Couldn't leave the group.") }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}
