package com.gayathrini.chatapp.data.conversations

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.CreateConversationRequest
import com.gayathrini.chatapp.core.network.safeApiCall
import com.gayathrini.chatapp.domain.model.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val dao: ConversationDao,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) : ConversationRepository {

    @Volatile
    private var syncCursor: String? = null

    override val conversations: Flow<List<Conversation>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refresh(): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.getConversations() }) {
            is AppResult.Success -> {
                dao.replaceAll(result.data.map { it.toEntity() })
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun sync(): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.sync(syncCursor) }) {
            is AppResult.Success -> {
                val response = result.data
                if (response.conversations.isNotEmpty()) {
                    dao.upsertAll(response.conversations.map { it.toEntity() })
                }
                syncCursor = response.serverTime
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun createConversation(peerUserId: String): AppResult<Conversation> =
        when (val result = safeApiCall(dispatchers, json) {
            api.createConversation(CreateConversationRequest(peerUserId))
        }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(result.data.toEntity().toDomain())
            }
            is AppResult.Failure -> result
        }

    override suspend fun deleteConversation(id: String): AppResult<Unit> {
        dao.deleteById(id) // optimistic removal
        return when (val result = safeApiCall(dispatchers, json) { api.deleteConversation(id) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> {
                refresh() // restore on failure
                result
            }
        }
    }
}
