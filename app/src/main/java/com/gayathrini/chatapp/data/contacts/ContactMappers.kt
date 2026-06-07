package com.gayathrini.chatapp.data.contacts

import com.gayathrini.chatapp.core.network.dto.ContactDto
import com.gayathrini.chatapp.data.mapper.toUser
import com.gayathrini.chatapp.domain.model.Contact
import com.gayathrini.chatapp.domain.model.User

fun ContactDto.toEntity(): ContactEntity = ContactEntity(
    id = id,
    userId = user.id,
    username = user.username,
    displayName = user.displayName,
    avatarUrl = user.avatarUrl,
    alias = alias,
)

fun ContactDto.toDomain(): Contact = Contact(id = id, user = user.toUser(), alias = alias)

fun ContactEntity.toDomain(): Contact = Contact(
    id = id,
    user = User(id = userId, username = username, displayName = displayName, avatarUrl = avatarUrl),
    alias = alias,
)
