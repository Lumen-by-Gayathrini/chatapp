package com.gayathrini.chatapp.core.network.dto

import kotlinx.serialization.Serializable

// Network DTOs mirroring the consumed API contract (TDD §7 / server TDD §5–6). Timestamps are
// ISO-8601 strings; ids are server strings. DTOs never cross the repository boundary — feature
// mappers convert them to domain models (per-feature, later phases).

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val status: String? = null,
    // Trailing optionals (all defaulted) so existing positional constructions — e.g. the
    // FakeChatApi 5-arg seeds `UserDto(id, username, name, null, "ACTIVE")` — keep working.
    val about: String? = null,
    // Presence (TDD §6.5).
    val online: Boolean = false,
    val lastSeenAt: String? = null,
    val showLastSeen: Boolean = true,
    // True on a profile read when the caller has blocked this user (TDD §6.19).
    val blocked: Boolean = false,
)

// --- Auth & profile ---

@Serializable
data class RegisterRequest(val username: String, val password: String, val displayName: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class AuthResponse(val user: UserDto, val accessToken: String, val refreshToken: String)

@Serializable
data class TokenResponse(val accessToken: String, val refreshToken: String)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val about: String? = null,
    val showLastSeen: Boolean? = null,
)

// --- Contacts ---

@Serializable
data class ContactDto(val id: String, val user: UserDto, val alias: String? = null)

@Serializable
data class AddContactRequest(val username: String? = null, val userId: String? = null)

@Serializable
data class UpdateContactRequest(val alias: String? = null)

// --- Conversations ---

@Serializable
data class LastMessageDto(
    val text: String? = null,
    val type: String,
    val senderId: String,
    val sentAt: String,
)

@Serializable
data class ConversationDto(
    val id: String,
    val peer: UserDto? = null,
    val lastMessage: LastMessageDto? = null,
    val lastMessageAt: String? = null,
    val unreadCount: Int = 0,
    // Group fields (TDD §6.15) — trailing so existing positional constructions keep working.
    val type: String = "DIRECT",
    val title: String? = null,
    val iconUrl: String? = null,
    val description: String? = null,
    val participants: List<UserDto> = emptyList(),
    val admins: List<String> = emptyList(),
    val createdBy: String? = null,
    // The caller's mute expiry (TDD §6.18) — trailing/defaulted so positional constructions keep working.
    val mutedUntil: String? = null,
    /** When the caller pinned this conversation (TDD §6.22); null when not pinned. */
    val pinnedAt: String? = null,
    /** When the caller archived this conversation (TDD §6.23); null when not archived. */
    val archivedAt: String? = null,
    /** Disappearing-messages TTL in seconds (TDD §6.25); null when off. */
    val disappearingTtlSeconds: Int? = null,
)

@Serializable
data class CreateGroupRequest(val title: String, val memberIds: List<String>)

/** Mute a conversation until [until] (ISO-8601); null = mute indefinitely (TDD §6.18). */
@Serializable
data class MuteRequest(val until: String? = null)

@Serializable
data class AddMemberRequest(val userId: String)

/**
 * Set (or clear) disappearing-messages TTL on a conversation (TDD §6.25). No default on the field so
 * it is always serialized — sending `null` explicitly turns disappearing off.
 */
@Serializable
data class SetDisappearingRequest(val disappearingTtlSeconds: Int?)

@Serializable
data class UpdateGroupRequest(
    val title: String? = null,
    val iconUrl: String? = null,
    val description: String? = null,
)

@Serializable
data class CreateConversationRequest(val peerUserId: String)

@Serializable
data class ReadRequest(val upTo: String? = null)

// --- Messages & media ---

@Serializable
data class MessageDto(
    val id: String,
    val clientId: String,
    val conversationId: String,
    val senderId: String,
    val type: String,
    val text: String? = null,
    val mediaUrl: String? = null,
    val status: String,
    val sentAt: String,
    // Trailing optionals (all defaulted) so existing positional constructions (FakeChatApi, tests)
    // keep working. `updatedAt` is the receipt change-feed cursor (§6.4); the file fields are
    // denormalized media metadata for FILE messages (§6.7).
    val updatedAt: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    /** Bundled sticker id for STICKER messages (TDD §6.20); null otherwise. */
    val stickerId: String? = null,
    /** When the sender edited this message (TDD §6.21); null if never edited. */
    val editedAt: String? = null,
    /** Whether the current user has starred this message (TDD §6.24). */
    val starred: Boolean = false,
    /** When this disappearing message expires (TDD §6.25); null = never. */
    val expiresAt: String? = null,
    val reactions: List<ReactionDto> = emptyList(),
    val replyTo: ReplyPreviewDto? = null,
    val forwardedFrom: ForwardedFromDto? = null,
    val deletedForEveryone: Boolean = false,
)

@Serializable
data class ReactionDto(val userId: String, val emoji: String, val at: String? = null)

@Serializable
data class ReactionRequest(val emoji: String)

/** Edit a message's text (TDD §6.21). */
@Serializable
data class EditMessageRequest(val text: String)

@Serializable
data class ReplyPreviewDto(val messageId: String, val senderId: String, val preview: String)

@Serializable
data class ForwardedFromDto(val messageId: String)

@Serializable
data class SendMessageRequest(
    val clientId: String,
    val type: String,
    val text: String? = null,
    val mediaId: String? = null,
    val stickerId: String? = null,
    val replyToId: String? = null,
    val forwardedFromId: String? = null,
)

@Serializable
data class MessagesPageDto(
    val messages: List<MessageDto>,
    val nextCursor: String? = null,
    /** userIds currently typing in this conversation, excluding the caller (TDD §6.10). */
    val typers: List<String> = emptyList(),
)

@Serializable
data class MediaUploadResponse(val mediaId: String, val url: String)

// --- Sync ---

@Serializable
data class SyncResponse(
    val conversations: List<ConversationDto> = emptyList(),
    val messages: List<MessageDto> = emptyList(),
    val serverTime: String,
)

// --- Error envelope ---

@Serializable
data class ErrorEnvelope(val error: ErrorBody)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)
