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

    /**
     * Incremental fetch since the newest cached message (foreground polling, TDD §7.9). Returns
     * the userIds currently typing in the conversation (TDD §6.10).
     */
    suspend fun poll(conversationId: String): AppResult<List<String>>

    /** Emit a typing heartbeat (TDD §6.10); best-effort. */
    suspend fun sendTyping(conversationId: String): AppResult<Unit>

    /** The conversation's media messages of a given type for the gallery (TDD §6.16). */
    suspend fun conversationMedia(conversationId: String, type: String): AppResult<List<Message>>

    /** Add/replace the current user's emoji reaction on a message (TDD §6.11). */
    suspend fun react(conversationId: String, messageId: String, emoji: String): AppResult<Unit>

    /** Remove the current user's reaction on a message (TDD §6.11). */
    suspend fun unreact(conversationId: String, messageId: String): AppResult<Unit>

    /** Forward [original] into [targetConversationId] (TDD §6.13); the server copies its content. */
    suspend fun forwardMessage(targetConversationId: String, original: Message): AppResult<Unit>

    /** Edit a message's text (TDD §6.21) — sender only, within the server's window. */
    suspend fun editMessage(conversationId: String, messageId: String, text: String): AppResult<Unit>

    /** Star a message for the current user (TDD §6.24). */
    suspend fun star(conversationId: String, messageId: String): AppResult<Unit>

    /** Remove the current user's star from a message (TDD §6.24). */
    suspend fun unstar(conversationId: String, messageId: String): AppResult<Unit>

    /** The current user's starred messages for the Starred screen (TDD §6.24); an ephemeral read. */
    suspend fun starredMessages(): AppResult<List<Message>>

    /** Delete a message for everyone (TDD §6.14) — sender only, within the server's window. */
    suspend fun deleteForEveryone(conversationId: String, messageId: String): AppResult<Unit>

    /** Delete a message for the current user only (TDD §6.14). */
    suspend fun deleteForMe(conversationId: String, messageId: String): AppResult<Unit>

    /**
     * Load the page of messages older than the oldest cached one (TDD §6.8, infinite scroll).
     * Returns `true` when more history may remain, `false` once the start is reached.
     */
    suspend fun loadOlder(conversationId: String): AppResult<Boolean>

    suspend fun sendText(conversationId: String, text: String, replyToId: String? = null): AppResult<Unit>

    suspend fun sendImage(
        conversationId: String,
        payload: MediaPayload,
        localPreviewUri: String,
        replyToId: String? = null,
    ): AppResult<Unit>

    /** Send a document/file (TDD §6.7): upload then send a FILE message referencing it. */
    suspend fun sendFile(
        conversationId: String,
        payload: MediaPayload,
        replyToId: String? = null,
    ): AppResult<Unit>

    /** Send a bundled sticker (TDD §6.20): a STICKER message carrying the [stickerId]. */
    suspend fun sendSticker(
        conversationId: String,
        stickerId: String,
        replyToId: String? = null,
    ): AppResult<Unit>

    suspend fun retry(conversationId: String, clientId: String): AppResult<Unit>

    suspend fun markRead(conversationId: String): AppResult<Unit>
}
