package com.gayathrini.chatapp.data.contacts

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.domain.model.Contact
import kotlinx.coroutines.flow.Flow

/** Contacts, cached in Room with the network as the source of truth on refresh (TDD §5.2, §6.2). */
interface ContactRepository {
    /** Reactive cache stream; renders instantly, updated by [refresh] / mutations. */
    val contacts: Flow<List<Contact>>

    suspend fun refresh(): AppResult<Unit>

    suspend fun addContact(username: String): AppResult<Contact>

    suspend fun removeContact(contactId: String): AppResult<Unit>
}
