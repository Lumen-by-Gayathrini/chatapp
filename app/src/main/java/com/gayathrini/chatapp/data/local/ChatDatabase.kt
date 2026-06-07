package com.gayathrini.chatapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gayathrini.chatapp.data.contacts.ContactDao
import com.gayathrini.chatapp.data.contacts.ContactEntity
import com.gayathrini.chatapp.data.conversations.ConversationDao
import com.gayathrini.chatapp.data.conversations.ConversationEntity
import com.gayathrini.chatapp.data.messages.MessageDao
import com.gayathrini.chatapp.data.messages.MessageEntity

/**
 * The Room offline cache (TDD §6.2): contacts (Phase 3), conversations (Phase 4), messages (Phase 5).
 */
@Database(
    entities = [ContactEntity::class, ConversationEntity::class, MessageEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
