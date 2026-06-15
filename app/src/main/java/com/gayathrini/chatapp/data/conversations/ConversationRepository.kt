package com.gayathrini.chatapp.data.conversations

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.domain.model.Conversation
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Conversations (chat list), cached in Room; refreshed/synced from the network (TDD §5.3, §7.7). */
interface ConversationRepository {
    val conversations: Flow<List<Conversation>>

    /** Full reload via `GET /conversations`. */
    suspend fun refresh(): AppResult<Unit>

    /** Incremental `GET /sync?since=` used by foreground polling. */
    suspend fun sync(): AppResult<Unit>

    suspend fun createConversation(peerUserId: String): AppResult<Conversation>

    suspend fun deleteConversation(id: String): AppResult<Unit>

    /** Create a group (TDD §6.15). */
    suspend fun createGroup(title: String, memberIds: List<String>): AppResult<Conversation>

    /** Add a member to a group (admin, TDD §6.15). */
    suspend fun addMember(conversationId: String, userId: String): AppResult<Conversation>

    /** Remove a member, or leave when [userId] is the current user (TDD §6.15). */
    suspend fun removeMember(conversationId: String, userId: String): AppResult<Unit>

    /** Edit group title / icon / description (admin, TDD §6.15). */
    suspend fun updateGroup(
        conversationId: String,
        title: String?,
        description: String?,
    ): AppResult<Conversation>

    /** Mute a conversation until [until] (null = indefinitely) (TDD §6.18). */
    suspend fun mute(conversationId: String, until: Instant?): AppResult<Unit>

    /** Clear the mute on a conversation (TDD §6.18). */
    suspend fun unmute(conversationId: String): AppResult<Unit>

    /** Pin a conversation to the top of the list (TDD §6.22). */
    suspend fun pin(conversationId: String): AppResult<Unit>

    /** Remove the pin from a conversation (TDD §6.22). */
    suspend fun unpin(conversationId: String): AppResult<Unit>

    /** Archive a conversation out of the main list (TDD §6.23). */
    suspend fun archive(conversationId: String): AppResult<Unit>

    /** Unarchive a conversation back into the main list (TDD §6.23). */
    suspend fun unarchive(conversationId: String): AppResult<Unit>

    /** The caller's archived conversations for the Archived screen (TDD §6.23); an ephemeral read. */
    suspend fun archivedConversations(): AppResult<List<Conversation>>

    /** Set/clear the disappearing-messages TTL in seconds (TDD §6.25); null turns it off. */
    suspend fun setDisappearing(conversationId: String, ttlSeconds: Int?): AppResult<Unit>
}
