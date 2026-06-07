package com.gayathrini.chatapp.data.conversations

import com.gayathrini.chatapp.core.network.dto.ConversationDto
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.domain.model.User
import java.time.Instant

private fun String?.toEpochMillisOrNull(): Long? =
    this?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

fun ConversationDto.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    peerUserId = peer.id,
    peerUsername = peer.username,
    peerDisplayName = peer.displayName,
    peerAvatarUrl = peer.avatarUrl,
    lastMessagePreview = lastMessage?.text,
    lastMessageType = lastMessage?.type,
    lastMessageSenderId = lastMessage?.senderId,
    lastMessageAt = lastMessageAt.toEpochMillisOrNull() ?: lastMessage?.sentAt.toEpochMillisOrNull(),
    unreadCount = unreadCount,
)

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    peer = User(
        id = peerUserId,
        username = peerUsername,
        displayName = peerDisplayName,
        avatarUrl = peerAvatarUrl,
    ),
    lastMessagePreview = lastMessagePreview
        ?: lastMessageType?.let { if (it == "IMAGE") "📷 Photo" else null },
    lastMessageAt = lastMessageAt?.let { Instant.ofEpochMilli(it) },
    unreadCount = unreadCount,
)
