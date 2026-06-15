package com.gayathrini.chatapp.ui.starred

import app.cash.turbine.test
import com.gayathrini.chatapp.core.common.AppError
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.data.messages.MessageRepository
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.domain.model.MessageDirection
import com.gayathrini.chatapp.domain.model.MessageStatus
import com.gayathrini.chatapp.domain.model.MessageType
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
class StarredMessagesViewModelTest {

    private val repository = mockk<MessageRepository>()

    private fun starred(id: String) = Message(
        id = id, clientId = "c_$id", conversationId = "conv_$id",
        direction = MessageDirection.INCOMING, type = MessageType.TEXT,
        text = "starred $id", mediaUrl = null, status = MessageStatus.READ, sentAt = Instant.now(),
        starred = true,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { repository.unstar(any(), any()) } returns AppResult.Success(Unit)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsStarredMessages() {
        coEvery { repository.starredMessages() } returns AppResult.Success(listOf(starred("m1")))

        val vm = StarredMessagesViewModel(repository)

        assertEquals(1, vm.state.value.messages.size)
        assertEquals(false, vm.state.value.isLoading)
        coVerify { repository.starredMessages() }
    }

    @Test
    fun load_failure_setsError() {
        coEvery { repository.starredMessages() } returns AppResult.Failure(AppError.Network)

        val vm = StarredMessagesViewModel(repository)

        assertEquals(true, vm.state.value.error != null)
    }

    @Test
    fun unstar_removesFromListAndCallsRepository() {
        val target = starred("m1")
        coEvery { repository.starredMessages() } returns AppResult.Success(listOf(target))
        val vm = StarredMessagesViewModel(repository)

        vm.unstar(target)

        assertEquals(0, vm.state.value.messages.size)
        coVerify { repository.unstar("conv_m1", "m1") }
    }

    @Test
    fun onMessageClick_emitsOpenConversation() = runTest {
        coEvery { repository.starredMessages() } returns AppResult.Success(emptyList())
        val vm = StarredMessagesViewModel(repository)

        vm.effects.test {
            vm.onMessageClick(starred("m1"))
            assertEquals(StarredMessagesEffect.OpenConversation("conv_m1"), awaitItem())
        }
    }
}
