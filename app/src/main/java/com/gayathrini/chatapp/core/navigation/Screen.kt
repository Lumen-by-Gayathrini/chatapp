package com.gayathrini.chatapp.core.navigation

/** Centralized navigation routes (TDD §8.2). Feature destinations are registered in [ChatNavHost]. */
sealed class Screen(val route: String) {
    data object Launch : Screen("launch")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Conversations : Screen("conversations")
    data object Contacts : Screen("contacts")
    data object ContactPicker : Screen("contact_picker")
    data object Conversation : Screen("conversation/{conversationId}") {
        const val ARG = "conversationId"
        fun route(conversationId: String) = "conversation/$conversationId"
    }
    data object Profile : Screen("profile")
}
