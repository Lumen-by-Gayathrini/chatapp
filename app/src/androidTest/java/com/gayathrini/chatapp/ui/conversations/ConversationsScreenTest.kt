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
        onConversationLongClick: (Conversation) -> Unit = {},
        onConfirmDelete: () -> Unit = {},
    ) {
        composeRule.setContent {
            ChatAppTheme {
                ConversationsScreen(
                    state = state,
                    onConversationClick = {},
                    onConversationLongClick = onConversationLongClick,
                    onNewChat = {},
                    onOpenContacts = {},
                    onOpenProfile = {},
                    onRetry = {},
                    onConfirmDelete = onConfirmDelete,
                    onCancelDelete = {},
                )
            }
        }
    }

    @Test
    fun task3_longPressConversation_requestsDelete() {
        var longPressed = false
        setScreen(
            ConversationsUiState(isLoading = false, conversations = listOf(conversation)),
            onConversationLongClick = { longPressed = true },
        )
        composeRule.onNodeWithText("John").performTouchInput { longClick() }
        assertTrue(longPressed)
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
