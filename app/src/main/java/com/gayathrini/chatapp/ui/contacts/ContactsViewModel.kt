package com.gayathrini.chatapp.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.contacts.ContactRepository
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

data class ContactsUiState(
    val query: String = "",
    val isLoading: Boolean = true,
    val contacts: List<Contact> = emptyList(),
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val addUsername: String = "",
    val addError: String? = null,
    val isAdding: Boolean = false,
    val pendingRemoval: Contact? = null,
)

sealed interface ContactsEffect {
    data class OpenConversation(val peerUserId: String) : ContactsEffect
}

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repository: ContactRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ContactsUiState())
    val state: StateFlow<ContactsUiState> = _state.asStateFlow()

    private val _effects = Channel<ContactsEffect>(Channel.BUFFERED)
    val effects: Flow<ContactsEffect> = _effects.receiveAsFlow()

    private var allContacts: List<Contact> = emptyList()

    init {
        viewModelScope.launch {
            repository.contacts.collect { list ->
                allContacts = list
                _state.update { it.copy(contacts = filter(list, it.query), isLoading = false) }
            }
        }
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(error = null) }
        viewModelScope.launch {
            when (val result = repository.refresh()) {
                is AppResult.Success -> Unit // cache flow emits the new list
                is AppResult.Failure -> _state.update {
                    it.copy(error = result.error.toUserMessage(), isLoading = false)
                }
            }
        }
    }

    fun onQueryChange(value: String) =
        _state.update { it.copy(query = value, contacts = filter(allContacts, value)) }

    fun onContactClick(contact: Contact) {
        viewModelScope.launch { _effects.send(ContactsEffect.OpenConversation(contact.user.id)) }
    }

    fun requestRemove(contact: Contact) = _state.update { it.copy(pendingRemoval = contact) }

    fun cancelRemove() = _state.update { it.copy(pendingRemoval = null) }

    fun confirmRemove() {
        val target = _state.value.pendingRemoval ?: return
        _state.update { it.copy(pendingRemoval = null) }
        viewModelScope.launch { repository.removeContact(target.id) }
    }

    fun openAddDialog() = _state.update { it.copy(showAddDialog = true, addUsername = "", addError = null) }

    fun dismissAddDialog() = _state.update { it.copy(showAddDialog = false) }

    fun onAddUsernameChange(value: String) =
        _state.update { it.copy(addUsername = value, addError = null) }

    fun submitAdd() {
        val username = _state.value.addUsername.trim()
        if (username.isEmpty()) {
            _state.update { it.copy(addError = "Please enter a username.") }
            return
        }
        _state.update { it.copy(isAdding = true) }
        viewModelScope.launch {
            when (val result = repository.addContact(username)) {
                is AppResult.Success -> _state.update {
                    it.copy(isAdding = false, showAddDialog = false, addUsername = "")
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isAdding = false, addError = result.error.toUserMessage())
                }
            }
        }
    }

    private fun filter(list: List<Contact>, query: String): List<Contact> {
        if (query.isBlank()) return list
        return list.filter {
            it.displayName.contains(query, ignoreCase = true) ||
                it.user.username.contains(query, ignoreCase = true)
        }
    }
}
