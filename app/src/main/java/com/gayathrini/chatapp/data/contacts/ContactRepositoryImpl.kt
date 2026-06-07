package com.gayathrini.chatapp.data.contacts

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.AddContactRequest
import com.gayathrini.chatapp.core.network.safeApiCall
import com.gayathrini.chatapp.domain.model.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val dao: ContactDao,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) : ContactRepository {

    override val contacts: Flow<List<Contact>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refresh(): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.getContacts() }) {
            is AppResult.Success -> {
                dao.replaceAll(result.data.map { it.toEntity() })
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun addContact(username: String): AppResult<Contact> =
        when (val result = safeApiCall(dispatchers, json) {
            api.addContact(AddContactRequest(username = username))
        }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(result.data.toDomain())
            }
            is AppResult.Failure -> result
        }

    override suspend fun removeContact(contactId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.deleteContact(contactId) }) {
            is AppResult.Success -> {
                dao.deleteById(contactId)
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }
}
