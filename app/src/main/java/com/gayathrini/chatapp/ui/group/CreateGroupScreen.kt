package com.gayathrini.chatapp.ui.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayathrini.chatapp.core.designsystem.component.EmptyState
import com.gayathrini.chatapp.core.designsystem.component.ErrorBanner
import com.gayathrini.chatapp.core.designsystem.component.LabeledTextField
import com.gayathrini.chatapp.core.designsystem.component.PrimaryButton

@Composable
fun CreateGroupRoute(
    onBack: () -> Unit,
    onGroupCreated: (conversationId: String) -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CreateGroupEffect.Created -> onGroupCreated(effect.conversationId)
            }
        }
    }
    CreateGroupScreen(
        state = state,
        onBack = onBack,
        onTitleChange = viewModel::onTitleChange,
        onToggleMember = viewModel::toggleMember,
        onCreate = viewModel::create,
        onDismissError = viewModel::dismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    state: CreateGroupUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onToggleMember: (String) -> Unit,
    onCreate: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("New group") },
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
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            if (state.error != null) {
                ErrorBanner(message = state.error, actionLabel = "Dismiss", onAction = onDismissError)
                Spacer(Modifier.heightIn(min = 8.dp))
            }
            LabeledTextField(value = state.title, onValueChange = onTitleChange, label = "Group name")
            Spacer(Modifier.heightIn(min = 8.dp))
            Text(
                text = "Add members (${state.selectedIds.size} selected)",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.heightIn(min = 4.dp))

            if (state.contacts.isEmpty()) {
                EmptyState(title = "No contacts", description = "Add contacts to create a group.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.contacts, key = { it.id }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleMember(contact.user.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = contact.user.id in state.selectedIds,
                                onCheckedChange = { onToggleMember(contact.user.id) },
                            )
                            Text(text = contact.displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.heightIn(min = 8.dp))
            PrimaryButton(
                text = if (state.isCreating) "Creating…" else "Create group",
                onClick = onCreate,
                enabled = state.canCreate,
            )
        }
    }
}
