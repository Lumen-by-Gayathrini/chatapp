package com.gayathrini.chatapp.data.conversations

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room cache row for a conversation (TDD §6.2, §6.15). DIRECT denormalizes the peer; GROUP carries
 * title/icon/description + participants & admins as JSON. Last message denormalized for the list.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val type: String = "DIRECT",
    val peerUserId: String?,
    val peerUsername: String?,
    val peerDisplayName: String?,
    val peerAvatarUrl: String?,
    val title: String? = null,
    val iconUrl: String? = null,
    val description: String? = null,
    /** JSON arrays (parsed by the mapper) — group participants (UserDto) and admin userIds. */
    val participantsJson: String = "[]",
    val adminsJson: String = "[]",
    val createdBy: String? = null,
    val lastMessagePreview: String?,
    val lastMessageType: String?,
    val lastMessageSenderId: String?,
    val lastMessageAt: Long?,
    val unreadCount: Int,
    /** The caller's mute expiry epoch-millis (TDD §6.18); null = not muted. */
    val mutedUntil: Long? = null,
    /** When the caller pinned this conversation epoch-millis (TDD §6.22); null = not pinned. */
    val pinnedAt: Long? = null,
    /** When the caller archived this conversation epoch-millis (TDD §6.23); null = not archived. */
    val archivedAt: Long? = null,
    /** Disappearing-messages TTL in seconds (TDD §6.25); null = off. */
    val disappearingTtlSeconds: Int? = null,
)
