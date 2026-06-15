package com.gayathrini.chatapp.ui.contacts

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayathrini.chatapp.core.designsystem.component.EmptyState
import com.gayathrini.chatapp.core.designsystem.component.ErrorBanner
import com.gayathrini.chatapp.core.designsystem.component.LabeledTextField
import com.gayathrini.chatapp.core.designsystem.component.LoadingBox
import com.gayathrini.chatapp.domain.model.Contact

@Composable
fun ContactsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    pickerMode: Boolean = false,
    onContactSelected: (peerUserId: String) -> Unit = {},
    onOpenProfile: (userId: String) -> Unit = {},
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ContactsEffect.OpenConversation -> onContactSelected(effect.peerUserId)
            }
        }
    }
    ContactsScreen(
        state = state,
        pickerMode = pickerMode,
        modifier = modifier,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onContactClick = viewModel::onContactClick,
        onOpenProfile = onOpenProfile,
        onRequestRemove = viewModel::requestRemove,
        onConfirmRemove = viewModel::confirmRemove,
        onCancelRemove = viewModel::cancelRemove,
        onOpenAddDialog = viewModel::openAddDialog,
        onDismissAddDialog = viewModel::dismissAddDialog,
        onAddUsernameChange = viewModel::onAddUsernameChange,
        onSubmitAdd = viewModel::submitAdd,
        onRetry = viewModel::refresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    state: ContactsUiState,
    pickerMode: Boolean,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onContactClick: (Contact) -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    onRequestRemove: (Contact) -> Unit,
    onConfirmRemove: () -> Unit,
    onCancelRemove: () -> Unit,
    onOpenAddDialog: () -> Unit,
    onDismissAddDialog: () -> Unit,
    onAddUsernameChange: (String) -> Unit,
    onSubmitAdd: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (pickerMode) "Choose a contact" else "Contacts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            if (!pickerMode) {
                FloatingActionButton(onClick = onOpenAddDialog) {
                    Icon(Icons.Default.Add, contentDescription = "Add contact")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.heightIn(min = 8.dp))
            LabeledTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = "Search contacts",
            )
            Spacer(Modifier.heightIn(min = 8.dp))

            when {
                state.isLoading -> LoadingBox()
                state.error != null -> ErrorBanner(message = state.error, actionLabel = "Retry", onAction = onRetry)
                state.contacts.isEmpty() -> EmptyState(
                    title = if (state.query.isBlank()) "No contacts yet" else "No matches",
                    description = if (state.query.isBlank()) {
                        "Add someone to start chatting."
                    } else {
                        "Try a different name."
                    },
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.contacts, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            showRemove = !pickerMode,
                            onClick = if (pickerMode) {
                                { onContactClick(contact) }
                            } else {
                                { onOpenProfile(contact.user.id) }
                            },
                            onRemove = { onRequestRemove(contact) },
                        )
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddContactDialog(
            username = state.addUsername,
            error = state.addError,
            isAdding = state.isAdding,
            onUsernameChange = onAddUsernameChange,
            onConfirm = onSubmitAdd,
            onDismiss = onDismissAddDialog,
        )
    }

    val pendingRemoval = state.pendingRemoval
    if (pendingRemoval != null) {
        AlertDialog(
            onDismissRequest = onCancelRemove,
            title = { Text("Remove contact?") },
            text = { Text("Remove ${pendingRemoval.displayName} from your contacts?") },
            confirmButton = { TextButton(onClick = onConfirmRemove) { Text("Remove") } },
            dismissButton = { TextButton(onClick = onCancelRemove) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    showRemove: Boolean,
    onClick: (() -> Unit)?,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = contact.displayName)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${contact.user.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showRemove) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove ${contact.displayName}",
                )
            }
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

@Composable
private fun AddContactDialog(
    username: String,
    error: String?,
    isAdding: Boolean,
    onUsernameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add contact") },
        text = {
            LabeledTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = "Username",
                error = error,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isAdding) {
                Text(if (isAdding) "Adding…" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
