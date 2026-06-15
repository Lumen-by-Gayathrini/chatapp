package com.gayathrini.chatapp.ui.conversations

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.gayathrini.chatapp.R
import com.gayathrini.chatapp.core.designsystem.component.EmptyState
import com.gayathrini.chatapp.core.designsystem.component.ErrorBanner
import com.gayathrini.chatapp.core.designsystem.component.LoadingBox
import com.gayathrini.chatapp.domain.model.Conversation

@Composable
fun ConversationsRoute(
    onOpenConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    onNewGroup: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenArchived: () -> Unit,
    onOpenStarred: () -> Unit,
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
        onMuteConversation = viewModel::requestMute,
        onUnmuteConversation = viewModel::unmute,
        onPinConversation = viewModel::pin,
        onUnpinConversation = viewModel::unpin,
        onArchiveConversation = viewModel::archive,
        onDeleteConversation = viewModel::requestDelete,
        onNewChat = onNewChat,
        onNewGroup = onNewGroup,
        onOpenContacts = onOpenContacts,
        onOpenProfile = onOpenProfile,
        onOpenSearch = onOpenSearch,
        onOpenArchived = onOpenArchived,
        onOpenStarred = onOpenStarred,
        onRetry = viewModel::refresh,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onConfirmMute = viewModel::confirmMute,
        onCancelMute = viewModel::cancelMute,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    state: ConversationsUiState,
    onConversationClick: (Conversation) -> Unit,
    onMuteConversation: (Conversation) -> Unit,
    onUnmuteConversation: (Conversation) -> Unit,
    onPinConversation: (Conversation) -> Unit,
    onUnpinConversation: (Conversation) -> Unit,
    onArchiveConversation: (Conversation) -> Unit,
    onDeleteConversation: (Conversation) -> Unit,
    onNewChat: () -> Unit,
    onNewGroup: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenArchived: () -> Unit,
    onOpenStarred: () -> Unit,
    onRetry: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmMute: (MuteDuration) -> Unit,
    onCancelMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    OverflowMenu(
                        onNewGroup = onNewGroup,
                        onContacts = onOpenContacts,
                        onArchived = onOpenArchived,
                        onStarred = onOpenStarred,
                        onProfile = onOpenProfile,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
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
                            onMute = { onMuteConversation(conversation) },
                            onUnmute = { onUnmuteConversation(conversation) },
                            onPin = { onPinConversation(conversation) },
                            onUnpin = { onUnpinConversation(conversation) },
                            onArchive = { onArchiveConversation(conversation) },
                            onDelete = { onDeleteConversation(conversation) },
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
            text = { Text("This will remove your chat with ${pendingDelete.displayName}.") },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = onCancelDelete) { Text("Cancel") } },
        )
    }

    val mutePickerFor = state.mutePickerFor
    if (mutePickerFor != null) {
        AlertDialog(
            onDismissRequest = onCancelMute,
            title = { Text("Mute ${mutePickerFor.displayName}") },
            text = {
                Column {
                    MuteDuration.entries.forEach { duration ->
                        Text(
                            text = duration.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConfirmMute(duration) }
                                .padding(vertical = 14.dp),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onCancelMute) { Text("Cancel") } },
        )
    }
}

@Composable
private fun OverflowMenu(
    onNewGroup: () -> Unit,
    onContacts: () -> Unit,
    onArchived: () -> Unit,
    onStarred: () -> Unit,
    onProfile: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("New group") },
            onClick = {
                expanded = false
                onNewGroup()
            },
        )
        DropdownMenuItem(
            text = { Text("Contacts") },
            onClick = {
                expanded = false
                onContacts()
            },
        )
        DropdownMenuItem(
            text = { Text("Archived") },
            onClick = {
                expanded = false
                onArchived()
            },
        )
        DropdownMenuItem(
            text = { Text("Starred") },
            onClick = {
                expanded = false
                onStarred()
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
    onMute: () -> Unit,
    onUnmute: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .combinedClickable(onClick = onClick, onLongClick = { menuExpanded = true })
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = conversation.displayName)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (conversation.isMuted) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_notifications_off),
                        contentDescription = "Muted",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (conversation.isPinned) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_pin),
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
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
                style = MaterialTheme.typography.labelMedium,
                color = if (conversation.unreadCount > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (conversation.unreadCount > 0) {
                Spacer(Modifier.heightIn(min = 4.dp))
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = "${conversation.unreadCount} unread messages"
                    },
                ) { Text(conversation.unreadCount.toString()) }
            }
        }

        // Long-press context menu (TDD §6.18, §6.22): pin/unpin, mute/unmute + delete.
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            if (conversation.isPinned) {
                DropdownMenuItem(
                    text = { Text("Unpin") },
                    onClick = {
                        menuExpanded = false
                        onUnpin()
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Pin") },
                    onClick = {
                        menuExpanded = false
                        onPin()
                    },
                )
            }
            if (conversation.isMuted) {
                DropdownMenuItem(
                    text = { Text("Unmute") },
                    onClick = {
                        menuExpanded = false
                        onUnmute()
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Mute") },
                    onClick = {
                        menuExpanded = false
                        onMute()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Archive") },
                onClick = {
                    menuExpanded = false
                    onArchive()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    menuExpanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun Avatar(name: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
