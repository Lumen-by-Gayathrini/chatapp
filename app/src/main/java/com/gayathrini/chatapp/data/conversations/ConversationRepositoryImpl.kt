package com.gayathrini.chatapp.data.conversations

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.AddMemberRequest
import com.gayathrini.chatapp.core.network.dto.ConversationDto
import com.gayathrini.chatapp.core.network.dto.CreateConversationRequest
import com.gayathrini.chatapp.core.network.dto.CreateGroupRequest
import com.gayathrini.chatapp.core.network.dto.MuteRequest
import com.gayathrini.chatapp.core.network.dto.SetDisappearingRequest
import com.gayathrini.chatapp.core.network.dto.UpdateGroupRequest
import com.gayathrini.chatapp.core.network.safeApiCall
import com.gayathrini.chatapp.core.notifications.MessageNotifier
import com.gayathrini.chatapp.data.local.SessionStore
import com.gayathrini.chatapp.domain.model.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val dao: ConversationDao,
    private val sessionStore: SessionStore,
    private val notifier: MessageNotifier,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) : ConversationRepository {

    @Volatile
    private var syncCursor: String? = null

    override val conversations: Flow<List<Conversation>> =
        // The main list excludes archived threads (TDD §6.23); they live in the Archived screen.
        dao.observeAll().map { entities -> entities.map { it.toDomain() }.filter { !it.isArchived } }

    override suspend fun refresh(): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.getConversations() }) {
            is AppResult.Success -> {
                dao.replaceAll(result.data.map { it.toEntity() })
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun sync(): AppResult<Unit> {
        // The first sync (no cursor) returns every conversation; seed the notifier without
        // alerting so pre-existing unread threads don't spam notifications on app open (TDD §6.6).
        val isInitialSync = syncCursor == null
        return when (val result = safeApiCall(dispatchers, json) { api.sync(syncCursor) }) {
            is AppResult.Success -> {
                val response = result.data
                if (response.conversations.isNotEmpty()) {
                    dao.upsertAll(response.conversations.map { it.toEntity() })
                    notifyIncoming(response.conversations, alert = !isInitialSync)
                }
                syncCursor = response.serverTime
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }
    }

    /**
     * Drive notifications from the synced conversation rows (TDD §6.6): a thread with unread
     * messages whose latest message is from the peer earns a notification; a now-read thread has
     * its notification cancelled. `unreadCount` doubles as the badge number and the read filter.
     */
    private suspend fun notifyIncoming(conversations: List<ConversationDto>, alert: Boolean) {
        val me = sessionStore.currentUserId() ?: return
        for (c in conversations) {
            val last = c.lastMessage
            if (c.unreadCount > 0 && last != null && last.senderId != me) {
                if (isMuted(c.mutedUntil)) continue // muted → suppress the notification (TDD §6.18)
                val body = when {
                    !last.text.isNullOrBlank() -> last.text!!
                    last.type.equals("IMAGE", ignoreCase = true) -> "📷 Photo"
                    else -> "New message"
                }
                notifier.notifyMessage(
                    conversationId = c.id,
                    title = c.peer?.displayName ?: c.title ?: "Group",
                    body = body,
                    unreadCount = c.unreadCount,
                    messageKey = c.lastMessageAt ?: last.sentAt,
                    alert = alert,
                )
            } else if (c.unreadCount == 0) {
                notifier.cancel(c.id)
            }
        }
    }

    /** Muted if the ISO expiry is in the future (TDD §6.18). */
    private fun isMuted(mutedUntil: String?): Boolean =
        mutedUntil?.let { runCatching { Instant.parse(it).isAfter(Instant.now()) }.getOrDefault(false) }
            ?: false

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

    override suspend fun createGroup(title: String, memberIds: List<String>): AppResult<Conversation> =
        when (val result = safeApiCall(dispatchers, json) {
            api.createGroup(CreateGroupRequest(title, memberIds))
        }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(result.data.toEntity().toDomain())
            }
            is AppResult.Failure -> result
        }

    override suspend fun addMember(conversationId: String, userId: String): AppResult<Conversation> =
        when (val result = safeApiCall(dispatchers, json) {
            api.addMember(conversationId, AddMemberRequest(userId))
        }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(result.data.toEntity().toDomain())
            }
            is AppResult.Failure -> result
        }

    override suspend fun removeMember(conversationId: String, userId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.removeMember(conversationId, userId) }) {
            is AppResult.Success -> {
                // If the current user left, drop the conversation locally.
                if (userId == sessionStore.currentUserId()) dao.deleteById(conversationId) else refresh()
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun updateGroup(
        conversationId: String,
        title: String?,
        description: String?,
    ): AppResult<Conversation> =
        when (val result = safeApiCall(dispatchers, json) {
            api.updateGroup(conversationId, UpdateGroupRequest(title = title, description = description))
        }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(result.data.toEntity().toDomain())
            }
            is AppResult.Failure -> result
        }

    override suspend fun mute(conversationId: String, until: Instant?): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) {
            api.muteConversation(conversationId, MuteRequest(until?.toString()))
        }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun unmute(conversationId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.unmuteConversation(conversationId) }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun pin(conversationId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.pinConversation(conversationId) }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun unpin(conversationId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.unpinConversation(conversationId) }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun archive(conversationId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.archiveConversation(conversationId) }) {
            is AppResult.Success -> {
                // Drop it from the main cache so it leaves the main list immediately (TDD §6.23).
                dao.deleteById(conversationId)
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun unarchive(conversationId: String): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) { api.unarchiveConversation(conversationId) }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity()) // back into the main list
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }

    override suspend fun archivedConversations(): AppResult<List<Conversation>> =
        when (val result = safeApiCall(dispatchers, json) { api.getConversations(archived = true) }) {
            // Ephemeral read for the Archived screen — map to domain without touching the main cache.
            is AppResult.Success -> AppResult.Success(result.data.map { it.toEntity().toDomain() })
            is AppResult.Failure -> result
        }

    override suspend fun setDisappearing(conversationId: String, ttlSeconds: Int?): AppResult<Unit> =
        when (val result = safeApiCall(dispatchers, json) {
            api.setDisappearing(conversationId, SetDisappearingRequest(ttlSeconds))
        }) {
            is AppResult.Success -> {
                dao.upsert(result.data.toEntity())
                AppResult.Success(Unit)
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
