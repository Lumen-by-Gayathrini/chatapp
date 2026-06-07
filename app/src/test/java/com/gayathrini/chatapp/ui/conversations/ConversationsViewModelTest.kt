package com.gayathrini.chatapp.ui.conversations

import app.cash.turbine.test
import com.gayathrini.chatapp.core.common.AppError
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsViewModelTest {

    private val conversationRepository = mockk<ConversationRepository>()

    private fun conversation(id: String, name: String) =
        Conversation(id, User("u1", name.lowercase(), name), null, null, 0)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { conversationRepository.conversations } returns flowOf(emptyList())
        coEvery { conversationRepository.refresh() } returns AppResult.Success(Unit)
        coEvery { conversationRepository.sync() } returns AppResult.Success(Unit)
        coEvery { conversationRepository.deleteConversation(any()) } returns AppResult.Success(Unit)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsConversationsFromCache() {
        every { conversationRepository.conversations } returns flowOf(listOf(conversation("c1", "John")))

        val viewModel = ConversationsViewModel(conversationRepository)

        assertEquals(1, viewModel.state.value.conversations.size)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun createConversation_emitsOpenConversation() = runTest {
        coEvery { conversationRepository.createConversation("u1") } returns
            AppResult.Success(conversation("c1", "John"))
        val viewModel = ConversationsViewModel(conversationRepository)

        viewModel.effects.test {
            viewModel.createConversation("u1")
            assertEquals(ConversationsEffect.OpenConversation("c1"), awaitItem())
        }
    }

    @Test
    fun createConversation_failure_setsError() {
        coEvery { conversationRepository.createConversation("u1") } returns AppResult.Failure(AppError.Server)
        val viewModel = ConversationsViewModel(conversationRepository)

        viewModel.createConversation("u1")

        assertEquals(
            "Something went wrong on our side. Please try again shortly.",
            viewModel.state.value.error,
        )
    }

    @Test
    fun confirmDelete_callsRepository_andClearsPending() {
        val target = conversation("c1", "John")
        val viewModel = ConversationsViewModel(conversationRepository)

        viewModel.requestDelete(target)
        assertEquals(target, viewModel.state.value.pendingDelete)

        viewModel.confirmDelete()
        coVerify { conversationRepository.deleteConversation("c1") }
        assertNull(viewModel.state.value.pendingDelete)
    }

    @Test
    fun polling_runsWhileStarted_andStopsWhenStopped() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        runTest(dispatcher) {
            val viewModel = ConversationsViewModel(conversationRepository)
            runCurrent()

            viewModel.startPolling()
            advanceTimeBy(5_000)
            runCurrent()
            coVerify(exactly = 1) { conversationRepository.sync() }

            advanceTimeBy(5_000)
            runCurrent()
            coVerify(exactly = 2) { conversationRepository.sync() }

            viewModel.stopPolling()
            advanceTimeBy(20_000)
            runCurrent()
            coVerify(exactly = 2) { conversationRepository.sync() }
        }
    }
}
