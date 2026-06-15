package com.gayathrini.chatapp.domain.model

import java.time.Instant

enum class MessageType { TEXT, IMAGE, FILE, STICKER }

enum class MessageStatus { SENDING, SENT, DELIVERED, READ, FAILED }

enum class MessageDirection { INCOMING, OUTGOING }

/** A single emoji reaction on a message (TDD §6.11). */
data class Reaction(val userId: String, val emoji: String)

/** A quoted reply target (TDD §6.12). [fromMe] is derived from the current user. */
data class ReplyPreview(
    val messageId: String,
    val senderId: String,
    val preview: String,
    val fromMe: Boolean,
)

/** Domain message (TDD §6.1, §6.7). [direction] is derived from the sender vs the current user. */
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
    /** Denormalized media metadata for FILE messages (TDD §6.7). */
    val fileName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    /** Bundled sticker id for STICKER messages (TDD §6.20); null otherwise. */
    val stickerId: String? = null,
    /** Emoji reactions (TDD §6.11) and the current user's own reaction (for the toggle). */
    val reactions: List<Reaction> = emptyList(),
    val myReactionEmoji: String? = null,
    /** Quoted reply target (TDD §6.12); null when this message isn't a reply. */
    val replyTo: ReplyPreview? = null,
    /** Original message id when this was forwarded (TDD §6.13); null otherwise. */
    val forwardedFromMessageId: String? = null,
    /** Tombstone: deleted for everyone (TDD §6.14) → render "This message was deleted". */
    val deletedForEveryone: Boolean = false,
    /** Sender's user id — used for group sender-name labels (TDD §6.15). */
    val senderId: String = "",
    /** When the sender edited this message (TDD §6.21); null if never edited. */
    val editedAt: Instant? = null,
    /** Whether the current user has starred this message (TDD §6.24). */
    val starred: Boolean = false,
    /** When this disappearing message expires (TDD §6.25); null = never. */
    val expiresAt: Instant? = null,
) {
    /** True once a disappearing message's TTL has elapsed (TDD §6.25) → hide it locally. */
    val isExpired: Boolean get() = expiresAt?.isBefore(Instant.now()) == true

    val isForwarded: Boolean get() = forwardedFromMessageId != null

    /** True once the sender has edited this message (TDD §6.21) → render an "edited" label. */
    val isEdited: Boolean get() = editedAt != null

    val isOutgoing: Boolean get() = direction == MessageDirection.OUTGOING

    /** Reactions grouped for display: emoji → count, insertion-ordered. */
    val reactionCounts: List<Pair<String, Int>>
        get() = reactions.groupingBy { it.emoji }.eachCount().entries.map { it.key to it.value }
}
