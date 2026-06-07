package com.gayathrini.chatapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gayathrini.chatapp.ui.auth.LoginRoute
import com.gayathrini.chatapp.ui.auth.RegisterRoute
import com.gayathrini.chatapp.ui.contacts.ContactsRoute
import com.gayathrini.chatapp.ui.conversation.ConversationRoute
import com.gayathrini.chatapp.ui.conversations.ConversationsRoute
import com.gayathrini.chatapp.ui.launch.LaunchRoute
import com.gayathrini.chatapp.ui.profile.ProfileRoute

private const val NEW_CHAT_PEER_ID = "new_chat_peer_id"

/**
 * The single-activity navigation graph (TDD §4.4, §8.2). Conversations is home; the new-chat
 * picker returns the chosen peer via the back-stack saved state.
 */
@Composable
fun ChatNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Launch.route,
    ) {
        composable(Screen.Launch.route) {
            LaunchRoute(
                onShowLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Launch.route) { inclusive = true }
                    }
                },
                onShowHome = {
                    navController.navigate(Screen.Conversations.route) {
                        popUpTo(Screen.Launch.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Login.route) {
            LoginRoute(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoggedIn = {
                    navController.navigate(Screen.Conversations.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Register.route) {
            RegisterRoute(
                onNavigateBack = { navController.popBackStack() },
                onRegistered = {
                    navController.navigate(Screen.Conversations.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Conversations.route) { entry ->
            val newChatPeerId by entry.savedStateHandle
                .getStateFlow<String?>(NEW_CHAT_PEER_ID, null)
                .collectAsStateWithLifecycle()

            ConversationsRoute(
                onOpenConversation = { id -> navController.navigate(Screen.Conversation.route(id)) },
                onNewChat = { navController.navigate(Screen.ContactPicker.route) },
                onOpenContacts = { navController.navigate(Screen.Contacts.route) },
                onOpenProfile = { navController.navigate(Screen.Profile.route) },
                newChatPeerId = newChatPeerId,
                onNewChatHandled = { entry.savedStateHandle[NEW_CHAT_PEER_ID] = null },
            )
        }

        composable(Screen.ContactPicker.route) {
            ContactsRoute(
                pickerMode = true,
                onBack = { navController.popBackStack() },
                onContactSelected = { peerUserId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(NEW_CHAT_PEER_ID, peerUserId)
                    navController.popBackStack()
                },
            )
        }

        composable(Screen.Contacts.route) {
            ContactsRoute(onBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            ProfileRoute(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Conversations.route) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Screen.Conversation.route,
            arguments = listOf(navArgument(Screen.Conversation.ARG) { type = NavType.StringType }),
        ) {
            ConversationRoute(onBack = { navController.popBackStack() })
        }
    }
}
