package com.gayathrini.chatapp.data.messages

import com.gayathrini.chatapp.core.network.dto.MessageDto
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.domain.model.MessageDirection
import com.gayathrini.chatapp.domain.model.MessageStatus
import com.gayathrini.chatapp.domain.model.MessageType
import java.time.Instant

fun MessageDto.toEntity(): MessageEntity = MessageEntity(
    clientId = clientId,
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    type = type,
    text = text,
    mediaUrl = mediaUrl,
    status = status,
    sentAt = runCatching { Instant.parse(sentAt).toEpochMilli() }.getOrDefault(System.currentTimeMillis()),
)

fun MessageEntity.toDomain(currentUserId: String?): Message = Message(
    id = id,
    clientId = clientId,
    conversationId = conversationId,
    direction = if (senderId == currentUserId) MessageDirection.OUTGOING else MessageDirection.INCOMING,
    type = if (type.equals("IMAGE", ignoreCase = true)) MessageType.IMAGE else MessageType.TEXT,
    text = text,
    mediaUrl = mediaUrl,
    status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.SENT),
    sentAt = Instant.ofEpochMilli(sentAt),
)
