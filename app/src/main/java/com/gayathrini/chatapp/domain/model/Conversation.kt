package com.gayathrini.chatapp.domain.model

import java.time.Instant

enum class ConversationType { DIRECT, GROUP }

/** Domain conversation (TDD §6.1, §6.15). DIRECT has a [peer]; GROUP has [title] + [participants]. */
data class Conversation(
    val id: String,
    val peer: User?,
    val lastMessagePreview: String?,
    val lastMessageAt: Instant?,
    val unreadCount: Int,
    val type: ConversationType = ConversationType.DIRECT,
    val title: String? = null,
    val iconUrl: String? = null,
    val description: String? = null,
    val participants: List<User> = emptyList(),
    val admins: List<String> = emptyList(),
    val createdBy: String? = null,
    /** The caller's mute expiry (TDD §6.18); null = not muted. */
    val mutedUntil: Instant? = null,
    /** When the caller pinned this conversation (TDD §6.22); null = not pinned. */
    val pinnedAt: Instant? = null,
    /** When the caller archived this conversation (TDD §6.23); null = not archived. */
    val archivedAt: Instant? = null,
    /** Disappearing-messages TTL in seconds (TDD §6.25); null = off. */
    val disappearingTtlSeconds: Int? = null,
) {
    val isGroup: Boolean get() = type == ConversationType.GROUP

    /** Muted if the expiry is still in the future (TDD §6.18). */
    val isMuted: Boolean get() = mutedUntil?.isAfter(Instant.now()) == true

    /** Pinned to the top of the chat list (TDD §6.22). */
    val isPinned: Boolean get() = pinnedAt != null

    /** Archived out of the main chat list (TDD §6.23). */
    val isArchived: Boolean get() = archivedAt != null

    /** What the chat list / top bar shows: the group title, or the peer's name for a 1:1. */
    val displayName: String get() = if (isGroup) (title ?: "Group") else peer?.displayName.orEmpty()

    val displayAvatarUrl: String? get() = if (isGroup) iconUrl else peer?.avatarUrl

    fun isAdmin(userId: String?): Boolean = userId != null && userId in admins

    /** Resolve a sender's display name within this conversation (group sender labels, §6.15). */
    fun participantName(senderId: String): String? =
        participants.firstOrNull { it.id == senderId }?.displayName
}
