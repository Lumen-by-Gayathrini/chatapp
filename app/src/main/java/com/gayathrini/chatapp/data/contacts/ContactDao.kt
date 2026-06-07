package com.gayathrini.chatapp.data.contacts

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY displayName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Upsert
    suspend fun upsert(contact: ContactEntity)

    @Upsert
    suspend fun upsertAll(contacts: List<ContactEntity>)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM contacts")
    suspend fun clear()

    /** Replaces the cached set with the latest from the server (network is the source of truth). */
    @Transaction
    suspend fun replaceAll(contacts: List<ContactEntity>) {
        clear()
        upsertAll(contacts)
    }
}
