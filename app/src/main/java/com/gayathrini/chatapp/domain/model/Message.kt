package com.gayathrini.chatapp.domain.model

import java.time.Instant

enum class MessageType { TEXT, IMAGE }

enum class MessageStatus { SENDING, SENT, DELIVERED, READ, FAILED }

enum class MessageDirection { INCOMING, OUTGOING }

/** Domain message (TDD §6.1). [direction] is derived from the sender vs the current user. */
data class Message(
    val id: String,
    val clientId: String,
    val conversationId: String,
    val direction: MessageDirection,
    val type: MessageType,
    val text: String?,
    val mediaUrl: String?,
    val status: MessageStatus,
    val sentAt: Instant,
) {
    val isOutgoing: Boolean get() = direction == MessageDirection.OUTGOING
}
