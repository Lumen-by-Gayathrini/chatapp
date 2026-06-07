package com.gayathrini.chatapp.data.conversations

import com.gayathrini.chatapp.core.network.dto.ConversationDto
import com.gayathrini.chatapp.core.network.dto.LastMessageDto
import com.gayathrini.chatapp.core.network.dto.UserDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ConversationMappersTest {

    @Test
    fun dtoToEntity_parsesTimestamp_andDenormalizesPeer() {
        val dto = ConversationDto(
            id = "c1",
            peer = UserDto("u1", "john", "John"),
            lastMessage = LastMessageDto("Hello", "TEXT", "u1", "2026-06-05T09:00:00Z"),
            lastMessageAt = "2026-06-05T09:00:00Z",
            unreadCount = 3,
        )

        val entity = dto.toEntity()

        assertEquals("u1", entity.peerUserId)
        assertEquals("Hello", entity.lastMessagePreview)
        assertEquals(3, entity.unreadCount)
        assertNotNull(entity.lastMessageAt)
    }

    @Test
    fun entityToDomain_showsPhotoPreview_forImageWithoutText() {
        val entity = ConversationEntity(
            id = "c1",
            peerUserId = "u1",
            peerUsername = "john",
            peerDisplayName = "John",
            peerAvatarUrl = null,
            lastMessagePreview = null,
            lastMessageType = "IMAGE",
            lastMessageSenderId = "u1",
            lastMessageAt = 0L,
            unreadCount = 0,
        )

        assertEquals("📷 Photo", entity.toDomain().lastMessagePreview)
    }
}
