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
    /**
     * Server change cursor (TDD §6.4): bumps when a receipt transitions, so the open-conversation
     * poll (which windows on `updatedAt`) re-fetches the row and refreshes its ✓/✓✓ tick. Defaults
     * to 0 for the optimistic row until the server echo supplies the real value.
     */
    val updatedAt: Long = 0L,
    /** Denormalized media metadata for FILE messages (TDD §6.7). */
    val fileName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    /** Bundled sticker id for STICKER messages (TDD §6.20); null otherwise. */
    val stickerId: String? = null,
    /** When the sender edited this message epoch-millis (TDD §6.21); null if never edited. */
    val editedAt: Long? = null,
    /** Whether the current user has starred this message (TDD §6.24). */
    val starred: Boolean = false,
    /** When this disappearing message expires epoch-millis (TDD §6.25); null = never. */
    val expiresAt: Long? = null,
    /** Reactions as a JSON array string (TDD §6.11); parsed by the mapper. Defaults to empty. */
    val reactions: String = "[]",
    /** Quoted reply target as a JSON object string (TDD §6.12); null when not a reply. */
    val replyTo: String? = null,
    /** Original message id when forwarded (TDD §6.13); null otherwise. */
    val forwardedFromMessageId: String? = null,
    /** Tombstone flag — deleted for everyone (TDD §6.14). */
    val deletedForEveryone: Boolean = false,
)
