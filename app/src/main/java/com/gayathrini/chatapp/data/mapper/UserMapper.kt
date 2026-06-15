package com.gayathrini.chatapp.data.mapper

import com.gayathrini.chatapp.core.network.dto.UserDto
import com.gayathrini.chatapp.domain.model.User
import java.time.Instant

/** Shared DTO → domain mapping for users (reused by auth, contacts, conversations, presence). */
fun UserDto.toUser(): User = User(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    about = about,
    online = online,
    lastSeenAt = lastSeenAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
    showLastSeen = showLastSeen,
    blocked = blocked,
)
