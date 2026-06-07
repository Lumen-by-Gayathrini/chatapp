package com.gayathrini.chatapp.data.remote

import com.gayathrini.chatapp.core.network.dto.LoginRequest
import com.gayathrini.chatapp.core.network.dto.SendMessageRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the fake API + the unit-test harness (JUnit + coroutines-test) — plan §P1 acceptance.
 */
class FakeChatApiTest {

    private val api = FakeChatApi()

    @Test
    fun login_returnsTokensAndUser() = runTest {
        val response = api.login(LoginRequest("mary", "pw"))
        assertEquals("mary", response.user.username)
        assertTrue(response.accessToken.isNotEmpty())
        assertTrue(response.refreshToken.isNotEmpty())
    }

    @Test
    fun getContacts_returnsSeededData() = runTest {
        val contacts = api.getContacts()
        assertTrue("expected seeded contacts", contacts.isNotEmpty())
    }

    @Test
    fun getConversations_returnsSeededConversation() = runTest {
        val conversations = api.getConversations()
        assertEquals(1, conversations.size)
        assertEquals("John", conversations.first().peer.displayName)
    }

    @Test
    fun sendMessage_appendsAndIsReturnedByGetMessages() = runTest {
        val sent = api.sendMessage(
            id = "conv_john",
            body = SendMessageRequest(clientId = "c-1", type = "TEXT", text = "Hello again"),
        )
        assertEquals("SENT", sent.status)

        val page = api.getMessages("conv_john")
        assertTrue(page.messages.any { it.clientId == "c-1" && it.text == "Hello again" })
    }
}
