package com.gayathrini.chatapp.data.messages

import com.gayathrini.chatapp.core.network.dto.MessageDto
import com.gayathrini.chatapp.domain.model.MessageDirection
import com.gayathrini.chatapp.domain.model.MessageType
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageMappersTest {

    @Test
    fun dtoToEntity_copiesFields() {
        val entity = MessageDto(
            id = "m1",
            clientId = "c1",
            conversationId = "conv",
            senderId = "u_me",
            type = "TEXT",
            text = "Hi",
            mediaUrl = null,
            status = "SENT",
            sentAt = "2026-06-05T09:00:00Z",
        ).toEntity()

        assertEquals("c1", entity.clientId)
        assertEquals("u_me", entity.senderId)
        assertEquals("Hi", entity.text)
    }

    @Test
    fun entityToDomain_outgoing_whenSenderIsCurrentUser() {
        val entity = MessageEntity("c1", "m1", "conv", "u_me", "TEXT", "Hi", null, "SENT", 0L)

        assertEquals(MessageDirection.OUTGOING, entity.toDomain("u_me").direction)
    }

    @Test
    fun entityToDomain_incoming_andImageType() {
        val entity = MessageEntity("c1", "m1", "conv", "u_john", "IMAGE", null, "http://x", "SENT", 0L)

        val message = entity.toDomain("u_me")
        assertEquals(MessageDirection.INCOMING, message.direction)
        assertEquals(MessageType.IMAGE, message.type)
    }
}
