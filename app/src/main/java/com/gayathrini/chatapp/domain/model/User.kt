package com.gayathrini.chatapp.domain.model

/** Domain user (TDD §6.1). */
data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
)
