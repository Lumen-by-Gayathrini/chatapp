package com.gayathrini.chatapp.ui.conversation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gayathrini.chatapp.core.designsystem.component.ErrorBanner
import com.gayathrini.chatapp.core.designsystem.component.LoadingBox
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.domain.model.MessageStatus
import com.gayathrini.chatapp.domain.model.MessageType

@Composable
fun ConversationRoute(
    onBack: () -> Unit,
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
        onPhotoPicked = viewModel::onPhotoPicked,
        onRetry = viewModel::retry,
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onDismissError = viewModel::dismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    state: ConversationUiState,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onRetry: (String) -> Unit,
    onRequestDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPhotoPicked(uri)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.peerName.ifBlank { "Chat" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { ThreadOverflow(onDelete = onRequestDelete) },
            )
        },
        bottomBar = {
            MessageInput(
                draft = state.draft,
                isUploading = state.isUploading,
                onDraftChange = onDraftChange,
                onSend = onSend,
                onAttach = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                ) {
                    items(state.messages, key = { it.clientId }) { message ->
                        MessageItem(message = message, onRetry = onRetry)
                    }
                }
            }
        }
    }

    if (state.pendingDelete) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text("Delete conversation?") },
            text = { Text("This will remove this chat and its messages.") },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = onCancelDelete) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ThreadOverflow(onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Delete conversation") },
            onClick = {
                expanded = false
                onDelete()
            },
        )
    }
}

@Composable
private fun MessageItem(message: Message, onRetry: (String) -> Unit) {
    val isOutgoing = message.isOutgoing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
    ) {
        Surface(
            color = if (isOutgoing) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isOutgoing) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            shape = RoundedCornerShape(16.dp),
        ) {
            if (message.type == MessageType.IMAGE && message.mediaUrl != null) {
                AsyncImage(
                    model = message.mediaUrl,
                    contentDescription = "Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(220.dp),
                )
            } else {
                Text(
                    text = message.text.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
        if (isOutgoing) {
            MessageStatusLabel(message = message, onRetry = onRetry)
        }
    }
}

@Composable
private fun MessageStatusLabel(message: Message, onRetry: (String) -> Unit) {
    if (message.status == MessageStatus.FAILED) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Not sent",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = { onRetry(message.clientId) }) { Text("Retry") }
        }
    } else {
        val label = when (message.status) {
            MessageStatus.SENDING -> "Sending…"
            MessageStatus.READ -> "Read"
            else -> "Sent"
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, end = 4.dp),
        )
    }
}

@Composable
private fun MessageInput(
    draft: String,
    isUploading: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        // Lift the input controls above whichever is taller: the keyboard (IME) or the navigation
        // bar. `union` applies the larger inset only once, so the bar sits just above the keyboard
        // when open and above the nav bar when closed — with the Surface background still filling
        // to the screen edge. (The activity uses adjustResize so the window reports the IME inset
        // rather than double-moving.)
        Column(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.ime.union(WindowInsets.navigationBars),
            ),
        ) {
            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onAttach,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Attach a photo")
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    placeholder = { Text("Message") },
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onSend,
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun EmptyConversation() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.heightIn(min = 8.dp))
        Text(
            text = "Say hello to start the conversation.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}
