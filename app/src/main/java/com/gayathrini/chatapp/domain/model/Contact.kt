package com.gayathrini.chatapp.domain.model

/** Domain contact (TDD §6.1). [displayName] prefers the user's chosen alias. */
data class Contact(
    val id: String,
    val user: User,
    val alias: String? = null,
) {
    val displayName: String get() = alias?.takeIf { it.isNotBlank() } ?: user.displayName
}
