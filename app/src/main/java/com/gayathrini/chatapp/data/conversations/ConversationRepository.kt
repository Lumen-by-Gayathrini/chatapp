package com.gayathrini.chatapp.data.conversations

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

/** Conversations (chat list), cached in Room; refreshed/synced from the network (TDD §5.3, §7.7). */
interface ConversationRepository {
    val conversations: Flow<List<Conversation>>

    /** Full reload via `GET /conversations`. */
    suspend fun refresh(): AppResult<Unit>

    /** Incremental `GET /sync?since=` used by foreground polling. */
    suspend fun sync(): AppResult<Unit>

    suspend fun createConversation(peerUserId: String): AppResult<Conversation>

    suspend fun deleteConversation(id: String): AppResult<Unit>
}
