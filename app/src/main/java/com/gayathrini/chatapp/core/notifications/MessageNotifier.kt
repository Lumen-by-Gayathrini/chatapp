package com.gayathrini.chatapp.core.notifications

/**
 * Posts local, foreground/polling-driven message notifications (TDD §6.6). No FCM / background
 * push. Abstracted behind an interface so the sync layer stays Android-free and unit-testable.
 */
interface MessageNotifier {
    /** The conversation currently on screen; notifications for it are suppressed and cancelled. */
    fun setActiveConversation(conversationId: String?)

    /**
     * Post/refresh a notification for a conversation's unread incoming message.
     *
     * @param messageKey changes when a newer message arrives, so the notifier only re-alerts for
     *   genuinely new messages (not on every poll).
     * @param alert when false, only seed the baseline key (no banner) — used for the first sync so
     *   pre-existing unread threads don't spam notifications on app open.
     */
    fun notifyMessage(
        conversationId: String,
        title: String,
        body: String,
        unreadCount: Int,
        messageKey: String,
        alert: Boolean = true,
    )

    /** Cancel a conversation's notification (it was opened or read). */
    fun cancel(conversationId: String)

    companion object {
        /** Intent extra carrying the deep-link target conversation id (TDD §6.6). */
        const val EXTRA_CONVERSATION_ID = "com.gayathrini.chatapp.CONVERSATION_ID"
    }
}
