package com.gayathrini.chatapp.data.conversations

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Upsert
    suspend fun upsert(conversation: ConversationEntity)

    @Upsert
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM conversations")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(conversations: List<ConversationEntity>) {
        clear()
        upsertAll(conversations)
    }
}
