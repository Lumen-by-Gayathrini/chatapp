package com.gayathrini.chatapp.data.messages

import com.gayathrini.chatapp.core.network.dto.MessageDto
import com.gayathrini.chatapp.core.network.dto.ReactionDto
import com.gayathrini.chatapp.core.network.dto.ReplyPreviewDto
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.domain.model.MessageDirection
import com.gayathrini.chatapp.domain.model.MessageStatus
import com.gayathrini.chatapp.domain.model.MessageType
import com.gayathrini.chatapp.domain.model.Reaction
import com.gayathrini.chatapp.domain.model.ReplyPreview
import kotlinx.serialization.json.Json
import java.time.Instant

/** Embedded JSON (reactions array, reply-preview object) on [MessageEntity] (TDD §6.11, §6.12). */
private val reactionsJson = Json { ignoreUnknownKeys = true }

fun MessageDto.toEntity(): MessageEntity {
    val sentAtMillis = runCatching { Instant.parse(sentAt).toEpochMilli() }
        .getOrDefault(System.currentTimeMillis())
    return MessageEntity(
        clientId = clientId,
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        type = type,
        text = text,
        mediaUrl = mediaUrl,
        status = status,
        sentAt = sentAtMillis,
        // Fall back to sentAt when the server omits updatedAt (e.g. the FakeChatApi seeds).
        updatedAt = updatedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: sentAtMillis,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        stickerId = stickerId,
        editedAt = editedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
        starred = starred,
        expiresAt = expiresAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
        reactions = reactionsJson.encodeToString(reactions),
        replyTo = replyTo?.let { reactionsJson.encodeToString(it) },
        forwardedFromMessageId = forwardedFrom?.messageId,
        deletedForEveryone = deletedForEveryone,
    )
}

fun MessageEntity.toDomain(currentUserId: String?): Message {
    val reactionList = runCatching { reactionsJson.decodeFromString<List<ReactionDto>>(reactions) }
        .getOrDefault(emptyList())
    val reply = replyTo?.let { runCatching { reactionsJson.decodeFromString<ReplyPreviewDto>(it) }.getOrNull() }
    return Message(
        id = id,
        clientId = clientId,
        conversationId = conversationId,
        direction = if (senderId == currentUserId) MessageDirection.OUTGOING else MessageDirection.INCOMING,
        type = when {
            type.equals("IMAGE", ignoreCase = true) -> MessageType.IMAGE
            type.equals("FILE", ignoreCase = true) -> MessageType.FILE
            type.equals("STICKER", ignoreCase = true) -> MessageType.STICKER
            else -> MessageType.TEXT
        },
        text = text,
        mediaUrl = mediaUrl,
        status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.SENT),
        sentAt = Instant.ofEpochMilli(sentAt),
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        stickerId = stickerId,
        reactions = reactionList.map { Reaction(it.userId, it.emoji) },
        myReactionEmoji = currentUserId?.let { me -> reactionList.firstOrNull { it.userId == me }?.emoji },
        replyTo = reply?.let {
            ReplyPreview(it.messageId, it.senderId, it.preview, fromMe = it.senderId == currentUserId)
        },
        forwardedFromMessageId = forwardedFromMessageId,
        deletedForEveryone = deletedForEveryone,
        senderId = senderId,
        editedAt = editedAt?.let { Instant.ofEpochMilli(it) },
        starred = starred,
        expiresAt = expiresAt?.let { Instant.ofEpochMilli(it) },
    )
}
