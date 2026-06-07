package com.gayathrini.chatapp.data.messages

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room cache row for a message (TDD §6.2). Keyed by [clientId] (the idempotency key) so an
 * optimistic outgoing row is replaced in place when the server acknowledges it.
 */
@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId", "sentAt"])],
)
data class MessageEntity(
    @PrimaryKey val clientId: String,
    val id: String,
    val conversationId: String,
    val senderId: String,
    val type: String,
    val text: String?,
    val mediaUrl: String?,
    val status: String,
    val sentAt: Long,
)
