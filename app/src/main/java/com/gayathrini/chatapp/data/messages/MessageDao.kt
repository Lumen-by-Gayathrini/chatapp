package com.gayathrini.chatapp.data.messages

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY sentAt ASC")
    fun observeForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE clientId = :clientId")
    suspend fun getByClientId(clientId: String): MessageEntity?

    @Query("SELECT MAX(sentAt) FROM messages WHERE conversationId = :conversationId")
    suspend fun latestSentAt(conversationId: String): Long?

    /** Highest server `updatedAt` seen — the receipt change-feed cursor (TDD §6.4). */
    @Query("SELECT MAX(updatedAt) FROM messages WHERE conversationId = :conversationId")
    suspend fun latestUpdatedAt(conversationId: String): Long?

    /** Oldest cached `sentAt` — the backward-pagination cursor (TDD §6.8). */
    @Query("SELECT MIN(sentAt) FROM messages WHERE conversationId = :conversationId")
    suspend fun oldestSentAt(conversationId: String): Long?

    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Upsert
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE clientId = :clientId")
    suspend fun updateStatus(clientId: String, status: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteForConversation(conversationId: String)

    /** Remove a single cached message by its server id (TDD §6.14 — delete for me). */
    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)
}
