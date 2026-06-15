package com.gayathrini.chatapp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.gayathrini.chatapp.core.designsystem.component.ErrorBanner
import com.gayathrini.chatapp.core.designsystem.component.LabeledTextField
import com.gayathrini.chatapp.core.designsystem.component.PrimaryButton

@Composable
fun LoginRoute(
    onNavigateToRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.NavigateHome -> onLoggedIn()
            }
        }
    }
    LoginScreen(
        state = state,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePassword = viewModel::togglePasswordVisibility,
        onSubmit = viewModel::submit,
        onNavigateToRegister = onNavigateToRegister,
        onForgotPassword = onForgotPassword,
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "ChatApp",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Log in to start chatting",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            if (state.formError != null) {
                ErrorBanner(message = state.formError)
                Spacer(Modifier.height(16.dp))
            }

            LabeledTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = "Username",
                error = state.usernameError,
            )
            Spacer(Modifier.height(16.dp))

            LabeledTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = "Password",
                error = state.passwordError,
                isPassword = !state.showPassword,
                keyboardType = KeyboardType.Password,
            )
            TextButton(
                onClick = onTogglePassword,
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text(if (state.showPassword) "Hide password" else "Show password")
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = if (state.isSubmitting) "Logging in…" else "Log in",
                onClick = onSubmit,
                enabled = !state.isSubmitting,
            )

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onForgotPassword) {
                Text("Forgot password?")
            }
            TextButton(onClick = onNavigateToRegister) {
                Text("Create a new account")
            }
        }
    }
}
