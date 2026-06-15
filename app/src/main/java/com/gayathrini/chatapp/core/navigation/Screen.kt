package com.gayathrini.chatapp.core.navigation

/** Centralized navigation routes (TDD §8.2). Feature destinations are registered in [ChatNavHost]. */
sealed class Screen(val route: String) {
    data object Launch : Screen("launch")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object Conversations : Screen("conversations")
    data object Contacts : Screen("contacts")
    data object ContactPicker : Screen("contact_picker")
    data object ContactProfile : Screen("contact_profile/{userId}") {
        const val ARG = "userId"
        fun route(userId: String) = "contact_profile/$userId"
    }
    data object Conversation : Screen("conversation/{conversationId}") {
        const val ARG = "conversationId"
        fun route(conversationId: String) = "conversation/$conversationId"
    }
    data object Profile : Screen("profile")
    data object Search : Screen("search")
    data object Archived : Screen("archived")
    data object Starred : Screen("starred")
    data object CreateGroup : Screen("create_group")
    data object GroupInfo : Screen("group_info/{conversationId}") {
        const val ARG = "conversationId"
        fun route(conversationId: String) = "group_info/$conversationId"
    }
    data object MediaGallery : Screen("media/{conversationId}") {
        const val ARG = "conversationId"
        fun route(conversationId: String) = "media/$conversationId"
    }
}
