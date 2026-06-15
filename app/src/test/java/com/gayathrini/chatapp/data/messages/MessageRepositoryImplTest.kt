package com.gayathrini.chatapp.data.messages

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.MessageDto
import com.gayathrini.chatapp.core.network.dto.MessagesPageDto
import com.gayathrini.chatapp.data.local.Session
import com.gayathrini.chatapp.data.local.SessionStore
import com.gayathrini.chatapp.data.media.MediaRepository
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.domain.model.MessageDirection
import com.gayathrini.chatapp.domain.model.MessageStatus
import com.gayathrini.chatapp.domain.model.MessageType
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
import org.junit.Assert.assertEquals
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
    fun poll_windowsOnUpdatedAtCursor() = runTest {
        val epoch = 1_717_577_400_000L
        coEvery { dao.latestUpdatedAt("conv") } returns epoch
        coEvery { api.getMessages(any(), any(), any(), any()) } returns MessagesPageDto(emptyList(), null)

        repo.poll("conv")

        val expected = java.time.Instant.ofEpochMilli(epoch).toString()
        coVerify { api.getMessages("conv", expected, null, null) }
    }

    @Test
    fun loadOlder_pagesBackwardFromOldestCached_andReportsMore() = runTest {
        val oldest = 1_717_500_000_000L
        coEvery { dao.oldestSentAt("conv") } returns oldest
        // A non-null nextCursor means a full page came back → more history remains.
        coEvery { api.getMessages(eq("conv"), any(), any(), any()) } returns
            MessagesPageDto(listOf(serverMessage("old1")), nextCursor = "2026-06-05T08:00:00Z")

        val result = repo.loadOlder("conv")

        assertTrue(result is AppResult.Success && (result as AppResult.Success).data)
        val before = java.time.Instant.ofEpochMilli(oldest).toString()
        coVerify { api.getMessages("conv", null, before, 30) }
        coVerify { dao.upsertAll(any()) }
    }

    @Test
    fun poll_returnsTypersFromResponse() = runTest {
        coEvery { dao.latestUpdatedAt("conv") } returns null
        coEvery { api.getMessages(any(), any(), any(), any()) } returns
            MessagesPageDto(emptyList(), null, typers = listOf("u_john"))

        val result = repo.poll("conv")

        assertTrue(result is AppResult.Success)
        assertEquals(listOf("u_john"), (result as AppResult.Success).data)
    }

    @Test
    fun react_addsReaction_andUpsertsReturnedMessage() = runTest {
        coEvery { api.addReaction("conv", "m1", any()) } returns serverMessage("m1")

        val result = repo.react("conv", "m1", "👍")

        assertTrue(result is AppResult.Success)
        coVerify { api.addReaction("conv", "m1", match { it.emoji == "👍" }) }
        coVerify { dao.upsert(any()) }
    }

    @Test
    fun unreact_removesReaction_andUpsertsReturnedMessage() = runTest {
        coEvery { api.removeReaction("conv", "m1") } returns serverMessage("m1")

        val result = repo.unreact("conv", "m1")

        assertTrue(result is AppResult.Success)
        coVerify { api.removeReaction("conv", "m1") }
        coVerify { dao.upsert(any()) }
    }

    @Test
    fun forwardMessage_sendsToTarget_withForwardedFromId() = runTest {
        coEvery { api.sendMessage(eq("conv2"), any()) } returns serverMessage("fwd")
        val original = Message(
            "m_orig", "c_orig", "conv1", MessageDirection.INCOMING, MessageType.TEXT,
            "Forward me", null, MessageStatus.SENT, java.time.Instant.now(),
        )

        val result = repo.forwardMessage("conv2", original)

        assertTrue(result is AppResult.Success)
        coVerify { api.sendMessage("conv2", match { it.forwardedFromId == "m_orig" && it.type == "TEXT" }) }
        coVerify { dao.upsert(any()) }
    }

    @Test
    fun star_upsertsReturnedMessage() = runTest {
        coEvery { api.starMessage("conv", "m1") } returns serverMessage("m1")

        val result = repo.star("conv", "m1")

        assertTrue(result is AppResult.Success)
        coVerify { api.starMessage("conv", "m1") }
        coVerify { dao.upsert(any()) }
    }

    @Test
    fun starredMessages_mapsResponseToDomain() = runTest {
        coEvery { sessionStore.currentUserId() } returns "u_me"
        coEvery { api.getStarredMessages() } returns listOf(serverMessage("s1"))

        val result = repo.starredMessages()

        assertTrue(result is AppResult.Success)
        assertEquals(1, (result as AppResult.Success).data.size)
        coVerify { api.getStarredMessages() }
    }

    @Test
    fun deleteForEveryone_upsertsTombstone() = runTest {
        coEvery { api.deleteForEveryone("conv", "m1") } returns serverMessage("m1")

        val result = repo.deleteForEveryone("conv", "m1")

        assertTrue(result is AppResult.Success)
        coVerify { api.deleteForEveryone("conv", "m1") }
        coVerify { dao.upsert(any()) }
    }

    @Test
    fun editMessage_sendsNewText_andUpsertsReturnedMessage() = runTest {
        coEvery { api.editMessage("conv", "m1", any()) } returns serverMessage("m1")

        val result = repo.editMessage("conv", "m1", "updated")

        assertTrue(result is AppResult.Success)
        coVerify { api.editMessage("conv", "m1", match { it.text == "updated" }) }
        coVerify { dao.upsert(any()) }
    }

    @Test
    fun deleteForMe_hidesViaApi_andRemovesLocally() = runTest {
        coEvery { api.hideMessage("conv", "m1") } returns Unit

        val result = repo.deleteForMe("conv", "m1")

        assertTrue(result is AppResult.Success)
        coVerify { api.hideMessage("conv", "m1") }
        coVerify { dao.deleteById("m1") }
    }

    @Test
    fun conversationMedia_mapsResponseToDomain() = runTest {
        coEvery { sessionStore.currentUserId() } returns "u_me"
        coEvery { api.getConversationMedia(any(), any(), any(), any()) } returns
            MessagesPageDto(listOf(serverMessage("i1")), null)

        val result = repo.conversationMedia("conv", "IMAGE")

        assertTrue(result is AppResult.Success)
        assertEquals(1, (result as AppResult.Success).data.size)
        coVerify { api.getConversationMedia("conv", "IMAGE", null, null) }
    }

    @Test
    fun sendTyping_callsApi() = runTest {
        coEvery { api.sendTyping("conv") } returns Unit

        repo.sendTyping("conv")

        coVerify { api.sendTyping("conv") }
    }

    @Test
    fun loadOlder_withNothingCached_isNoOp() = runTest {
        coEvery { dao.oldestSentAt("conv") } returns null

        val result = repo.loadOlder("conv")

        assertTrue(result is AppResult.Success && !(result as AppResult.Success).data)
        coVerify(exactly = 0) { api.getMessages(any(), any(), any(), any()) }
    }

    @Test
    fun sendFile_uploadsThenSendsFileMessage() = runTest {
        coEvery { sessionStore.currentUserId() } returns "u_me"
        coEvery { mediaRepository.upload(any(), any(), any()) } returns
            AppResult.Success(com.gayathrini.chatapp.data.media.MediaRef("media1", "https://x/doc.pdf"))
        coEvery { api.sendMessage(any(), any()) } answers {
            serverMessage(secondArg<com.gayathrini.chatapp.core.network.dto.SendMessageRequest>().clientId)
        }

        val payload = com.gayathrini.chatapp.data.media.MediaPayload(byteArrayOf(1, 2, 3), "doc.pdf", "application/pdf")
        val result = repo.sendFile("conv", payload)

        assertTrue(result is AppResult.Success)
        // Optimistic FILE row carries the picked filename before the server echo.
        coVerify { dao.upsert(match { it.status == "SENDING" && it.type == "FILE" && it.fileName == "doc.pdf" }) }
        coVerify { api.sendMessage("conv", match { it.type == "FILE" && it.mediaId == "media1" }) }
    }

    @Test
    fun sendSticker_insertsOptimistic_thenSendsStickerMessage() = runTest {
        coEvery { sessionStore.currentUserId() } returns "u_me"
        coEvery { api.sendMessage(any(), any()) } answers {
            serverMessage(secondArg<com.gayathrini.chatapp.core.network.dto.SendMessageRequest>().clientId)
        }

        val result = repo.sendSticker("conv", "smile")

        assertTrue(result is AppResult.Success)
        coVerify { dao.upsert(match { it.status == "SENDING" && it.type == "STICKER" && it.stickerId == "smile" }) }
        coVerify { api.sendMessage("conv", match { it.type == "STICKER" && it.stickerId == "smile" }) }
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

    @Test
    fun messages_hidesExpiredDisappearingMessages() = runTest {
        every { dao.observeForConversation("conv") } returns flowOf(
            listOf(
                MessageEntity("c1", "m1", "conv", "u_me", "TEXT", "live", null, "SENT", 0L),
                MessageEntity(
                    "c2", "m2", "conv", "u_me", "TEXT", "gone", null, "SENT", 0L,
                    expiresAt = System.currentTimeMillis() - 60_000,
                ),
            ),
        )
        every { sessionStore.session } returns flowOf(Session("a", "r", "u_me", "Me"))

        val messages = repo.messages("conv").first()

        // The expired disappearing message (TDD §6.25) is hidden locally.
        assertEquals(listOf("m1"), messages.map { it.id })
    }
}
