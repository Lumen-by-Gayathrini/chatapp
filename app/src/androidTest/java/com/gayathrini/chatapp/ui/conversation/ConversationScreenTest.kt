package com.gayathrini.chatapp.ui.conversation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gayathrini.chatapp.core.designsystem.theme.ChatAppTheme
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.domain.model.MessageDirection
import com.gayathrini.chatapp.domain.model.MessageStatus
import com.gayathrini.chatapp.domain.model.MessageType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant

/**
 * Compose UI tests for the conversation thread — pinning evaluation Task 1 (send text) and
 * Task 2 (photo message rendering). Runs on a device/emulator via `connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class ConversationScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun outgoing(text: String, status: MessageStatus) = Message(
        id = "m1", clientId = "c1", conversationId = "conv",
        direction = MessageDirection.OUTGOING, type = MessageType.TEXT,
        text = text, mediaUrl = null, status = status, sentAt = Instant.now(),
    )

    private fun setScreen(state: ConversationUiState, onSend: () -> Unit = {}) {
        composeRule.setContent {
            ChatAppTheme {
                ConversationScreen(
                    state = state,
                    onBack = {},
                    onDraftChange = {},
                    onSend = onSend,
                    onSendSticker = {},
                    onPhotoPicked = {},
                    onFilePicked = {},
                    onLoadOlder = {},
                    onReact = { _, _ -> },
                    onReply = {},
                    onCancelReply = {},
                    onEdit = {},
                    onCancelEdit = {},
                    onStar = {},
                    onSetDisappearing = {},
                    onForward = {},
                    onForwardTargetPicked = {},
                    onCancelForward = {},
                    onDeleteMessage = {},
                    onDeleteForMe = {},
                    onDeleteForEveryone = {},
                    onCancelMessageDelete = {},
                    onRetry = {},
                    onRequestDelete = {},
                    onConfirmDelete = {},
                    onCancelDelete = {},
                    onDismissError = {},
                )
            }
        }
    }

    @Test
    fun task1_sentTextMessage_isShownWithStatus() {
        setScreen(
            ConversationUiState(
                peerName = "John",
                isLoading = false,
                messages = listOf(outgoing("Hello E2E", MessageStatus.SENT)),
            ),
        )
        composeRule.onNodeWithText("Hello E2E").assertIsDisplayed()
        // Delivery status is now a WhatsApp-style tick exposed via content description.
        composeRule.onNodeWithContentDescription("Sent").assertExists()
    }

    @Test
    fun task1_sendButton_invokesOnSend() {
        var sent = false
        setScreen(
            ConversationUiState(peerName = "John", isLoading = false, draft = "Hi"),
            onSend = { sent = true },
        )
        composeRule.onNodeWithContentDescription("Send").performClick()
        assertTrue(sent)
    }

    @Test
    fun task2_imageMessage_rendersPhoto() {
        val image = Message(
            id = "m2", clientId = "c2", conversationId = "conv",
            direction = MessageDirection.OUTGOING, type = MessageType.IMAGE,
            text = null, mediaUrl = "https://example.com/p.jpg",
            status = MessageStatus.SENT, sentAt = Instant.now(),
        )
        setScreen(ConversationUiState(peerName = "John", isLoading = false, messages = listOf(image)))
        composeRule.onNodeWithContentDescription("Photo").assertExists()
    }
}
