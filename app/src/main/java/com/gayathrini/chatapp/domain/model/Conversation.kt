package com.gayathrini.chatapp.domain.model

import java.time.Instant

/** Domain conversation (a 1:1 thread) for the chat list (TDD §6.1). */
data class Conversation(
    val id: String,
    val peer: User,
    val lastMessagePreview: String?,
    val lastMessageAt: Instant?,
    val unreadCount: Int,
)
