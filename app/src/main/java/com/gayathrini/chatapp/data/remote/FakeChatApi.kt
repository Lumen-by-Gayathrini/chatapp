package com.gayathrini.chatapp.data.remote

import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.AddContactRequest
import com.gayathrini.chatapp.core.network.dto.AuthResponse
import com.gayathrini.chatapp.core.network.dto.AddMemberRequest
import com.gayathrini.chatapp.core.network.dto.ContactDto
import com.gayathrini.chatapp.core.network.dto.ConversationDto
import com.gayathrini.chatapp.core.network.dto.CreateConversationRequest
import com.gayathrini.chatapp.core.network.dto.CreateGroupRequest
import com.gayathrini.chatapp.core.network.dto.EditMessageRequest
import com.gayathrini.chatapp.core.network.dto.UpdateGroupRequest
import com.gayathrini.chatapp.core.network.dto.LastMessageDto
import com.gayathrini.chatapp.core.network.dto.LoginRequest
import com.gayathrini.chatapp.core.network.dto.LogoutRequest
import com.gayathrini.chatapp.core.network.dto.MediaUploadResponse
import com.gayathrini.chatapp.core.network.dto.MessageDto
import com.gayathrini.chatapp.core.network.dto.MessagesPageDto
import com.gayathrini.chatapp.core.network.dto.MuteRequest
import com.gayathrini.chatapp.core.network.dto.ReactionDto
import com.gayathrini.chatapp.core.network.dto.ReactionRequest
import com.gayathrini.chatapp.core.network.dto.ReadRequest
import com.gayathrini.chatapp.core.network.dto.RefreshRequest
import com.gayathrini.chatapp.core.network.dto.RegisterRequest
import com.gayathrini.chatapp.core.network.dto.SetDisappearingRequest
import com.gayathrini.chatapp.core.network.dto.SendMessageRequest
import com.gayathrini.chatapp.core.network.dto.SyncResponse
import com.gayathrini.chatapp.core.network.dto.TokenResponse
import com.gayathrini.chatapp.core.network.dto.UpdateContactRequest
import com.gayathrini.chatapp.core.network.dto.UpdateProfileRequest
import com.gayathrini.chatapp.core.network.dto.UserDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MultipartBody
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory implementation of [ChatApi] used for fake-API-first development (plan §8). Seeded with
 * a current user, a couple of contacts and a conversation so every screen has data before the real
 * backend exists. Behaviour is intentionally simple; feature slices refine it as needed.
 */
@Singleton
class FakeChatApi @Inject constructor() : ChatApi {

    private val mutex = Mutex()
    private val seq = AtomicLong(1000)

    private val me = UserDto("u_me", "mary", "Mary", null, "ACTIVE")
    private val john = UserDto("u_john", "john", "John", null, "ACTIVE")
    private val david = UserDto("u_david", "david", "David", null, "ACTIVE")
    private val emma = UserDto("u_emma", "emma", "Emma", null, "ACTIVE")
    private val directory = mutableListOf(me, john, david, emma)

    private val contacts = mutableListOf(
        ContactDto("c_john", john, null),
        ContactDto("c_david", david, null),
    )

    /** Ids the current user has blocked (TDD §6.19). */
    private val blockedIds = mutableSetOf<String>()

    private val messages = mutableListOf(
        MessageDto("m_1", "seed-1", "conv_john", john.id, "TEXT", "Hello Mary", null, "READ", isoAgo(3600)),
        MessageDto("m_2", "seed-2", "conv_john", me.id, "TEXT", "Hi John!", null, "READ", isoAgo(3500)),
    )

    private val conversations = mutableListOf(
        ConversationDto(
            id = "conv_john",
            peer = john,
            lastMessage = LastMessageDto("Hi John!", "TEXT", me.id, isoAgo(3500)),
            lastMessageAt = isoAgo(3500),
            unreadCount = 0,
        ),
    )

    private fun newId(prefix: String) = "${prefix}_${seq.incrementAndGet()}"
    private fun nowIso() = Instant.now().toString()

    // --- Auth & profile ---

    override suspend fun register(body: RegisterRequest): AuthResponse = mutex.withLock {
        AuthResponse(me.copy(displayName = body.displayName, username = body.username), token(), token())
    }

    override suspend fun login(body: LoginRequest): AuthResponse = mutex.withLock {
        AuthResponse(me, token(), token())
    }

    override suspend fun refresh(body: RefreshRequest): TokenResponse = TokenResponse(token(), token())

    override suspend fun logout(body: LogoutRequest) = Unit

    override suspend fun getMe(): UserDto = me

    override suspend fun updateMe(body: UpdateProfileRequest): UserDto = mutex.withLock {
        me.copy(
            displayName = body.displayName ?: me.displayName,
            avatarUrl = body.avatarUrl ?: me.avatarUrl,
            about = body.about ?: me.about,
        )
    }

    override suspend fun getUserProfile(id: String): UserDto = mutex.withLock {
        val user = directory.firstOrNull { it.id == id }
            ?: contacts.firstOrNull { it.user.id == id }?.user
            ?: UserDto(id, "user", "User", null, "ACTIVE")
        user.copy(blocked = id in blockedIds, online = if (id in blockedIds) false else user.online)
    }

    override suspend fun pingPresence() = Unit

    override suspend fun blockUser(id: String) {
        mutex.withLock { blockedIds.add(id) }
    }

    override suspend fun unblockUser(id: String) {
        mutex.withLock { blockedIds.remove(id) }
    }

    override suspend fun getBlockedUsers(): List<UserDto> = mutex.withLock {
        blockedIds.mapNotNull { id -> directory.firstOrNull { it.id == id } }
    }

    // --- Contacts ---

    override suspend fun getContacts(): List<ContactDto> = mutex.withLock { contacts.toList() }

    override suspend fun addContact(body: AddContactRequest): ContactDto = mutex.withLock {
        val user = directory.firstOrNull { it.username == body.username || it.id == body.userId }
            ?: UserDto(newId("u"), body.username ?: "user", body.username ?: "User", null, "ACTIVE")
        val contact = ContactDto(newId("c"), user, null)
        contacts.add(contact)
        contact
    }

    override suspend fun updateContact(id: String, body: UpdateContactRequest): ContactDto = mutex.withLock {
        val index = contacts.indexOfFirst { it.id == id }
        val updated = contacts[index].copy(alias = body.alias)
        contacts[index] = updated
        updated
    }

    override suspend fun deleteContact(id: String) {
        mutex.withLock { contacts.removeAll { it.id == id } }
    }

    // --- Conversations ---

    override suspend fun getConversations(archived: Boolean?): List<ConversationDto> = mutex.withLock {
        val wantArchived = archived == true
        conversations
            .filter { (it.archivedAt != null) == wantArchived }
            .sortedByDescending { it.lastMessageAt }
    }

    override suspend fun createConversation(body: CreateConversationRequest): ConversationDto = mutex.withLock {
        conversations.firstOrNull { it.peer?.id == body.peerUserId }?.let { return@withLock it }
        val peer = directory.firstOrNull { it.id == body.peerUserId } ?: me
        val conversation = ConversationDto(newId("conv"), peer, null, nowIso(), 0)
        conversations.add(conversation)
        conversation
    }

    override suspend fun deleteConversation(id: String) {
        mutex.withLock {
            conversations.removeAll { it.id == id }
            messages.removeAll { it.conversationId == id }
        }
    }

    override suspend fun createGroup(body: CreateGroupRequest): ConversationDto = mutex.withLock {
        val members = body.memberIds.mapNotNull { id -> directory.firstOrNull { it.id == id } }
        val parts = (listOf(me) + members).distinctBy { it.id }
        val conversation = ConversationDto(
            id = newId("conv"),
            peer = null,
            lastMessage = null,
            lastMessageAt = nowIso(),
            unreadCount = 0,
            type = "GROUP",
            title = body.title,
            participants = parts,
            admins = listOf(me.id),
            createdBy = me.id,
        )
        conversations.add(conversation)
        conversation
    }

    override suspend fun addMember(id: String, body: AddMemberRequest): ConversationDto = mutex.withLock {
        val index = conversations.indexOfFirst { it.id == id }
        if (index < 0) return@withLock conversations.first()
        val user = directory.firstOrNull { it.id == body.userId }
        val conv = conversations[index]
        val updated = conv.copy(participants = (conv.participants + listOfNotNull(user)).distinctBy { it.id })
        conversations[index] = updated
        updated
    }

    override suspend fun removeMember(id: String, userId: String) {
        mutex.withLock {
            val index = conversations.indexOfFirst { it.id == id }
            if (index >= 0) {
                val conv = conversations[index]
                conversations[index] = conv.copy(
                    participants = conv.participants.filterNot { it.id == userId },
                    admins = conv.admins.filterNot { it == userId },
                )
            }
        }
    }

    override suspend fun updateGroup(id: String, body: UpdateGroupRequest): ConversationDto = mutex.withLock {
        val index = conversations.indexOfFirst { it.id == id }
        if (index < 0) return@withLock conversations.first()
        val conv = conversations[index]
        val updated = conv.copy(
            title = body.title ?: conv.title,
            iconUrl = body.iconUrl ?: conv.iconUrl,
            description = body.description ?: conv.description,
        )
        conversations[index] = updated
        updated
    }

    override suspend fun setDisappearing(id: String, body: SetDisappearingRequest): ConversationDto =
        mutex.withLock {
            val index = conversations.indexOfFirst { it.id == id }
            if (index < 0) return@withLock conversations.first()
            val updated = conversations[index].copy(disappearingTtlSeconds = body.disappearingTtlSeconds)
            conversations[index] = updated
            updated
        }

    override suspend fun markRead(id: String, body: ReadRequest) {
        mutex.withLock {
            val index = conversations.indexOfFirst { it.id == id }
            if (index >= 0) conversations[index] = conversations[index].copy(unreadCount = 0)
        }
    }

    override suspend fun muteConversation(id: String, body: MuteRequest): ConversationDto = mutex.withLock {
        val index = conversations.indexOfFirst { it.id == id }
        if (index < 0) return@withLock conversations.first()
        // null `until` = mute "Always" → far-future sentinel, matching the server.
        val until = body.until ?: "9999-12-31T23:59:59.000Z"
        val updated = conversations[index].copy(mutedUntil = until)
        conversations[index] = updated
        updated
    }

    override suspend fun unmuteConversation(id: String): ConversationDto = mutex.withLock {
        val index = conversations.indexOfFirst { it.id == id }
        if (index < 0) return@withLock conversations.first()
        val updated = conversations[index].copy(mutedUntil = null)
        conversations[index] = updated
        updated
    }

    override suspend fun pinConversation(id: String): ConversationDto = mutex.withLock {
        val index = conversations.indexOfFirst { it.id == id }
        if (index < 0) return@withLock conversations.first()
        val updated = conversations[index].copy(pinnedAt = nowIso())
        conversations[index] = updated
        updated
    }

    override suspend fun unpinConversation(id: String): ConversationDto = mutex.withLock {
        val index = conversations.indexOfFirst { it.id == id }
        if (index < 0) return@withLock conversations.first()
        val updated = conversations[index].copy(pinnedAt = null)
        conversations[index] = updated
        updated
    }

    override suspend fun archiveConversation(id: String): ConversationDto = mutex.withLock {
        val index = conversations.indexOfFirst { it.id == id }
        if (index < 0) return@withLock conversations.first()
        val updated = conversations[index].copy(archivedAt = nowIso())
        conversations[index] = updated
        updated
    }

    override suspend fun unarchiveConversation(id: String): ConversationDto = mutex.withLock {
        val index = conversations.indexOfFirst { it.id == id }
        if (index < 0) return@withLock conversations.first()
        val updated = conversations[index].copy(archivedAt = null)
        conversations[index] = updated
        updated
    }

    override suspend fun sendTyping(id: String) = Unit

    override suspend fun getConversationMedia(id: String, type: String, before: String?, limit: Int?): MessagesPageDto =
        mutex.withLock {
            val items = messages
                .filter { it.conversationId == id && it.type.equals(type, ignoreCase = true) }
                .sortedByDescending { it.sentAt }
            MessagesPageDto(items, nextCursor = null)
        }

    // --- Messages & media ---

    override suspend fun getMessages(id: String, since: String?, before: String?, limit: Int?): MessagesPageDto =
        mutex.withLock {
            val thread = messages.filter { it.conversationId == id }.sortedBy { it.sentAt }
            if (before != null) {
                // Backward page: the most recent messages older than `before` (TDD §6.8).
                val older = thread.filter { it.sentAt < before }
                val page = if (limit != null) older.takeLast(limit) else older
                val hasMore = limit != null && older.size > limit
                return@withLock MessagesPageDto(page, nextCursor = if (hasMore) page.first().sentAt else null)
            }
            val items = thread.filter { since == null || it.sentAt > since }
            MessagesPageDto(items, nextCursor = items.lastOrNull()?.sentAt)
        }

    override suspend fun sendMessage(id: String, body: SendMessageRequest): MessageDto = mutex.withLock {
        val sentAt = nowIso()
        val ttl = conversations.firstOrNull { it.id == id }?.disappearingTtlSeconds
        val message = MessageDto(
            id = newId("m"),
            clientId = body.clientId,
            conversationId = id,
            senderId = me.id,
            type = body.type,
            text = body.text,
            mediaUrl = body.mediaId?.let { "https://fake.local/media/$it" },
            status = "SENT",
            sentAt = sentAt,
            stickerId = body.stickerId,
            expiresAt = ttl?.let { Instant.now().plusSeconds(it.toLong()).toString() },
        )
        messages.add(message)
        val index = conversations.indexOfFirst { it.id == id }
        if (index >= 0) {
            conversations[index] = conversations[index].copy(
                lastMessage = LastMessageDto(body.text, body.type, me.id, sentAt),
                lastMessageAt = sentAt,
            )
        }
        message
    }

    override suspend fun addReaction(id: String, mid: String, body: ReactionRequest): MessageDto =
        mutex.withLock {
            val index = messages.indexOfFirst { it.id == mid || it.clientId == mid }
            if (index < 0) return@withLock messages.firstOrNull() ?: error("message not found")
            val msg = messages[index]
            val updated = msg.copy(
                reactions = msg.reactions.filterNot { it.userId == me.id } + ReactionDto(me.id, body.emoji),
            )
            messages[index] = updated
            updated
        }

    override suspend fun removeReaction(id: String, mid: String): MessageDto = mutex.withLock {
        val index = messages.indexOfFirst { it.id == mid || it.clientId == mid }
        if (index < 0) return@withLock messages.firstOrNull() ?: error("message not found")
        val msg = messages[index]
        val updated = msg.copy(reactions = msg.reactions.filterNot { it.userId == me.id })
        messages[index] = updated
        updated
    }

    override suspend fun editMessage(id: String, mid: String, body: EditMessageRequest): MessageDto =
        mutex.withLock {
            val index = messages.indexOfFirst { it.id == mid || it.clientId == mid }
            if (index < 0) return@withLock messages.firstOrNull() ?: error("message not found")
            val updated = messages[index].copy(text = body.text, editedAt = nowIso())
            messages[index] = updated
            updated
        }

    override suspend fun starMessage(id: String, mid: String): MessageDto = mutex.withLock {
        val index = messages.indexOfFirst { it.id == mid || it.clientId == mid }
        if (index < 0) return@withLock messages.firstOrNull() ?: error("message not found")
        val updated = messages[index].copy(starred = true)
        messages[index] = updated
        updated
    }

    override suspend fun unstarMessage(id: String, mid: String): MessageDto = mutex.withLock {
        val index = messages.indexOfFirst { it.id == mid || it.clientId == mid }
        if (index < 0) return@withLock messages.firstOrNull() ?: error("message not found")
        val updated = messages[index].copy(starred = false)
        messages[index] = updated
        updated
    }

    override suspend fun getStarredMessages(): List<MessageDto> = mutex.withLock {
        messages.filter { it.starred }.sortedByDescending { it.sentAt }
    }

    override suspend fun deleteForEveryone(id: String, mid: String): MessageDto = mutex.withLock {
        val index = messages.indexOfFirst { it.id == mid || it.clientId == mid }
        if (index < 0) return@withLock messages.firstOrNull() ?: error("message not found")
        val updated = messages[index].copy(text = null, mediaUrl = null, reactions = emptyList())
        // (FakeChatApi MessageDto has no deletedForEveryone in the seed ctors; emulate via blank text.)
        messages[index] = updated.copy(deletedForEveryone = true)
        messages[index]
    }

    override suspend fun hideMessage(id: String, mid: String) {
        mutex.withLock { messages.removeAll { it.id == mid || it.clientId == mid } }
    }

    override suspend fun uploadMedia(file: MultipartBody.Part): MediaUploadResponse {
        val id = newId("media")
        return MediaUploadResponse(id, "https://fake.local/media/$id")
    }

    // --- Search (TDD §6.17) ---

    override suspend fun searchMessages(q: String): List<MessageDto> = mutex.withLock {
        val term = q.trim().lowercase()
        messages
            .filter { it.deletedForEveryone != true && it.text?.lowercase()?.contains(term) == true }
            .sortedByDescending { it.sentAt }
    }

    override suspend fun searchUsers(q: String): List<UserDto> = mutex.withLock {
        val term = q.trim().lowercase()
        directory.filter {
            it.id != me.id &&
                (it.username.lowercase().contains(term) || it.displayName.lowercase().contains(term))
        }
    }

    override suspend fun searchGroups(q: String): List<ConversationDto> = mutex.withLock {
        val term = q.trim().lowercase()
        conversations.filter {
            it.type == "GROUP" && it.title?.lowercase()?.contains(term) == true
        }
    }

    // --- Sync ---

    override suspend fun sync(since: String?): SyncResponse = mutex.withLock {
        SyncResponse(
            conversations = conversations.toList(),
            messages = messages.filter { since == null || it.sentAt > since },
            serverTime = nowIso(),
        )
    }

    private fun token() = "fake-${UUID.randomUUID()}"

    private companion object {
        fun isoAgo(secondsAgo: Long): String = Instant.now().minusSeconds(secondsAgo).toString()
    }
}
