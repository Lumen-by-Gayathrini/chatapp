package com.gayathrini.chatapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gayathrini.chatapp.ui.auth.ForgotPasswordScreen
import com.gayathrini.chatapp.ui.auth.LoginRoute
import com.gayathrini.chatapp.ui.auth.RegisterRoute
import com.gayathrini.chatapp.ui.contacts.ContactsRoute
import com.gayathrini.chatapp.ui.conversation.ConversationRoute
import com.gayathrini.chatapp.ui.conversations.ArchivedConversationsRoute
import com.gayathrini.chatapp.ui.conversations.ConversationsRoute
import com.gayathrini.chatapp.ui.group.CreateGroupRoute
import com.gayathrini.chatapp.ui.group.GroupInfoRoute
import com.gayathrini.chatapp.ui.media.MediaGalleryRoute
import com.gayathrini.chatapp.ui.launch.LaunchRoute
import com.gayathrini.chatapp.ui.profile.ContactProfileRoute
import com.gayathrini.chatapp.ui.profile.ProfileRoute
import com.gayathrini.chatapp.ui.search.SearchRoute
import com.gayathrini.chatapp.ui.starred.StarredMessagesRoute

private const val NEW_CHAT_PEER_ID = "new_chat_peer_id"

/**
 * The single-activity navigation graph (TDD §4.4, §8.2). Conversations is home; the new-chat
 * picker returns the chosen peer via the back-stack saved state.
 */
@Composable
fun ChatNavHost(
    navController: NavHostController = rememberNavController(),
    deepLinkConversationId: String? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
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
                onForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onLoggedIn = {
                    navController.navigate(Screen.Conversations.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
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

            // Deep-link from a tapped message notification (TDD §6.6). Consumed from the
            // authenticated home, so it can't bypass the auth gate.
            LaunchedEffect(deepLinkConversationId) {
                if (deepLinkConversationId != null) {
                    navController.navigate(Screen.Conversation.route(deepLinkConversationId))
                    onDeepLinkHandled()
                }
            }

            ConversationsRoute(
                onOpenConversation = { id -> navController.navigate(Screen.Conversation.route(id)) },
                onNewChat = { navController.navigate(Screen.ContactPicker.route) },
                onNewGroup = { navController.navigate(Screen.CreateGroup.route) },
                onOpenContacts = { navController.navigate(Screen.Contacts.route) },
                onOpenProfile = { navController.navigate(Screen.Profile.route) },
                onOpenSearch = { navController.navigate(Screen.Search.route) },
                onOpenArchived = { navController.navigate(Screen.Archived.route) },
                onOpenStarred = { navController.navigate(Screen.Starred.route) },
                newChatPeerId = newChatPeerId,
                onNewChatHandled = { entry.savedStateHandle[NEW_CHAT_PEER_ID] = null },
            )
        }

        composable(Screen.Archived.route) {
            ArchivedConversationsRoute(
                onBack = { navController.popBackStack() },
                onOpenConversation = { id ->
                    navController.navigate(Screen.Conversation.route(id)) {
                        popUpTo(Screen.Conversations.route)
                    }
                },
            )
        }

        composable(Screen.Starred.route) {
            StarredMessagesRoute(
                onBack = { navController.popBackStack() },
                onOpenConversation = { id ->
                    navController.navigate(Screen.Conversation.route(id)) {
                        popUpTo(Screen.Conversations.route)
                    }
                },
            )
        }

        composable(Screen.Search.route) {
            SearchRoute(
                onBack = { navController.popBackStack() },
                onOpenConversation = { id ->
                    navController.navigate(Screen.Conversation.route(id)) {
                        // Land back on the chat list after viewing a result, not the search screen.
                        popUpTo(Screen.Conversations.route)
                    }
                },
            )
        }

        composable(Screen.CreateGroup.route) {
            CreateGroupRoute(
                onBack = { navController.popBackStack() },
                onGroupCreated = { conversationId ->
                    navController.navigate(Screen.Conversation.route(conversationId)) {
                        popUpTo(Screen.Conversations.route)
                    }
                },
            )
        }

        composable(
            route = Screen.GroupInfo.route,
            arguments = listOf(navArgument(Screen.GroupInfo.ARG) { type = NavType.StringType }),
        ) { entry ->
            val conversationId = entry.arguments?.getString(Screen.GroupInfo.ARG).orEmpty()
            GroupInfoRoute(
                onBack = { navController.popBackStack() },
                onOpenMedia = { navController.navigate(Screen.MediaGallery.route(conversationId)) },
            )
        }

        composable(
            route = Screen.MediaGallery.route,
            arguments = listOf(navArgument(Screen.MediaGallery.ARG) { type = NavType.StringType }),
        ) {
            MediaGalleryRoute(onBack = { navController.popBackStack() })
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
            ContactsRoute(
                onBack = { navController.popBackStack() },
                onOpenProfile = { userId -> navController.navigate(Screen.ContactProfile.route(userId)) },
            )
        }

        composable(
            route = Screen.ContactProfile.route,
            arguments = listOf(navArgument(Screen.ContactProfile.ARG) { type = NavType.StringType }),
        ) {
            ContactProfileRoute(
                onBack = { navController.popBackStack() },
                onOpenConversation = { conversationId ->
                    navController.navigate(Screen.Conversation.route(conversationId)) {
                        // Land back on the chat list (not the profile/contacts) after messaging.
                        popUpTo(Screen.Conversations.route)
                    }
                },
            )
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
        ) { entry ->
            val conversationId = entry.arguments?.getString(Screen.Conversation.ARG).orEmpty()
            ConversationRoute(
                onBack = { navController.popBackStack() },
                onOpenGroupInfo = { navController.navigate(Screen.GroupInfo.route(conversationId)) },
                onOpenMedia = { navController.navigate(Screen.MediaGallery.route(conversationId)) },
            )
        }
    }
}
