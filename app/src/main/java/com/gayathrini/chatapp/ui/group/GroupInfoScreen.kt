package com.gayathrini.chatapp.ui.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.gayathrini.chatapp.core.designsystem.component.ErrorBanner
import com.gayathrini.chatapp.core.designsystem.component.LabeledTextField
import com.gayathrini.chatapp.core.designsystem.component.PrimaryButton
import com.gayathrini.chatapp.domain.model.User

@Composable
fun GroupInfoRoute(
    onBack: () -> Unit,
    onOpenMedia: () -> Unit = {},
    viewModel: GroupInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                GroupInfoEffect.Left -> onBack()
            }
        }
    }
    GroupInfoScreen(
        state = state,
        onBack = onBack,
        onOpenMedia = onOpenMedia,
        onTitleChange = viewModel::onTitleChange,
        onSaveTitle = viewModel::saveTitle,
        onOpenAddMember = viewModel::openAddMember,
        onDismissAddMember = viewModel::dismissAddMember,
        onAddMember = viewModel::addMember,
        onRemoveMember = viewModel::removeMember,
        onLeave = viewModel::leave,
        onDismissError = viewModel::dismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    state: GroupInfoUiState,
    onBack: () -> Unit,
    onOpenMedia: () -> Unit = {},
    onTitleChange: (String) -> Unit,
    onSaveTitle: () -> Unit,
    onOpenAddMember: () -> Unit,
    onDismissAddMember: () -> Unit,
    onAddMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onLeave: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val conversation = state.conversation
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Group info") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (state.error != null) {
                ErrorBanner(message = state.error, actionLabel = "Dismiss", onAction = onDismissError)
                Spacer(Modifier.height(8.dp))
            }

            if (state.isAdmin) {
                LabeledTextField(value = state.titleDraft, onValueChange = onTitleChange, label = "Group name")
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onSaveTitle) { Text("Save name") }
            } else {
                Text(text = conversation?.title ?: "Group", style = MaterialTheme.typography.headlineSmall)
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${conversation?.participants?.size ?: 0} members",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (state.isAdmin) {
                    TextButton(onClick = onOpenAddMember) { Text("Add member") }
                }
            }

            conversation?.participants?.forEach { member ->
                MemberRow(
                    member = member,
                    isAdmin = conversation.isAdmin(member.id),
                    canRemove = state.isAdmin && member.id != state.currentUserId,
                    onRemove = { onRemoveMember(member.id) },
                )
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onOpenMedia, modifier = Modifier.fillMaxWidth()) {
                Text("View media")
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton(text = "Leave group", onClick = onLeave)
        }
    }

    if (state.showAddMember) {
        AlertDialog(
            onDismissRequest = onDismissAddMember,
            title = { Text("Add member") },
            text = {
                if (state.addableContacts.isEmpty()) {
                    Text("All your contacts are already in this group.")
                } else {
                    LazyColumn(modifier = Modifier.height(320.dp)) {
                        items(state.addableContacts, key = { it.id }) { contact ->
                            Text(
                                text = contact.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAddMember(contact.user.id) }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismissAddMember) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MemberRow(member: User, isAdmin: Boolean, canRemove: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = member.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (isAdmin) "Admin" else "@${member.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove ${member.displayName}")
            }
        }
    }
}
