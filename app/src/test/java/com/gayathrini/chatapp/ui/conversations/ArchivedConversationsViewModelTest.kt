package com.gayathrini.chatapp.ui.conversations

import app.cash.turbine.test
import com.gayathrini.chatapp.core.common.AppError
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ArchivedConversationsViewModelTest {

    private val repository = mockk<ConversationRepository>()

    private fun archived(id: String, name: String) =
        Conversation(id, User("u_$id", name.lowercase(), name), null, null, 0, archivedAt = Instant.now())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { repository.unarchive(any()) } returns AppResult.Success(Unit)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsArchivedConversations() {
        coEvery { repository.archivedConversations() } returns
            AppResult.Success(listOf(archived("c1", "John")))

        val vm = ArchivedConversationsViewModel(repository)

        assertEquals(1, vm.state.value.conversations.size)
        assertEquals(false, vm.state.value.isLoading)
        coVerify { repository.archivedConversations() }
    }

    @Test
    fun load_failure_setsError() {
        coEvery { repository.archivedConversations() } returns AppResult.Failure(AppError.Network)

        val vm = ArchivedConversationsViewModel(repository)

        assertEquals(true, vm.state.value.error != null)
    }

    @Test
    fun unarchive_removesFromListAndCallsRepository() {
        val target = archived("c1", "John")
        coEvery { repository.archivedConversations() } returns AppResult.Success(listOf(target))
        val vm = ArchivedConversationsViewModel(repository)

        vm.unarchive(target)

        assertEquals(0, vm.state.value.conversations.size)
        coVerify { repository.unarchive("c1") }
    }

    @Test
    fun onConversationClick_emitsOpen() = runTest {
        coEvery { repository.archivedConversations() } returns AppResult.Success(emptyList())
        val vm = ArchivedConversationsViewModel(repository)

        vm.effects.test {
            vm.onConversationClick(archived("c1", "John"))
            assertEquals(ArchivedConversationsEffect.OpenConversation("c1"), awaitItem())
        }
    }
}
