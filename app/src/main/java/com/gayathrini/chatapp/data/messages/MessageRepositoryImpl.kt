package com.gayathrini.chatapp.data.messages

import com.gayathrini.chatapp.core.common.AppError
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.EditMessageRequest
import com.gayathrini.chatapp.core.network.dto.ReactionRequest
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
            // Hide disappearing messages whose TTL has elapsed locally (TDD §6.25); the server also
            // filters them on read and a TTL index purges them.
            entities.map { it.toDomain(session?.userId) }.filterNot { it.isExpired }
        }

    override suspend fun loadInitial(conversationId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.getMessages(conversationId, since = null) }) {
            is AppResult.Success -> {
                dao.upsertAll(result.data.messages.map { it.toEntity() })
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun poll(conversationId: String): AppResult<List<String>> {
        // Window on the server `updatedAt` high-water mark so receipt transitions (which don't
        // move `sentAt`) are re-fetched and ticks refresh (TDD §6.4).
        val cursor = dao.latestUpdatedAt(conversationId)?.let { Instant.ofEpochMilli(it).toString() }
        return when (val result = safeApiCall(dispatchers, json) { api.getMessages(conversationId, since = cursor) }) {
            is AppResult.Success -> {
                if (result.data.messages.isNotEmpty()) {
                    dao.upsertAll(result.data.messages.map { it.toEntity() })
                }
                AppResult.Success(result.data.typers)
            }
            is AppResult.Failure -> result
        }
    }

    override suspend fun sendTyping(conversationId: String): AppResult<Unit> =
        safeApiCall(dispatchers, json) { api.sendTyping(conversationId) }

    override suspend fun conversationMedia(conversationId: String, type: String): AppResult<List<Message>> {
        val me = sessionStore.currentUserId()
        return when (val result = safeApiCall(dispatchers, json) { api.getConversationMedia(conversationId, type) }) {
            // The gallery is an ephemeral read — map DTOs to domain without touching the cache.
            is AppResult.Success -> AppResult.Success(result.data.messages.map { it.toEntity().toDomain(me) })
            is AppResult.Failure -> result
        }
    }

    override suspend fun react(conversationId: String, messageId: String, emoji: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) {
            api.addReaction(conversationId, messageId, ReactionRequest(emoji))
        }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun unreact(conversationId: String, messageId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) {
            api.removeReaction(conversationId, messageId)
        }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun editMessage(conversationId: String, messageId: String, text: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) {
            api.editMessage(conversationId, messageId, EditMessageRequest(text))
        }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity()) // replace with the edited message
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun star(conversationId: String, messageId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.starMessage(conversationId, messageId) }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity()) // updated message carries starred=true
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun unstar(conversationId: String, messageId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.unstarMessage(conversationId, messageId) }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun starredMessages(): AppResult<List<Message>> {
        val me = sessionStore.currentUserId()
        return when (val result = safeApiCall(dispatchers, json) { api.getStarredMessages() }) {
            // Ephemeral read for the Starred screen — map to domain without touching the cache.
            is AppResult.Success -> AppResult.Success(result.data.map { it.toEntity().toDomain(me) })
            is AppResult.Failure -> result
        }
    }

    override suspend fun deleteForEveryone(conversationId: String, messageId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.deleteForEveryone(conversationId, messageId) }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity()) // replace with the tombstone
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun deleteForMe(conversationId: String, messageId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.hideMessage(conversationId, messageId) }) {
            is AppResult.Success -> {
                dao.deleteById(messageId) // remove locally; the server hides it on re-fetch
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun forwardMessage(targetConversationId: String, original: Message): AppResult<Unit> {
        // Send to the target conversation tagging the origin; the server copies the content (§6.13).
        val request = SendMessageRequest(
            clientId = UUID.randomUUID().toString(),
            type = original.type.name,
            forwardedFromId = original.id,
        )
        return when (val result = safeApiCall(dispatchers, json) { api.sendMessage(targetConversationId, request) }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity()) // belongs to the target thread, not the current one
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }
    }

    override suspend fun loadOlder(conversationId: String): AppResult<Boolean> {
        // Page backward from the oldest cached message (TDD §6.8). Nothing cached yet → no-op.
        val before = dao.oldestSentAt(conversationId)?.let { Instant.ofEpochMilli(it).toString() }
            ?: return AppResult.Success(false)
        return when (
            val result = safeApiCall(dispatchers, json) {
                api.getMessages(conversationId, before = before, limit = HISTORY_PAGE_SIZE)
            }
        ) {
            is AppResult.Success -> {
                if (result.data.messages.isNotEmpty()) {
                    dao.upsertAll(result.data.messages.map { it.toEntity() })
                }
                // The server returns a cursor only when a full page was returned → more remains.
                AppResult.Success(result.data.nextCursor != null)
            }
            is AppResult.Failure -> result
        }
    }

    override suspend fun sendText(conversationId: String, text: String, replyToId: String?): AppResult<Unit> {
        val clientId = UUID.randomUUID().toString()
        insertOptimistic(clientId, conversationId, type = "TEXT", text = text, mediaUrl = null)
        return finishSend(
            conversationId,
            clientId,
            SendMessageRequest(clientId, "TEXT", text = text, replyToId = replyToId),
        )
    }

    override suspend fun sendImage(
        conversationId: String,
        payload: MediaPayload,
        localPreviewUri: String,
        replyToId: String?,
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
                SendMessageRequest(clientId, "IMAGE", mediaId = upload.data.mediaId, replyToId = replyToId),
            )
        }
    }

    override suspend fun sendFile(
        conversationId: String,
        payload: MediaPayload,
        replyToId: String?,
    ): AppResult<Unit> {
        val clientId = UUID.randomUUID().toString()
        insertOptimistic(
            clientId, conversationId, type = "FILE", text = null, mediaUrl = null,
            fileName = payload.fileName, mimeType = payload.mimeType, sizeBytes = payload.bytes.size.toLong(),
        )
        return when (val upload = mediaRepository.upload(payload.bytes, payload.fileName, payload.mimeType)) {
            is AppResult.Failure -> {
                dao.updateStatus(clientId, MessageStatus.FAILED.name)
                upload
            }
            is AppResult.Success -> finishSend(
                conversationId,
                clientId,
                SendMessageRequest(clientId, "FILE", mediaId = upload.data.mediaId, replyToId = replyToId),
            )
        }
    }

    override suspend fun sendSticker(
        conversationId: String,
        stickerId: String,
        replyToId: String?,
    ): AppResult<Unit> {
        val clientId = UUID.randomUUID().toString()
        insertOptimistic(clientId, conversationId, type = "STICKER", text = null, mediaUrl = null, stickerId = stickerId)
        return finishSend(
            conversationId,
            clientId,
            SendMessageRequest(clientId, "STICKER", stickerId = stickerId, replyToId = replyToId),
        )
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
        fileName: String? = null,
        mimeType: String? = null,
        sizeBytes: Long? = null,
        stickerId: String? = null,
    ) {
        val me = sessionStore.currentUserId().orEmpty()
        val now = System.currentTimeMillis()
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
                sentAt = now,
                // Provisional; the server echo (finishSend) overwrites this with the real cursor
                // before the next poll, so client-clock skew never pollutes the change-feed.
                updatedAt = now,
                fileName = fileName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                stickerId = stickerId,
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

    private companion object {
        const val HISTORY_PAGE_SIZE = 30
    }
}
