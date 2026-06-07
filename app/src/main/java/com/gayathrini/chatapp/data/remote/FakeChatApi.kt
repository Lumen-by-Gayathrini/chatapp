package com.gayathrini.chatapp.data.remote

import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.AddContactRequest
import com.gayathrini.chatapp.core.network.dto.AuthResponse
import com.gayathrini.chatapp.core.network.dto.ContactDto
import com.gayathrini.chatapp.core.network.dto.ConversationDto
import com.gayathrini.chatapp.core.network.dto.CreateConversationRequest
import com.gayathrini.chatapp.core.network.dto.LastMessageDto
import com.gayathrini.chatapp.core.network.dto.LoginRequest
import com.gayathrini.chatapp.core.network.dto.LogoutRequest
import com.gayathrini.chatapp.core.network.dto.MediaUploadResponse
import com.gayathrini.chatapp.core.network.dto.MessageDto
import com.gayathrini.chatapp.core.network.dto.MessagesPageDto
import com.gayathrini.chatapp.core.network.dto.ReadRequest
import com.gayathrini.chatapp.core.network.dto.RefreshRequest
import com.gayathrini.chatapp.core.network.dto.RegisterRequest
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
        )
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

    override suspend fun getConversations(): List<ConversationDto> = mutex.withLock {
        conversations.sortedByDescending { it.lastMessageAt }
    }

    override suspend fun createConversation(body: CreateConversationRequest): ConversationDto = mutex.withLock {
        conversations.firstOrNull { it.peer.id == body.peerUserId }?.let { return@withLock it }
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

    override suspend fun markRead(id: String, body: ReadRequest) {
        mutex.withLock {
            val index = conversations.indexOfFirst { it.id == id }
            if (index >= 0) conversations[index] = conversations[index].copy(unreadCount = 0)
        }
    }

    // --- Messages & media ---

    override suspend fun getMessages(id: String, since: String?, limit: Int?): MessagesPageDto = mutex.withLock {
        val items = messages.filter { it.conversationId == id }
            .filter { since == null || it.sentAt > since }
            .sortedBy { it.sentAt }
        MessagesPageDto(items, nextCursor = items.lastOrNull()?.sentAt)
    }

    override suspend fun sendMessage(id: String, body: SendMessageRequest): MessageDto = mutex.withLock {
        val sentAt = nowIso()
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

    override suspend fun uploadMedia(file: MultipartBody.Part): MediaUploadResponse {
        val id = newId("media")
        return MediaUploadResponse(id, "https://fake.local/media/$id")
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
