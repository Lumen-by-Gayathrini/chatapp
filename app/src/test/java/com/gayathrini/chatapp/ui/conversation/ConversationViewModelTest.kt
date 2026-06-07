package com.gayathrini.chatapp.ui.conversation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.navigation.Screen
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.data.media.MediaPayload
import com.gayathrini.chatapp.data.media.MediaReader
import com.gayathrini.chatapp.data.messages.MessageRepository
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.domain.model.MessageDirection
import com.gayathrini.chatapp.domain.model.MessageStatus
import com.gayathrini.chatapp.domain.model.MessageType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class ConversationViewModelTest {

    private val messageRepository = mockk<MessageRepository>()
    private val conversationRepository = mockk<ConversationRepository>(relaxed = true)
    private val mediaReader = mockk<MediaReader>(relaxed = true)

    private fun viewModel() = ConversationViewModel(
        SavedStateHandle(mapOf(Screen.Conversation.ARG to "conv1")),
        messageRepository,
        conversationRepository,
        mediaReader,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { messageRepository.messages(any()) } returns flowOf(emptyList())
        every { conversationRepository.conversations } returns flowOf(emptyList())
        coEvery { messageRepository.loadInitial(any()) } returns AppResult.Success(Unit)
        coEvery { messageRepository.markRead(any()) } returns AppResult.Success(Unit)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun sendText_withBlankDraft_doesNotCallRepository() {
        val vm = viewModel()
        vm.sendText()
        coVerify(exactly = 0) { messageRepository.sendText(any(), any()) }
    }

    @Test
    fun sendText_sendsAndClearsDraft() {
        coEvery { messageRepository.sendText("conv1", "Hello") } returns AppResult.Success(Unit)
        val vm = viewModel()

        vm.onDraftChange("Hello")
        vm.sendText()

        assertEquals("", vm.state.value.draft)
        coVerify { messageRepository.sendText("conv1", "Hello") }
    }

    @Test
    fun loadsMessagesFromRepository() {
        every { messageRepository.messages("conv1") } returns flowOf(
            listOf(
                Message(
                    "m1", "c1", "conv1", MessageDirection.INCOMING, MessageType.TEXT,
                    "Hi", null, MessageStatus.SENT, Instant.now(),
                ),
            ),
        )
        val vm = viewModel()

        assertEquals(1, vm.state.value.messages.size)
    }

    @Test
    fun confirmDelete_deletesAndNavigatesBack() = runTest {
        coEvery { conversationRepository.deleteConversation("conv1") } returns AppResult.Success(Unit)
        val vm = viewModel()

        vm.effects.test {
            vm.confirmDelete()
            assertEquals(ConversationEffect.NavigateBack, awaitItem())
        }
        coVerify { conversationRepository.deleteConversation("conv1") }
    }

    @Test
    fun onPhotoPicked_readsPayload_andSendsImage() = runTest {
        val uri = mockk<Uri>(relaxed = true)
        val payload = MediaPayload(byteArrayOf(1, 2), "p.jpg", "image/jpeg")
        coEvery { mediaReader.read(uri) } returns payload
        coEvery { messageRepository.sendImage(eq("conv1"), eq(payload), any()) } returns AppResult.Success(Unit)
        val vm = viewModel()

        vm.onPhotoPicked(uri)

        coVerify { messageRepository.sendImage(eq("conv1"), eq(payload), any()) }
    }

    @Test
    fun onPhotoPicked_unreadablePhoto_setsError() = runTest {
        val uri = mockk<Uri>(relaxed = true)
        coEvery { mediaReader.read(uri) } returns null
        val vm = viewModel()

        vm.onPhotoPicked(uri)

        assertEquals("We couldn't read that photo. Please try another.", vm.state.value.error)
    }
}
