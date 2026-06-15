package com.gayathrini.chatapp.data.search

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.ConversationDto
import com.gayathrini.chatapp.core.network.dto.MessageDto
import com.gayathrini.chatapp.core.network.dto.UserDto
import com.gayathrini.chatapp.data.local.SessionStore
import com.gayathrini.chatapp.domain.model.ConversationType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepositoryImplTest {

    private val api = mockk<ChatApi>()
    private val sessionStore = mockk<SessionStore>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }
    private val repo = SearchRepositoryImpl(api, sessionStore, dispatchers, Json { ignoreUnknownKeys = true })

    @Test
    fun search_fansOutToThreeScopes_andMapsToDomain() = runTest {
        coEvery { sessionStore.currentUserId() } returns "u_me"
        coEvery { api.searchMessages("hello") } returns listOf(
            MessageDto(
                id = "m1", clientId = "c1", conversationId = "conv", senderId = "u_john",
                type = "TEXT", text = "hello world", mediaUrl = null, status = "SENT",
                sentAt = "2026-06-05T09:00:00Z",
            ),
        )
        coEvery { api.searchUsers("hello") } returns listOf(UserDto("u_john", "john", "John", null, "ACTIVE"))
        coEvery { api.searchGroups("hello") } returns listOf(
            ConversationDto(
                id = "g1", peer = null, lastMessage = null, lastMessageAt = null, unreadCount = 0,
                type = "GROUP", title = "Hello Group", participants = emptyList(),
                admins = emptyList(), createdBy = "u_me",
            ),
        )

        val result = repo.search("hello")

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals("hello world", data.messages.single().text)
        assertEquals("John", data.users.single().displayName)
        assertEquals(ConversationType.GROUP, data.groups.single().type)
        coVerify { api.searchMessages("hello") }
        coVerify { api.searchUsers("hello") }
        coVerify { api.searchGroups("hello") }
    }

    @Test
    fun search_blankQuery_returnsEmptyWithoutCallingApi() = runTest {
        val result = repo.search("   ")

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data.isEmpty)
        coVerify(exactly = 0) { api.searchMessages(any()) }
    }

    @Test
    fun search_networkFailure_propagates() = runTest {
        coEvery { sessionStore.currentUserId() } returns "u_me"
        coEvery { api.searchMessages(any()) } throws IOException("offline")
        coEvery { api.searchUsers(any()) } returns emptyList()
        coEvery { api.searchGroups(any()) } returns emptyList()

        val result = repo.search("hello")

        assertTrue(result is AppResult.Failure)
    }
}
