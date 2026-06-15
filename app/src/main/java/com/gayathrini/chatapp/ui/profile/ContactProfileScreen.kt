package com.gayathrini.chatapp.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gayathrini.chatapp.core.designsystem.component.ErrorBanner
import com.gayathrini.chatapp.core.designsystem.component.LoadingBox
import com.gayathrini.chatapp.core.designsystem.component.PrimaryButton
import com.gayathrini.chatapp.domain.model.User
import com.gayathrini.chatapp.ui.common.presenceLabel

@Composable
fun ContactProfileRoute(
    onBack: () -> Unit,
    onOpenConversation: (conversationId: String) -> Unit,
    viewModel: ContactProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ContactProfileEffect.OpenConversation -> onOpenConversation(effect.conversationId)
            }
        }
    }
    ContactProfileScreen(
        state = state,
        onBack = onBack,
        onMessage = viewModel::startChat,
        onToggleBlock = viewModel::toggleBlock,
        onRetry = viewModel::load,
        onDismissError = viewModel::dismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactProfileScreen(
    state: ContactProfileUiState,
    onBack: () -> Unit,
    onMessage: () -> Unit,
    onToggleBlock: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Contact info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.user != null) {
                        BlockMenu(
                            isBlocked = state.isBlocked,
                            enabled = !state.isUpdatingBlock,
                            onToggleBlock = onToggleBlock,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingBox(modifier = Modifier.padding(innerPadding))
            state.user == null -> ErrorBanner(
                message = state.error ?: "Could not load this contact.",
                actionLabel = "Retry",
                onAction = onRetry,
                modifier = Modifier.padding(innerPadding),
            )
            else -> ContactProfileContent(
                user = state.user,
                isStartingChat = state.isStartingChat,
                isBlocked = state.isBlocked,
                error = state.error,
                onMessage = onMessage,
                onDismissError = onDismissError,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun BlockMenu(isBlocked: Boolean, enabled: Boolean, onToggleBlock: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, enabled = enabled) {
        Icon(Icons.Default.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(if (isBlocked) "Unblock" else "Block") },
            onClick = {
                expanded = false
                onToggleBlock()
            },
        )
    }
}

@Composable
private fun ContactProfileContent(
    user: User,
    isStartingChat: Boolean,
    isBlocked: Boolean,
    error: String?,
    onMessage: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (error != null) {
            ErrorBanner(message = error, actionLabel = "Dismiss", onAction = onDismissError)
            Spacer(Modifier.height(16.dp))
        }

        ContactAvatar(avatarUrl = user.avatarUrl, name = user.displayName)

        Spacer(Modifier.height(16.dp))
        Text(
            text = user.displayName,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "@${user.username}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val presence = presenceLabel(user.online, user.lastSeenAt)
        if (presence != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = presence,
                style = MaterialTheme.typography.bodyMedium,
                color = if (user.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val about = user.about?.trim().orEmpty()
        if (about.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Status",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = about,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(32.dp))
        if (isBlocked) {
            Text(
                text = "You blocked this contact. Unblock to message them again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton(
            text = if (isStartingChat) "Opening…" else "Message",
            onClick = onMessage,
            enabled = !isStartingChat && !isBlocked,
        )
    }
}

@Composable
private fun ContactAvatar(avatarUrl: String?, name: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(120.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}
