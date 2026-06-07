package com.gayathrini.chatapp.ui.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class LaunchDestination { Loading, Login, Home }

/** Decides the cold-start destination from session presence (TDD §5.1). */
@HiltViewModel
class LaunchViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val destination: StateFlow<LaunchDestination> = authRepository.isLoggedIn
        .map { loggedIn -> if (loggedIn) LaunchDestination.Home else LaunchDestination.Login }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LaunchDestination.Loading,
        )
}
