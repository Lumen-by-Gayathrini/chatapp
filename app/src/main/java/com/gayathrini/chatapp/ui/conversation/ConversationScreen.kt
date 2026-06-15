package com.gayathrini.chatapp.ui.conversation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gayathrini.chatapp.core.designsystem.component.ErrorBanner
import com.gayathrini.chatapp.core.designsystem.component.LoadingBox
import com.gayathrini.chatapp.core.designsystem.theme.ChatTheme
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.R
import com.gayathrini.chatapp.domain.model.MessageStatus
import com.gayathrini.chatapp.ui.common.presenceLabel
import com.gayathrini.chatapp.domain.model.MessageType
import com.gayathrini.chatapp.domain.model.ReplyPreview
import com.gayathrini.chatapp.ui.stickers.StickerPack
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ConversationRoute(
    onBack: () -> Unit,
    onOpenGroupInfo: () -> Unit = {},
    onOpenMedia: () -> Unit = {},
    viewModel: ConversationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ConversationEffect.NavigateBack -> onBack()
            }
        }
    }

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

    ConversationScreen(
        state = state,
        onBack = onBack,
        onDraftChange = viewModel::onDraftChange,
        onSend = viewModel::sendText,
        onSendSticker = viewModel::sendSticker,
        onPhotoPicked = viewModel::onPhotoPicked,
        onFilePicked = viewModel::onFilePicked,
        onLoadOlder = viewModel::loadOlder,
        onReact = viewModel::onReact,
        onReply = viewModel::onReplyTo,
        onCancelReply = viewModel::cancelReply,
        onEdit = viewModel::onEditRequest,
        onCancelEdit = viewModel::cancelEdit,
        onStar = viewModel::onStar,
        onSetDisappearing = viewModel::setDisappearing,
        onForward = viewModel::onForward,
        onForwardTargetPicked = viewModel::onForwardTargetPicked,
        onCancelForward = viewModel::cancelForward,
        onDeleteMessage = viewModel::onDeleteRequest,
        onDeleteForMe = viewModel::deleteForMe,
        onDeleteForEveryone = viewModel::deleteForEveryone,
        onCancelMessageDelete = viewModel::cancelMessageDelete,
        onRetry = viewModel::retry,
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onDismissError = viewModel::dismissError,
        onOpenGroupInfo = onOpenGroupInfo,
        onOpenMedia = onOpenMedia,
    )
}

/** Document MIME types the picker offers — mirrors the server allowlist (TDD §6.7). */
private val DOCUMENT_MIME_TYPES = arrayOf(
    "application/pdf",
    "text/plain",
    "text/csv",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    state: ConversationUiState,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onSendSticker: (String) -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onFilePicked: (Uri) -> Unit,
    onLoadOlder: () -> Unit,
    onReact: (Message, String) -> Unit,
    onReply: (Message) -> Unit,
    onCancelReply: () -> Unit,
    onEdit: (Message) -> Unit,
    onCancelEdit: () -> Unit,
    onStar: (Message) -> Unit,
    onSetDisappearing: (Int?) -> Unit,
    onForward: (Message) -> Unit,
    onForwardTargetPicked: (Conversation) -> Unit,
    onCancelForward: () -> Unit,
    onDeleteMessage: (Message) -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onCancelMessageDelete: () -> Unit,
    onRetry: (String) -> Unit,
    onRequestDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onDismissError: () -> Unit,
    onOpenGroupInfo: () -> Unit = {},
    onOpenMedia: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPhotoPicked(uri)
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onFilePicked(uri)
    }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Auto-scroll to the bottom only when a NEW message arrives (last id changes) — not when an
    // older page is prepended (TDD §6.8). Stable item keys keep the scroll anchor on prepend.
    val lastMessageKey = state.messages.lastOrNull()?.clientId
    LaunchedEffect(lastMessageKey) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }
    // Load older history when the list reaches the top; the ViewModel guards re-entry/exhaustion.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index -> if (index == 0) onLoadOlder() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = if (state.isGroup) Modifier.clickable { onOpenGroupInfo() } else Modifier,
                    ) {
                        Text(state.peerName.ifBlank { "Chat" })
                        // "typing…" takes precedence over presence while the peer is typing (§6.10).
                        val subtitle =
                            if (state.isPeerTyping) "typing…" else presenceLabel(state.peerOnline, state.peerLastSeenAt)
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            )
                        }
                        // Disappearing-messages hint (TDD §6.25).
                        state.disappearingTtlSeconds?.let { ttl ->
                            Text(
                                text = "⏱ Disappearing: ${disappearingLabel(ttl)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    ThreadOverflow(
                        onMedia = onOpenMedia,
                        onDisappearing = { showDisappearingDialog = true },
                        onDelete = onRequestDelete,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            Column {
                if (state.editing != null) {
                    EditBanner(onCancel = onCancelEdit)
                }
                state.replyingTo?.let { target ->
                    ReplyBanner(
                        target = target,
                        peerName = state.peerName,
                        onCancel = onCancelReply,
                    )
                }
                MessageInput(
                    draft = state.draft,
                    isUploading = state.isUploading,
                    onDraftChange = onDraftChange,
                    onSend = onSend,
                    onSendSticker = onSendSticker,
                    onAttachPhoto = {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onAttachFile = { pickFile.launch(DOCUMENT_MIME_TYPES) },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ChatTheme.colors.wallpaper),
        ) {
            if (state.error != null) {
                ErrorBanner(
                    message = state.error,
                    actionLabel = "Dismiss",
                    onAction = onDismissError,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            when {
                state.isLoading -> LoadingBox()
                state.messages.isEmpty() -> EmptyConversation()
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                ) {
                    if (state.isLoadingMore) {
                        item(key = "history-loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    items(state.messages, key = { it.clientId }) { message ->
                        MessageItem(
                            message = message,
                            peerName = state.peerName,
                            // Group sender label on incoming bubbles (TDD §6.15).
                            senderName = if (state.isGroup && !message.isOutgoing) {
                                state.senderNames[message.senderId]
                            } else {
                                null
                            },
                            onRetry = onRetry,
                            onReact = onReact,
                            onReply = onReply,
                            onForward = onForward,
                            onEdit = onEdit,
                            onStar = onStar,
                            onDelete = onDeleteMessage,
                        )
                    }
                }
            }
        }
    }

    if (state.pendingDelete) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text("Delete chat?") },
            text = { Text("This chat and its messages will be removed.") },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = onCancelDelete) { Text("Cancel") } },
        )
    }

    if (showDisappearingDialog) {
        DisappearingDialog(
            current = state.disappearingTtlSeconds,
            onSelect = {
                showDisappearingDialog = false
                onSetDisappearing(it)
            },
            onDismiss = { showDisappearingDialog = false },
        )
    }

    if (state.forwarding != null) {
        AlertDialog(
            onDismissRequest = onCancelForward,
            title = { Text("Forward to…") },
            text = {
                if (state.forwardTargets.isEmpty()) {
                    Text("No other chats to forward to.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(state.forwardTargets, key = { it.id }) { target ->
                            Text(
                                text = target.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onForwardTargetPicked(target) }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onCancelForward) { Text("Cancel") } },
        )
    }

    state.pendingMessageDelete?.let { target ->
        AlertDialog(
            onDismissRequest = onCancelMessageDelete,
            title = { Text("Delete message?") },
            text = {
                Column {
                    TextButton(onClick = onDeleteForMe) { Text("Delete for me") }
                    // "Delete for everyone" only for your own messages (server enforces the window).
                    if (target.isOutgoing) {
                        TextButton(onClick = onDeleteForEveryone) { Text("Delete for everyone") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onCancelMessageDelete) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ThreadOverflow(onMedia: () -> Unit, onDisappearing: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Media") },
            onClick = {
                expanded = false
                onMedia()
            },
        )
        DropdownMenuItem(
            text = { Text("Disappearing messages") },
            onClick = {
                expanded = false
                onDisappearing()
            },
        )
        DropdownMenuItem(
            text = { Text("Delete chat") },
            onClick = {
                expanded = false
                onDelete()
            },
        )
    }
}

/** The disappearing-messages durations offered (TDD §6.25); null = Off. */
private val DISAPPEARING_OPTIONS: List<Pair<String, Int?>> = listOf(
    "Off" to null,
    "24 hours" to 86_400,
    "7 days" to 604_800,
    "90 days" to 7_776_000,
)

/** Human label for a disappearing TTL in seconds (TDD §6.25). */
private fun disappearingLabel(ttlSeconds: Int): String =
    DISAPPEARING_OPTIONS.firstOrNull { it.second == ttlSeconds }?.first ?: "${ttlSeconds / 86_400} days"

@Composable
private fun DisappearingDialog(
    current: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disappearing messages") },
        text = {
            Column {
                DISAPPEARING_OPTIONS.forEach { (label, seconds) ->
                    Text(
                        text = if (seconds == current) "$label ✓" else label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (seconds == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(seconds) }
                            .padding(vertical = 14.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageItem(
    message: Message,
    peerName: String,
    senderName: String?,
    onRetry: (String) -> Unit,
    onReact: (Message, String) -> Unit,
    onReply: (Message) -> Unit,
    onForward: (Message) -> Unit,
    onEdit: (Message) -> Unit,
    onStar: (Message) -> Unit,
    onDelete: (Message) -> Unit,
) {
    val isOutgoing = message.isOutgoing
    val bubbleColor =
        if (isOutgoing) ChatTheme.colors.outgoingBubble else ChatTheme.colors.incomingBubble
    val shape =
        if (isOutgoing) RoundedCornerShape(8.dp, 2.dp, 8.dp, 8.dp)
        else RoundedCornerShape(2.dp, 8.dp, 8.dp, 8.dp)

    // Long-pressing the bubble opens the actions menu (TDD §6.11–6.14). Disabled on tombstones
    // and unsent/failed messages.
    var showReactionBar by remember(message.clientId) { mutableStateOf(false) }
    val reactable = !message.deletedForEveryone &&
        message.status != MessageStatus.SENDING &&
        message.status != MessageStatus.FAILED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
    ) {
        Box {
            Surface(
                color = bubbleColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = shape,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .sizeIn(maxWidth = 300.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { if (reactable) showReactionBar = true },
                    ),
            ) {
            Column {
            if (senderName != null && !message.deletedForEveryone) {
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                )
            }
            if (message.isForwarded) {
                Text(
                    text = "↪ Forwarded",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                )
            }
            message.replyTo?.let { ReplyQuote(it, peerName) }
            when {
                message.deletedForEveryone -> {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text(
                            text = "This message was deleted",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        MessageMeta(
                            message = message,
                            timeColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            tickColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End).offset(y = (-1).dp),
                        )
                    }
                }

                message.type == MessageType.IMAGE && message.mediaUrl != null -> {
                    Box {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = "Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(240.dp)
                                .clip(shape),
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp),
                        ) {
                            MessageMeta(
                                message = message,
                                timeColor = Color.White,
                                tickColor = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                message.type == MessageType.FILE -> {
                    FileBubbleContent(message)
                }

                message.type == MessageType.STICKER -> {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                        val stickerRes = StickerPack.drawableFor(message.stickerId)
                        if (stickerRes != null) {
                            Image(
                                painter = painterResource(stickerRes),
                                contentDescription = "Sticker",
                                modifier = Modifier.size(120.dp),
                            )
                        } else {
                            Text(text = "🙂 Sticker", style = MaterialTheme.typography.bodyLarge)
                        }
                        MessageMeta(
                            message = message,
                            timeColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            tickColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End).offset(y = (-1).dp),
                        )
                    }
                }

                else -> {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text(
                            text = message.text.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        MessageMeta(
                            message = message,
                            timeColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            tickColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.End)
                                .offset(y = (-1).dp),
                        )
                    }
                }
            }
            }
            }
            ReactionBar(
                visible = showReactionBar,
                selected = message.myReactionEmoji,
                onDismiss = { showReactionBar = false },
                onPick = { emoji ->
                    showReactionBar = false
                    onReact(message, emoji)
                },
                onReply = {
                    showReactionBar = false
                    onReply(message)
                },
                onForward = {
                    showReactionBar = false
                    onForward(message)
                },
                // Edit only the sender's own TEXT messages (TDD §6.21). The server also enforces
                // the time window; this just hides the action where it can't apply.
                canEdit = isOutgoing && message.type == MessageType.TEXT && !message.deletedForEveryone,
                onEdit = {
                    showReactionBar = false
                    onEdit(message)
                },
                isStarred = message.starred,
                onStar = {
                    showReactionBar = false
                    onStar(message)
                },
                onDelete = {
                    showReactionBar = false
                    onDelete(message)
                },
            )
        }
        if (message.reactions.isNotEmpty()) {
            ReactionChips(message)
        }
        if (isOutgoing && message.status == MessageStatus.FAILED) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Not sent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = { onRetry(message.clientId) }) { Text("Retry") }
            }
        }
    }
}

/** The six WhatsApp-style reaction emojis (TDD §6.11). */
private val REACTION_EMOJIS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

/** Long-press menu: a row of reaction emojis (§6.11) plus a Reply action (§6.12). */
@Composable
private fun ReactionBar(
    visible: Boolean,
    selected: String?,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    canEdit: Boolean,
    onEdit: () -> Unit,
    isStarred: Boolean,
    onStar: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = visible, onDismissRequest = onDismiss) {
        Row(modifier = Modifier.padding(horizontal = 4.dp)) {
            REACTION_EMOJIS.forEach { emoji ->
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (emoji == selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        )
                        .clickable { onPick(emoji) }
                        .padding(8.dp),
                )
            }
        }
        DropdownMenuItem(text = { Text("Reply") }, onClick = onReply)
        DropdownMenuItem(text = { Text("Forward") }, onClick = onForward)
        DropdownMenuItem(text = { Text(if (isStarred) "Unstar" else "Star") }, onClick = onStar)
        if (canEdit) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = onEdit)
        }
        DropdownMenuItem(text = { Text("Delete") }, onClick = onDelete)
    }
}

/** The quoted reply shown at the top of a bubble (TDD §6.12). */
@Composable
private fun ReplyQuote(reply: ReplyPreview, peerName: String) {
    val author = if (reply.fromMe) "You" else peerName.ifBlank { "Reply" }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = author,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = reply.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The edit-in-progress banner above the composer (TDD §6.21). */
@Composable
private fun EditBanner(onCancel: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Editing message",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel edit")
            }
        }
    }
}

/** The reply-in-progress banner above the composer (TDD §6.12). */
@Composable
private fun ReplyBanner(target: Message, peerName: String, onCancel: () -> Unit) {
    val author = if (target.isOutgoing) "You" else peerName.ifBlank { "them" }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Replying to $author",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = messagePreview(target),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel reply")
            }
        }
    }
}

/** Short preview of a message for the reply banner. */
private fun messagePreview(message: Message): String = when (message.type) {
    MessageType.IMAGE -> "📷 Photo"
    MessageType.FILE -> "📎 ${message.fileName ?: "Document"}"
    MessageType.STICKER -> "🙂 Sticker"
    MessageType.TEXT -> message.text.orEmpty()
}

/** Reaction chips below a bubble: emoji + count, highlighting the current user's own reaction. */
@Composable
private fun ReactionChips(message: Message) {
    Row(
        modifier = Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        message.reactionCounts.forEach { (emoji, count) ->
            val mine = emoji == message.myReactionEmoji
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = if (count > 1) "$emoji $count" else emoji,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/** A document/file message (TDD §6.7): icon + name + size, tappable to open the stored URL. */
@Composable
private fun FileBubbleContent(message: Message) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .clickable(enabled = message.mediaUrl != null) {
                message.mediaUrl?.let { runCatching { uriHandler.openUri(it) } }
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .widthIn(min = 180.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_file),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = message.fileName ?: "Document",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = fileSubtitle(message)
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        MessageMeta(
            message = message,
            timeColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tickColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.End)
                .offset(y = (-1).dp),
        )
    }
}

/** "PDF · 1.2 MB" style subtitle for a file bubble. */
private fun fileSubtitle(message: Message): String {
    val ext = message.fileName?.substringAfterLast('.', "")?.uppercase().orEmpty()
    val size = formatFileSize(message.sizeBytes)
    return listOf(ext, size).filter { it.isNotEmpty() }.joinToString(" · ")
}

private fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return ""
    val kb = bytes / 1024.0
    return if (kb < 1024) "%.0f KB".format(kb) else "%.1f MB".format(kb / 1024)
}

/** Time + (for outgoing) delivery ticks, WhatsApp-style, tucked at the bottom of the bubble. */
@Composable
private fun MessageMeta(
    message: Message,
    timeColor: Color,
    tickColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        // Star marker (TDD §6.24), shown before the timestamp.
        if (message.starred && !message.deletedForEveryone) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Starred",
                tint = timeColor,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        // "edited" marker (TDD §6.21), shown before the timestamp.
        if (message.isEdited && !message.deletedForEveryone) {
            Text(
                text = "edited",
                style = MaterialTheme.typography.labelMedium,
                fontStyle = FontStyle.Italic,
                color = timeColor,
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = message.sentAt.toBubbleTime(),
            style = MaterialTheme.typography.labelMedium,
            color = timeColor,
        )
        if (message.isOutgoing && message.status != MessageStatus.FAILED) {
            Spacer(Modifier.width(3.dp))
            StatusTicks(status = message.status, tint = tickColor)
        }
    }
}

@Composable
private fun StatusTicks(status: MessageStatus, tint: Color) {
    val description = when (status) {
        MessageStatus.SENDING -> "Sending"
        MessageStatus.DELIVERED -> "Delivered"
        MessageStatus.READ -> "Read"
        MessageStatus.FAILED -> "Not sent"
        else -> "Sent"
    }
    // WhatsApp semantics (TDD §6.4): sent ✓ / delivered ✓✓ / read blue ✓✓.
    val color = if (status == MessageStatus.READ) ChatTheme.colors.readTick else tint
    Box(modifier = Modifier.clearAndSetSemantics { contentDescription = description }) {
        when (status) {
            MessageStatus.SENDING -> SingleTick(color.copy(alpha = 0.5f))
            MessageStatus.DELIVERED, MessageStatus.READ -> DoubleTick(color)
            else -> SingleTick(color) // SENT → single tick
        }
    }
}

@Composable
private fun SingleTick(color: Color) {
    Icon(Icons.Default.Check, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
}

@Composable
private fun DoubleTick(color: Color) {
    Box(modifier = Modifier.width(18.dp)) {
        Icon(Icons.Default.Check, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp).offset(x = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageInput(
    draft: String,
    isUploading: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onSendSticker: (String) -> Unit,
    onAttachPhoto: () -> Unit,
    onAttachFile: () -> Unit,
) {
    var stickerPickerOpen by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 2.dp) {
        // Lift the input controls above whichever is taller: the keyboard (IME) or the navigation
        // bar. `union` applies the larger inset only once (the activity uses adjustResize), so the
        // bar sits just above the keyboard when open and above the nav bar when closed.
        Column(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.ime.union(WindowInsets.navigationBars),
            ),
        ) {
            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (stickerPickerOpen) {
                StickerPicker(onPick = { stickerPickerOpen = false; onSendSticker(it) })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    placeholder = { Text("Message") },
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = {
                        var attachMenuOpen by remember { mutableStateOf(false) }
                        IconButton(onClick = { attachMenuOpen = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Attach")
                        }
                        DropdownMenu(expanded = attachMenuOpen, onDismissRequest = { attachMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Photo") },
                                onClick = { attachMenuOpen = false; onAttachPhoto() },
                            )
                            DropdownMenuItem(
                                text = { Text("Document") },
                                onClick = { attachMenuOpen = false; onAttachFile() },
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { stickerPickerOpen = !stickerPickerOpen }) {
                            Icon(Icons.Default.Face, contentDescription = "Stickers")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                FilledIconButton(
                    onClick = onSend,
                    enabled = draft.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

/** Bundled sticker picker (TDD §6.20): a grid of the client-side pack; tapping one sends it. */
@Composable
private fun StickerPicker(onPick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(StickerPack.all, key = { it.id }) { sticker ->
            Image(
                painter = painterResource(sticker.res),
                contentDescription = "Sticker ${sticker.id}",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPick(sticker.id) }
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun EmptyConversation() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.heightIn(min = 4.dp))
        Text(
            text = "Send a message to start the chat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val bubbleTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

private fun Instant.toBubbleTime(): String = bubbleTimeFormatter.format(this)
