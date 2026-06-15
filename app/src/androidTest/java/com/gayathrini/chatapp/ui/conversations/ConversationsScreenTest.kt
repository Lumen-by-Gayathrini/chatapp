package com.gayathrini.chatapp.ui.conversations

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayathrini.chatapp.core.designsystem.theme.ChatAppTheme
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.domain.model.User
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Compose UI tests for the chat list — pinning evaluation Task 3 (delete conversation): the
 * long-press affordance and the confirm dialog. Runs via `connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class ConversationsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val conversation = Conversation(
        id = "c1",
        peer = User("u1", "john", "John"),
        lastMessagePreview = "Hi",
        lastMessageAt = Instant.now(),
        unreadCount = 0,
    )

    private fun setScreen(
        state: ConversationsUiState,
        onMuteConversation: (Conversation) -> Unit = {},
        onPinConversation: (Conversation) -> Unit = {},
        onDeleteConversation: (Conversation) -> Unit = {},
        onConfirmDelete: () -> Unit = {},
    ) {
        composeRule.setContent {
            ChatAppTheme {
                ConversationsScreen(
                    state = state,
                    onConversationClick = {},
                    onMuteConversation = onMuteConversation,
                    onUnmuteConversation = {},
                    onPinConversation = onPinConversation,
                    onUnpinConversation = {},
                    onArchiveConversation = {},
                    onDeleteConversation = onDeleteConversation,
                    onNewChat = {},
                    onNewGroup = {},
                    onOpenContacts = {},
                    onOpenProfile = {},
                    onOpenSearch = {},
                    onOpenArchived = {},
                    onOpenStarred = {},
                    onRetry = {},
                    onConfirmDelete = onConfirmDelete,
                    onCancelDelete = {},
                    onConfirmMute = {},
                    onCancelMute = {},
                )
            }
        }
    }

    @Test
    fun task3_longPressConversation_opensMenu_andDeletes() {
        var deleteRequested = false
        setScreen(
            ConversationsUiState(isLoading = false, conversations = listOf(conversation)),
            onDeleteConversation = { deleteRequested = true },
        )
        composeRule.onNodeWithText("John").performTouchInput { longClick() }
        composeRule.onNodeWithText("Delete").performClick()
        assertTrue(deleteRequested)
    }

    @Test
    fun longPressConversation_muteOption_requestsMute() {
        var muteRequested = false
        setScreen(
            ConversationsUiState(isLoading = false, conversations = listOf(conversation)),
            onMuteConversation = { muteRequested = true },
        )
        composeRule.onNodeWithText("John").performTouchInput { longClick() }
        composeRule.onNodeWithText("Mute").performClick()
        assertTrue(muteRequested)
    }

    @Test
    fun longPressConversation_pinOption_requestsPin() {
        var pinRequested = false
        setScreen(
            ConversationsUiState(isLoading = false, conversations = listOf(conversation)),
            onPinConversation = { pinRequested = true },
        )
        composeRule.onNodeWithText("John").performTouchInput { longClick() }
        composeRule.onNodeWithText("Pin").performClick()
        assertTrue(pinRequested)
    }

    @Test
    fun task3_confirmDialog_invokesConfirmDelete() {
        var confirmed = false
        setScreen(
            ConversationsUiState(
                isLoading = false,
                conversations = listOf(conversation),
                pendingDelete = conversation,
            ),
            onConfirmDelete = { confirmed = true },
        )
        composeRule.onNodeWithText("Delete").performClick()
        assertTrue(confirmed)
    }
}
