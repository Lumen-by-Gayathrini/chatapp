package com.gayathrini.chatapp.data.messages

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.MessageDto
import com.gayathrini.chatapp.data.local.Session
import com.gayathrini.chatapp.data.local.SessionStore
import com.gayathrini.chatapp.data.media.MediaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MessageRepositoryImplTest {

    private val api = mockk<ChatApi>()
    private val dao = mockk<MessageDao>(relaxed = true)
    private val mediaRepository = mockk<MediaRepository>()
    private val sessionStore = mockk<SessionStore>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }
    private val repo = MessageRepositoryImpl(
        api, dao, mediaRepository, sessionStore, dispatchers, Json { ignoreUnknownKeys = true },
    )

    private fun serverMessage(clientId: String) = MessageDto(
        id = "m_server", clientId = clientId, conversationId = "conv", senderId = "u_me",
        type = "TEXT", text = "Hello", mediaUrl = null, status = "SENT", sentAt = "2026-06-05T09:00:00Z",
    )

    @Test
    fun sendText_insertsOptimistic_thenPersistsSentAck() = runTest {
        coEvery { sessionStore.currentUserId() } returns "u_me"
        coEvery { api.sendMessage(any(), any()) } answers {
            serverMessage(secondArg<com.gayathrini.chatapp.core.network.dto.SendMessageRequest>().clientId)
        }

        val result = repo.sendText("conv", "Hello")

        assertTrue(result is AppResult.Success)
        coVerify { dao.upsert(match { it.status == "SENDING" && it.type == "TEXT" }) }
        coVerify { dao.upsert(match { it.status == "SENT" }) }
    }

    @Test
    fun sendText_networkFailure_marksMessageFailed() = runTest {
        coEvery { sessionStore.currentUserId() } returns "u_me"
        coEvery { api.sendMessage(any(), any()) } throws IOException("offline")

        val result = repo.sendText("conv", "Hello")

        assertTrue(result is AppResult.Failure)
        coVerify { dao.updateStatus(any(), "FAILED") }
    }

    @Test
    fun messages_derivesDirectionFromCurrentUser() = runTest {
        every { dao.observeForConversation("conv") } returns flowOf(
            listOf(MessageEntity("c1", "m1", "conv", "u_me", "TEXT", "Hi", null, "SENT", 0L)),
        )
        every { sessionStore.session } returns flowOf(Session("a", "r", "u_me", "Me"))

        val messages = repo.messages("conv").first()

        assertTrue(messages.single().isOutgoing)
    }
}
