package com.gayathrini.chatapp.data.contacts

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room cache row for a contact (TDD §6.2). Flattens the nested user for simple querying. */
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val alias: String?,
)
