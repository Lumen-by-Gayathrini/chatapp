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
data class UpdateProfileRequest(val displayName: String? = null, val avatarUrl: String? = null)

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
    val peer: UserDto,
    val lastMessage: LastMessageDto? = null,
    val lastMessageAt: String? = null,
    val unreadCount: Int = 0,
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
)

@Serializable
data class SendMessageRequest(
    val clientId: String,
    val type: String,
    val text: String? = null,
    val mediaId: String? = null,
)

@Serializable
data class MessagesPageDto(val messages: List<MessageDto>, val nextCursor: String? = null)

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
