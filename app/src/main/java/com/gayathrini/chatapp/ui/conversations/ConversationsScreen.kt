package com.gayathrini.chatapp.ui.conversations

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayathrini.chatapp.core.designsystem.component.EmptyState
import com.gayathrini.chatapp.core.designsystem.component.ErrorBanner
import com.gayathrini.chatapp.core.designsystem.component.LoadingBox
import com.gayathrini.chatapp.domain.model.Conversation

@Composable
fun ConversationsRoute(
    onOpenConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenProfile: () -> Unit,
    newChatPeerId: String?,
    onNewChatHandled: () -> Unit,
    viewModel: ConversationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ConversationsEffect.OpenConversation -> onOpenConversation(effect.conversationId)
            }
        }
    }

    LaunchedEffect(newChatPeerId) {
        if (newChatPeerId != null) {
            viewModel.createConversation(newChatPeerId)
            onNewChatHandled()
        }
    }

    // Foreground-only polling (TDD §7.7).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.startPolling()
                Lifecycle.Event.ON_PAUSE -> viewModel.stopPolling()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopPolling()
        }
    }

    ConversationsScreen(
        state = state,
        onConversationClick = viewModel::onConversationClick,
        onConversationLongClick = viewModel::requestDelete,
        onNewChat = onNewChat,
        onOpenContacts = onOpenContacts,
        onOpenProfile = onOpenProfile,
        onRetry = viewModel::refresh,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    state: ConversationsUiState,
    onConversationClick: (Conversation) -> Unit,
    onConversationLongClick: (Conversation) -> Unit,
    onNewChat: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenProfile: () -> Unit,
    onRetry: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
                actions = { OverflowMenu(onContacts = onOpenContacts, onProfile = onOpenProfile) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewChat) {
                Icon(Icons.Default.Add, contentDescription = "New chat")
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> LoadingBox()
                state.error != null && state.conversations.isEmpty() ->
                    ErrorBanner(
                        message = state.error,
                        actionLabel = "Retry",
                        onAction = onRetry,
                        modifier = Modifier.padding(16.dp),
                    )
                state.conversations.isEmpty() -> EmptyState(
                    title = "No chats yet",
                    description = "Tap the + button to start a new chat.",
                    actionLabel = "New chat",
                    onAction = onNewChat,
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.conversations, key = { it.id }) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            onClick = { onConversationClick(conversation) },
                            onLongClick = { onConversationLongClick(conversation) },
                        )
                    }
                }
            }
        }
    }

    val pendingDelete = state.pendingDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text("Delete conversation?") },
            text = { Text("This will remove your chat with ${pendingDelete.peer.displayName}.") },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = onCancelDelete) { Text("Cancel") } },
        )
    }
}

@Composable
private fun OverflowMenu(onContacts: () -> Unit, onProfile: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Contacts") },
            onClick = {
                expanded = false
                onContacts()
            },
        )
        DropdownMenuItem(
            text = { Text("Profile") },
            onClick = {
                expanded = false
                onProfile()
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = conversation.peer.displayName)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.peer.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = conversation.lastMessagePreview ?: "No messages yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = conversation.lastMessageAt.toShortLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (conversation.unreadCount > 0) {
                Spacer(Modifier.heightIn(min = 4.dp))
                Badge(
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = "${conversation.unreadCount} unread messages"
                    },
                ) { Text(conversation.unreadCount.toString()) }
            }
        }
    }
}

@Composable
private fun Avatar(name: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
