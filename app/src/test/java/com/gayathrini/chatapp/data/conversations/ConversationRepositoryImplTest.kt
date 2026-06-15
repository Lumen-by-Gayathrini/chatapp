package com.gayathrini.chatapp.data.conversations

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.ConversationDto
import com.gayathrini.chatapp.core.network.dto.LastMessageDto
import com.gayathrini.chatapp.core.network.dto.SyncResponse
import com.gayathrini.chatapp.core.network.dto.UserDto
import com.gayathrini.chatapp.core.notifications.MessageNotifier
import com.gayathrini.chatapp.data.local.SessionStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationRepositoryImplTest {

    private val api = mockk<ChatApi>()
    private val dao = mockk<ConversationDao>(relaxed = true)
    private val sessionStore = mockk<SessionStore>()
    private val notifier = mockk<MessageNotifier>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }
    private val repo by lazy {
        ConversationRepositoryImpl(api, dao, sessionStore, notifier, dispatchers, Json { ignoreUnknownKeys = true })
    }

    private fun dto(id: String, peerName: String) =
        ConversationDto(id, UserDto("u_$id", peerName.lowercase(), peerName), null, null, 0)

    @Before
    fun setUp() {
        every { dao.observeAll() } returns flowOf(emptyList())
        coEvery { sessionStore.currentUserId() } returns "u_me"
    }

    @Test
    fun refresh_success_replacesCache() = runTest {
        coEvery { api.getConversations() } returns listOf(dto("c1", "John"))

        val result = repo.refresh()

        assertTrue(result is AppResult.Success)
        coVerify { dao.replaceAll(match { it.size == 1 && it.first().id == "c1" }) }
    }

    @Test
    fun createConversation_success_upsertsAndReturnsDomain() = runTest {
        coEvery { api.createConversation(any()) } returns dto("c2", "Emma")

        val result = repo.createConversation("u_emma")

        assertTrue(result is AppResult.Success)
        assertEquals("c2", (result as AppResult.Success).data.id)
        coVerify { dao.upsert(match { it.id == "c2" }) }
    }

    @Test
    fun deleteConversation_removesOptimistically_andCallsApi() = runTest {
        coEvery { api.deleteConversation("c1") } returns Unit

        repo.deleteConversation("c1")

        coVerify { dao.deleteById("c1") }
        coVerify { api.deleteConversation("c1") }
    }

    @Test
    fun createGroup_upsertsAndReturnsGroupDomain() = runTest {
        coEvery { api.createGroup(any()) } returns ConversationDto(
            id = "g1",
            peer = null,
            lastMessage = null,
            lastMessageAt = null,
            unreadCount = 0,
            type = "GROUP",
            title = "Team",
            participants = listOf(UserDto("u_me", "mary", "Mary"), UserDto("u_john", "john", "John")),
            admins = listOf("u_me"),
            createdBy = "u_me",
        )

        val result = repo.createGroup("Team", listOf("u_john"))

        assertTrue(result is AppResult.Success)
        assertEquals("Team", (result as AppResult.Success).data.title)
        assertTrue(result.data.isGroup)
        coVerify { dao.upsert(match { it.id == "g1" && it.type == "GROUP" }) }
    }

    @Test
    fun addMember_upsertsUpdatedGroup() = runTest {
        coEvery { api.addMember("g1", any()) } returns ConversationDto(
            id = "g1", peer = null, lastMessage = null, lastMessageAt = null, unreadCount = 0,
            type = "GROUP", title = "Team",
            participants = listOf(UserDto("u_me", "mary", "Mary")), admins = listOf("u_me"), createdBy = "u_me",
        )

        val result = repo.addMember("g1", "u_john")

        assertTrue(result is AppResult.Success)
        coVerify { api.addMember("g1", match { it.userId == "u_john" }) }
        coVerify { dao.upsert(match { it.id == "g1" }) }
    }

    @Test
    fun sync_upsertsChangedConversations() = runTest {
        coEvery { api.sync(any()) } returns
            SyncResponse(conversations = listOf(dto("c1", "John")), messages = emptyList(), serverTime = "2026-06-05T10:00:00Z")

        val result = repo.sync()

        assertTrue(result is AppResult.Success)
        coVerify { dao.upsertAll(match { it.size == 1 && it.first().id == "c1" }) }
    }

    private fun incoming(unread: Int, senderId: String = "u_john") = ConversationDto(
        id = "c1",
        peer = UserDto("u_john", "john", "John"),
        lastMessage = LastMessageDto("Hi Mary", "TEXT", senderId, "2026-06-05T10:00:00Z"),
        lastMessageAt = "2026-06-05T10:00:00Z",
        unreadCount = unread,
    )

    @Test
    fun sync_firstSync_seedsNotifierSilently() = runTest {
        coEvery { api.sync(any()) } returns
            SyncResponse(listOf(incoming(unread = 2)), emptyList(), "2026-06-05T10:00:01Z")

        repo.sync() // initial (cursor null) → no banner

        coVerify { notifier.notifyMessage("c1", "John", "Hi Mary", 2, "2026-06-05T10:00:00Z", false) }
    }

    @Test
    fun sync_subsequent_alertsForIncomingUnread() = runTest {
        coEvery { api.sync(any()) } returns
            SyncResponse(listOf(incoming(unread = 1)), emptyList(), "2026-06-05T10:00:01Z")

        repo.sync() // first advances the cursor (alert=false)
        repo.sync() // now alerts

        coVerify { notifier.notifyMessage("c1", "John", "Hi Mary", 1, "2026-06-05T10:00:00Z", true) }
    }

    @Test
    fun sync_doesNotNotifyForOwnMessages() = runTest {
        coEvery { api.sync(any()) } returns
            SyncResponse(listOf(incoming(unread = 3, senderId = "u_me")), emptyList(), "2026-06-05T10:00:01Z")

        repo.sync()

        coVerify(exactly = 0) { notifier.notifyMessage(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun sync_cancelsNotificationWhenRead() = runTest {
        coEvery { api.sync(any()) } returns
            SyncResponse(listOf(incoming(unread = 0)), emptyList(), "2026-06-05T10:00:01Z")

        repo.sync()

        coVerify { notifier.cancel("c1") }
    }
}
