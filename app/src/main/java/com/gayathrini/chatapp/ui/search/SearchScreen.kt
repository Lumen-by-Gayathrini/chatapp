package com.gayathrini.chatapp.ui.search

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayathrini.chatapp.core.designsystem.component.EmptyState
import com.gayathrini.chatapp.core.designsystem.component.ErrorBanner
import com.gayathrini.chatapp.domain.model.Conversation
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.domain.model.MessageType
import com.gayathrini.chatapp.domain.model.User

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SearchEffect.OpenConversation -> onOpenConversation(effect.conversationId)
            }
        }
    }

    SearchScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onBack = onBack,
        onMessageClick = viewModel::onMessageClick,
        onUserClick = viewModel::onUserClick,
        onGroupClick = viewModel::onGroupClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onMessageClick: (Message) -> Unit,
    onUserClick: (User) -> Unit,
    onGroupClick: (Conversation) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        placeholder = { Text("Search messages, people, groups") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            cursorColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val results = state.results
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Messages (${results.messages.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("People (${results.users.size})") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Groups (${results.groups.size})") })
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isSearching -> CircularProgressIndicator(Modifier.align(Alignment.Center).padding(24.dp))
                    state.error != null -> ErrorBanner(message = state.error, modifier = Modifier.padding(16.dp))
                    !state.hasSearched -> EmptyState(
                        title = "Search",
                        description = "Find messages, people, and groups across your chats.",
                    )
                    tab == 0 -> MessageResults(results.messages, onMessageClick)
                    tab == 1 -> UserResults(results.users, onUserClick)
                    else -> GroupResults(results.groups, onGroupClick)
                }
            }
        }
    }
}

@Composable
private fun MessageResults(messages: List<Message>, onClick: (Message) -> Unit) {
    if (messages.isEmpty()) {
        EmptyState(title = "No messages", description = "No messages match your search.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(messages, key = { it.id }) { message ->
            ResultRow(
                initial = "#",
                title = messageSnippet(message),
                subtitle = null,
                onClick = { onClick(message) },
            )
        }
    }
}

@Composable
private fun UserResults(users: List<User>, onClick: (User) -> Unit) {
    if (users.isEmpty()) {
        EmptyState(title = "No people", description = "No people match your search.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(users, key = { it.id }) { user ->
            ResultRow(
                initial = user.displayName.firstOrNull()?.uppercase() ?: "?",
                title = user.displayName,
                subtitle = "@${user.username}",
                onClick = { onClick(user) },
            )
        }
    }
}

@Composable
private fun GroupResults(groups: List<Conversation>, onClick: (Conversation) -> Unit) {
    if (groups.isEmpty()) {
        EmptyState(title = "No groups", description = "No groups match your search.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(groups, key = { it.id }) { group ->
            ResultRow(
                initial = group.displayName.firstOrNull()?.uppercase() ?: "?",
                title = group.displayName,
                subtitle = "${group.participants.size} members",
                onClick = { onClick(group) },
            )
        }
    }
}

private fun messageSnippet(message: Message): String = when {
    message.deletedForEveryone -> "This message was deleted"
    message.type == MessageType.IMAGE -> "📷 Photo"
    message.type == MessageType.FILE -> "📎 ${message.fileName ?: "Document"}"
    else -> message.text ?: ""
}

@Composable
private fun ResultRow(
    initial: String,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = initial, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
