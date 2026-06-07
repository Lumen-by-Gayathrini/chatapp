package com.gayathrini.chatapp.data.conversations

import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.common.DispatcherProvider
import com.gayathrini.chatapp.core.network.ChatApi
import com.gayathrini.chatapp.core.network.dto.ConversationDto
import com.gayathrini.chatapp.core.network.dto.SyncResponse
import com.gayathrini.chatapp.core.network.dto.UserDto
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
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }
    private val repo by lazy { ConversationRepositoryImpl(api, dao, dispatchers, Json { ignoreUnknownKeys = true }) }

    private fun dto(id: String, peerName: String) =
        ConversationDto(id, UserDto("u_$id", peerName.lowercase(), peerName), null, null, 0)

    @Before
    fun setUp() {
        every { dao.observeAll() } returns flowOf(emptyList())
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
    fun sync_upsertsChangedConversations() = runTest {
        coEvery { api.sync(any()) } returns
            SyncResponse(conversations = listOf(dto("c1", "John")), messages = emptyList(), serverTime = "2026-06-05T10:00:00Z")

        val result = repo.sync()

        assertTrue(result is AppResult.Success)
        coVerify { dao.upsertAll(match { it.size == 1 && it.first().id == "c1" }) }
    }
}
