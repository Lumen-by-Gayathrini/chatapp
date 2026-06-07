package com.gayathrini.chatapp.ui.launch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gayathrini.chatapp.core.designsystem.component.LoadingBox

/** Splash/router shown on cold start while the session is resolved (TDD §5.1). */
@Composable
fun LaunchRoute(
    onShowLogin: () -> Unit,
    onShowHome: () -> Unit,
    viewModel: LaunchViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    LaunchedEffect(destination) {
        when (destination) {
            LaunchDestination.Login -> onShowLogin()
            LaunchDestination.Home -> onShowHome()
            LaunchDestination.Loading -> Unit
        }
    }
    LoadingBox()
}
