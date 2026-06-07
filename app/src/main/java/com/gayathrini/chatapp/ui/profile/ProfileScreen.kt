package com.gayathrini.chatapp.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gayathrini.chatapp.core.designsystem.component.ErrorBanner
import com.gayathrini.chatapp.core.designsystem.component.LabeledTextField
import com.gayathrini.chatapp.core.designsystem.component.LoadingBox
import com.gayathrini.chatapp.core.designsystem.component.PrimaryButton

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.LoggedOut -> onLoggedOut()
            }
        }
    }
    ProfileScreen(
        state = state,
        onBack = onBack,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onSave = viewModel::save,
        onAvatarPicked = viewModel::onAvatarPicked,
        onDismissError = viewModel::dismissError,
        onRequestLogout = viewModel::requestLogout,
        onConfirmLogout = viewModel::confirmLogout,
        onCancelLogout = viewModel::cancelLogout,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onAvatarPicked: (Uri) -> Unit,
    onDismissError: () -> Unit,
    onRequestLogout: () -> Unit,
    onConfirmLogout: () -> Unit,
    onCancelLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onAvatarPicked(uri)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingBox(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.error != null) {
                ErrorBanner(message = state.error, actionLabel = "Dismiss", onAction = onDismissError)
                Spacer(Modifier.height(16.dp))
            }

            ProfileAvatar(
                avatarUrl = state.avatarUrl,
                name = state.displayName,
                isUploading = state.isUploadingAvatar,
                onClick = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            )
            Spacer(Modifier.height(8.dp))
            Text(text = "Tap the photo to change it", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(24.dp))
            LabeledTextField(
                value = state.displayName,
                onValueChange = onDisplayNameChange,
                label = "Your name",
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "@${state.username}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = if (state.isSaving) "Saving…" else "Save",
                onClick = onSave,
                enabled = !state.isSaving,
            )
            if (state.savedConfirmation) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Saved",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(32.dp))
            OutlinedButton(onClick = onRequestLogout) {
                Text("Log out")
            }
        }
    }

    if (state.showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = onCancelLogout,
            title = { Text("Log out?") },
            text = { Text("You'll need to log in again to use the app.") },
            confirmButton = { TextButton(onClick = onConfirmLogout) { Text("Log out") } },
            dismissButton = { TextButton(onClick = onCancelLogout) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProfileAvatar(
    avatarUrl: String?,
    name: String,
    isUploading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .size(120.dp)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                isUploading -> CircularProgressIndicator()
                avatarUrl != null -> AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}
