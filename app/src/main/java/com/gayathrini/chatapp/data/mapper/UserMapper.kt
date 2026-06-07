package com.gayathrini.chatapp.data.mapper

import com.gayathrini.chatapp.core.network.dto.UserDto
import com.gayathrini.chatapp.domain.model.User

/** Shared DTO → domain mapping for users (reused by auth, contacts, conversations). */
fun UserDto.toUser(): User = User(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
)
