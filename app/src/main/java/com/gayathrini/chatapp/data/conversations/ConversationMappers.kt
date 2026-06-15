package com.gayathrini.chatapp.data.conversations

import com.gayathrini.chatapp.core.network.dto.ConversationDto
import com.gayathrini.chatapp.core.network.dto.UserDto
import com.gayathrini.chatapp.data.mapper.toUser
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.domain.model.ConversationType
import com.gayathrini.chatapp.domain.model.User
import kotlinx.serialization.json.Json
import java.time.Instant

/** Embedded JSON for a group's participants / admin ids on [ConversationEntity] (TDD §6.15). */
private val conversationJson = Json { ignoreUnknownKeys = true }

private fun String?.toEpochMillisOrNull(): Long? =
    this?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

fun ConversationDto.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    type = type,
    peerUserId = peer?.id,
    peerUsername = peer?.username,
    peerDisplayName = peer?.displayName,
    peerAvatarUrl = peer?.avatarUrl,
    title = title,
    iconUrl = iconUrl,
    description = description,
    participantsJson = conversationJson.encodeToString(participants),
    adminsJson = conversationJson.encodeToString(admins),
    createdBy = createdBy,
    lastMessagePreview = lastMessage?.text,
    lastMessageType = lastMessage?.type,
    lastMessageSenderId = lastMessage?.senderId,
    lastMessageAt = lastMessageAt.toEpochMillisOrNull() ?: lastMessage?.sentAt.toEpochMillisOrNull(),
    unreadCount = unreadCount,
    mutedUntil = mutedUntil.toEpochMillisOrNull(),
    pinnedAt = pinnedAt.toEpochMillisOrNull(),
    archivedAt = archivedAt.toEpochMillisOrNull(),
    disappearingTtlSeconds = disappearingTtlSeconds,
)

fun ConversationEntity.toDomain(): Conversation {
    val participants = runCatching {
        conversationJson.decodeFromString<List<UserDto>>(participantsJson).map { it.toUser() }
    }.getOrDefault(emptyList())
    val admins = runCatching { conversationJson.decodeFromString<List<String>>(adminsJson) }
        .getOrDefault(emptyList())
    val peer = peerUserId?.let {
        User(id = it, username = peerUsername.orEmpty(), displayName = peerDisplayName.orEmpty(), avatarUrl = peerAvatarUrl)
    }
    return Conversation(
        id = id,
        peer = peer,
        lastMessagePreview = lastMessagePreview
            ?: lastMessageType?.let {
                when (it) {
                    "IMAGE" -> "📷 Photo"
                    "FILE" -> "📎 Document"
                    else -> null
                }
            },
        lastMessageAt = lastMessageAt?.let { Instant.ofEpochMilli(it) },
        unreadCount = unreadCount,
        type = if (type.equals("GROUP", ignoreCase = true)) ConversationType.GROUP else ConversationType.DIRECT,
        title = title,
        iconUrl = iconUrl,
        description = description,
        participants = participants,
        admins = admins,
        createdBy = createdBy,
        mutedUntil = mutedUntil?.let { Instant.ofEpochMilli(it) },
        pinnedAt = pinnedAt?.let { Instant.ofEpochMilli(it) },
        archivedAt = archivedAt?.let { Instant.ofEpochMilli(it) },
        disappearingTtlSeconds = disappearingTtlSeconds,
    )
}
