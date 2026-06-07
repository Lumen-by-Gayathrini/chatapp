package com.gayathrini.chatapp.data.contacts

import com.gayathrini.chatapp.core.network.dto.ContactDto
import com.gayathrini.chatapp.core.network.dto.UserDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactMappersTest {

    @Test
    fun dtoToEntity_flattensUser() {
        val entity = ContactDto("c1", UserDto("u1", "john", "John", "http://a"), "Johnny").toEntity()

        assertEquals("c1", entity.id)
        assertEquals("u1", entity.userId)
        assertEquals("john", entity.username)
        assertEquals("Johnny", entity.alias)
        assertEquals("http://a", entity.avatarUrl)
    }

    @Test
    fun entityToDomain_rebuildsUser_andPrefersAlias() {
        val contact = ContactEntity("c1", "u1", "john", "John", null, "Johnny").toDomain()

        assertEquals("u1", contact.user.id)
        assertEquals("John", contact.user.displayName)
        assertEquals("Johnny", contact.displayName) // alias wins
    }
}
