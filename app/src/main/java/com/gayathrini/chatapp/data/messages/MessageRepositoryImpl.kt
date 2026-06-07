package com.gayathrini.chatapp.data.messages

import com.gayathrini.chatapp.core.common.AppError
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.ReadRequest
import com.gayathrini.chatapp.core.network.dto.SendMessageRequest
import com.gayathrini.chatapp.core.network.safeApiCall
import com.gayathrini.chatapp.data.local.SessionStore
import com.gayathrini.chatapp.data.media.MediaPayload
import com.gayathrini.chatapp.data.media.MediaRepository
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val dao: MessageDao,
    private val mediaRepository: MediaRepository,
    private val sessionStore: SessionStore,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) : MessageRepository {

    override fun messages(conversationId: String): Flow<List<Message>> =
        combine(dao.observeForConversation(conversationId), sessionStore.session) { entities, session ->
            entities.map { it.toDomain(session?.userId) }
        }

    override suspend fun loadInitial(conversationId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.getMessages(conversationId, since = null) }) {
            is AppResult.Success -> {
                dao.upsertAll(result.data.messages.map { it.toEntity() })
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun poll(conversationId: String): AppResult<Unit> {
        val cursor = dao.latestSentAt(conversationId)?.let { Instant.ofEpochMilli(it).toString() }
        return when (val result = safeApiCall(dispatchers, json) { api.getMessages(conversationId, since = cursor) }) {
            is AppResult.Success -> {
                if (result.data.messages.isNotEmpty()) {
                    dao.upsertAll(result.data.messages.map { it.toEntity() })
                }
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }
    }

    override suspend fun sendText(conversationId: String, text: String): AppResult<Unit> {
        val clientId = UUID.randomUUID().toString()
        insertOptimistic(clientId, conversationId, type = "TEXT", text = text, mediaUrl = null)
        return finishSend(conversationId, clientId, SendMessageRequest(clientId, "TEXT", text = text))
    }

    override suspend fun sendImage(
        conversationId: String,
        payload: MediaPayload,
        localPreviewUri: String,
    ): AppResult<Unit> {
        val clientId = UUID.randomUUID().toString()
        insertOptimistic(clientId, conversationId, type = "IMAGE", text = null, mediaUrl = localPreviewUri)
        return when (val upload = mediaRepository.upload(payload.bytes, payload.fileName, payload.mimeType)) {
            is AppResult.Failure -> {
                dao.updateStatus(clientId, MessageStatus.FAILED.name)
                upload
            }
            is AppResult.Success -> finishSend(
                conversationId,
                clientId,
                SendMessageRequest(clientId, "IMAGE", mediaId = upload.data.mediaId),
            )
        }
    }

    override suspend fun retry(conversationId: String, clientId: String): AppResult<Unit> {
        val entity = dao.getByClientId(clientId) ?: return AppResult.Failure(AppError.Unknown())
        if (entity.type.equals("IMAGE", ignoreCase = true)) {
            // Image retry needs a re-upload; the user re-attaches the photo instead.
            return AppResult.Failure(AppError.Unknown())
        }
        dao.updateStatus(clientId, MessageStatus.SENDING.name)
        return finishSend(conversationId, clientId, SendMessageRequest(clientId, "TEXT", text = entity.text))
    }

    override suspend fun markRead(conversationId: String): AppResult<Unit> =
        safeApiCall(dispatchers, json) { api.markRead(conversationId, ReadRequest()) }

    private suspend fun insertOptimistic(
        clientId: String,
        conversationId: String,
        type: String,
        text: String?,
        mediaUrl: String?,
    ) {
        val me = sessionStore.currentUserId().orEmpty()
        dao.upsert(
            MessageEntity(
                clientId = clientId,
                id = clientId,
                conversationId = conversationId,
                senderId = me,
                type = type,
                text = text,
                mediaUrl = mediaUrl,
                status = MessageStatus.SENDING.name,
                sentAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun finishSend(
        conversationId: String,
        clientId: String,
        request: SendMessageRequest,
    ): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.sendMessage(conversationId, request) }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> {
                dao.updateStatus(clientId, MessageStatus.FAILED.name)
                result
            }
        }
}
