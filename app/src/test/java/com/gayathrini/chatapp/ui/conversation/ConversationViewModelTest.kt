package com.gayathrini.chatapp.ui.conversation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.navigation.Screen
import com.gayathrini.chatapp.core.notifications.MessageNotifier
import com.gayathrini.chatapp.data.conversations.ConversationRepository
import com.gayathrini.chatapp.data.media.MediaPayload
import com.gayathrini.chatapp.data.media.MediaReader
import com.gayathrini.chatapp.data.messages.MessageRepository
import com.gayathrini.chatapp.data.user.UserRepository
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.domain.model.MessageDirection
import com.gayathrini.chatapp.domain.model.MessageStatus
import com.gayathrini.chatapp.domain.model.MessageType
import com.gayathrini.chatapp.domain.model.User
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
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val notifier = mockk<MessageNotifier>(relaxed = true)
    private val mediaReader = mockk<MediaReader>(relaxed = true)

    private fun viewModel() = ConversationViewModel(
        SavedStateHandle(mapOf(Screen.Conversation.ARG to "conv1")),
        messageRepository,
        conversationRepository,
        userRepository,
        notifier,
        mediaReader,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { messageRepository.messages(any()) } returns flowOf(emptyList())
        every { conversationRepository.conversations } returns flowOf(emptyList())
        coEvery { messageRepository.loadInitial(any()) } returns AppResult.Success(Unit)
        coEvery { messageRepository.markRead(any()) } returns AppResult.Success(Unit)
        coEvery { messageRepository.sendTyping(any()) } returns AppResult.Success(Unit)
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
    fun onDraftChange_emitsTypingHeartbeat_throttled() {
        val vm = viewModel()

        vm.onDraftChange("H")
        vm.onDraftChange("He") // within the throttle window → no second emit

        coVerify(exactly = 1) { messageRepository.sendTyping("conv1") }
    }

    @Test
    fun onDraftChange_blank_doesNotEmitTyping() {
        val vm = viewModel()

        vm.onDraftChange("   ")

        coVerify(exactly = 0) { messageRepository.sendTyping(any()) }
    }

    @Test
    fun sendText_sendsAndClearsDraft() {
        coEvery { messageRepository.sendText("conv1", "Hello", null) } returns AppResult.Success(Unit)
        val vm = viewModel()

        vm.onDraftChange("Hello")
        vm.sendText()

        assertEquals("", vm.state.value.draft)
        coVerify { messageRepository.sendText("conv1", "Hello", null) }
    }

    @Test
    fun reply_setsBanner_andSendsWithReplyToId_thenClears() {
        coEvery { messageRepository.sendText("conv1", "Sure", "m1") } returns AppResult.Success(Unit)
        val target = Message(
            "m1", "c1", "conv1", MessageDirection.INCOMING, MessageType.TEXT,
            "Question?", null, MessageStatus.SENT, Instant.now(),
        )
        val vm = viewModel()

        vm.onReplyTo(target)
        assertEquals(target, vm.state.value.replyingTo)

        vm.onDraftChange("Sure")
        vm.sendText()

        assertEquals(null, vm.state.value.replyingTo) // cleared after send
        coVerify { messageRepository.sendText("conv1", "Sure", "m1") }
    }

    @Test
    fun onForwardTargetPicked_forwardsAndClears() = runTest {
        coEvery { messageRepository.forwardMessage(any(), any()) } returns AppResult.Success(Unit)
        val message = Message(
            "m1", "c1", "conv1", MessageDirection.INCOMING, MessageType.TEXT,
            "Hi", null, MessageStatus.SENT, Instant.now(),
        )
        val target = Conversation("conv2", User("u_dora", "dora", "Dora"), null, null, 0)
        val vm = viewModel()

        vm.onForward(message)
        assertEquals(message, vm.state.value.forwarding)

        vm.onForwardTargetPicked(target)

        assertEquals(null, vm.state.value.forwarding)
        coVerify { messageRepository.forwardMessage("conv2", message) }
    }

    @Test
    fun deleteForEveryone_usesPendingMessage_andClears() = runTest {
        coEvery { messageRepository.deleteForEveryone(any(), any()) } returns AppResult.Success(Unit)
        val message = Message(
            "m1", "c1", "conv1", MessageDirection.OUTGOING, MessageType.TEXT,
            "oops", null, MessageStatus.SENT, Instant.now(),
        )
        val vm = viewModel()

        vm.onDeleteRequest(message)
        assertEquals(message, vm.state.value.pendingMessageDelete)
        vm.deleteForEveryone()

        assertEquals(null, vm.state.value.pendingMessageDelete)
        coVerify { messageRepository.deleteForEveryone("conv1", "m1") }
    }

    @Test
    fun deleteForMe_usesPendingMessage() = runTest {
        coEvery { messageRepository.deleteForMe(any(), any()) } returns AppResult.Success(Unit)
        val message = Message(
            "m1", "c1", "conv1", MessageDirection.INCOMING, MessageType.TEXT,
            "hi", null, MessageStatus.SENT, Instant.now(),
        )
        val vm = viewModel()

        vm.onDeleteRequest(message)
        vm.deleteForMe()

        coVerify { messageRepository.deleteForMe("conv1", "m1") }
    }

    @Test
    fun cancelReply_clearsBanner() {
        val target = Message(
            "m1", "c1", "conv1", MessageDirection.INCOMING, MessageType.TEXT,
            "Q", null, MessageStatus.SENT, Instant.now(),
        )
        val vm = viewModel()
        vm.onReplyTo(target)
        vm.cancelReply()
        assertEquals(null, vm.state.value.replyingTo)
    }

    @Test
    fun loadOlder_loadsPage_andUpdatesHasMore() = runTest {
        every { messageRepository.messages("conv1") } returns flowOf(
            listOf(
                Message(
                    "m1", "c1", "conv1", MessageDirection.INCOMING, MessageType.TEXT,
                    "Hi", null, MessageStatus.SENT, Instant.now(),
                ),
            ),
        )
        coEvery { messageRepository.loadOlder("conv1") } returns AppResult.Success(false)
        val vm = viewModel()

        vm.loadOlder()

        assertEquals(false, vm.state.value.isLoadingMore)
        assertEquals(false, vm.state.value.hasMoreHistory)
        coVerify { messageRepository.loadOlder("conv1") }
    }

    @Test
    fun loadOlder_withNoMessages_isNoOp() = runTest {
        val vm = viewModel() // messages empty (default stub)
        vm.loadOlder()
        coVerify(exactly = 0) { messageRepository.loadOlder(any()) }
    }

    @Test
    fun onReact_withNewEmoji_addsReaction() = runTest {
        coEvery { messageRepository.react(any(), any(), any()) } returns AppResult.Success(Unit)
        val message = Message(
            "m1", "c1", "conv1", MessageDirection.INCOMING, MessageType.TEXT,
            "Hi", null, MessageStatus.SENT, Instant.now(), myReactionEmoji = null,
        )
        val vm = viewModel()

        vm.onReact(message, "👍")

        coVerify { messageRepository.react("conv1", "m1", "👍") }
    }

    @Test
    fun onReact_withSameEmoji_removesReaction() = runTest {
        coEvery { messageRepository.unreact(any(), any()) } returns AppResult.Success(Unit)
        val message = Message(
            "m1", "c1", "conv1", MessageDirection.INCOMING, MessageType.TEXT,
            "Hi", null, MessageStatus.SENT, Instant.now(), myReactionEmoji = "👍",
        )
        val vm = viewModel()

        vm.onReact(message, "👍")

        coVerify { messageRepository.unreact("conv1", "m1") }
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
    fun onFilePicked_readsPayload_andSendsFile() = runTest {
        val uri = mockk<Uri>(relaxed = true)
        val payload = MediaPayload(byteArrayOf(1, 2), "doc.pdf", "application/pdf")
        coEvery { mediaReader.read(uri) } returns payload
        coEvery { messageRepository.sendFile(eq("conv1"), eq(payload)) } returns AppResult.Success(Unit)
        val vm = viewModel()

        vm.onFilePicked(uri)

        coVerify { messageRepository.sendFile(eq("conv1"), eq(payload)) }
    }

    @Test
    fun setDisappearing_callsRepository() = runTest {
        coEvery { conversationRepository.setDisappearing("conv1", 86_400) } returns AppResult.Success(Unit)
        val vm = viewModel()

        vm.setDisappearing(86_400)

        coVerify { conversationRepository.setDisappearing("conv1", 86_400) }
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
