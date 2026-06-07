package com.gayathrini.chatapp.data.conversations

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room cache row for a conversation (TDD §6.2). Peer + last message are denormalized for the list. */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val peerUserId: String,
    val peerUsername: String,
    val peerDisplayName: String,
    val peerAvatarUrl: String?,
    val lastMessagePreview: String?,
    val lastMessageType: String?,
    val lastMessageSenderId: String?,
    val lastMessageAt: Long?,
    val unreadCount: Int,
)
