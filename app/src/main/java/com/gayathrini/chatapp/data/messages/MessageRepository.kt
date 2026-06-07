package com.gayathrini.chatapp.data.messages

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.media.MediaPayload
import com.gayathrini.chatapp.domain.model.Message
import kotlinx.coroutines.flow.Flow

/** Messages in a conversation: cached in Room, sent optimistically, received via polling (TDD §5.4). */
interface MessageRepository {
    fun messages(conversationId: String): Flow<List<Message>>

    /** Initial page load (`since=null`). */
    suspend fun loadInitial(conversationId: String): AppResult<Unit>

    /** Incremental fetch since the newest cached message (foreground polling, TDD §7.9). */
    suspend fun poll(conversationId: String): AppResult<Unit>

    suspend fun sendText(conversationId: String, text: String): AppResult<Unit>

    suspend fun sendImage(conversationId: String, payload: MediaPayload, localPreviewUri: String): AppResult<Unit>

    suspend fun retry(conversationId: String, clientId: String): AppResult<Unit>

    suspend fun markRead(conversationId: String): AppResult<Unit>
}
