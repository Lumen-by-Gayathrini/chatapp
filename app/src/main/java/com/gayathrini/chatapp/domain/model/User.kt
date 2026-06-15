package com.gayathrini.chatapp.domain.model

import java.time.Instant

/** Domain user (TDD §6.1, §6.5). */
data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    /** Free-text "status / about" line (TDD §6.1). */
    val about: String? = null,
    /** Presence (TDD §6.5), with the peer's `showLastSeen` privacy already applied server-side. */
    val online: Boolean = false,
    val lastSeenAt: Instant? = null,
    val showLastSeen: Boolean = true,
    /** True when the current user has blocked this user (TDD §6.19); only set on profile reads. */
    val blocked: Boolean = false,
)
