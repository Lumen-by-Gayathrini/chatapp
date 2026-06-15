package com.gayathrini.chatapp.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gayathrini.chatapp.MainActivity
import com.gayathrini.chatapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [MessageNotifier] backed by [NotificationManagerCompat] (TDD §6.6). One notification per
 * conversation (stable id), `setNumber(unreadCount)` for the badge, and a tap [PendingIntent]
 * that deep-links into the conversation. Never throws — notifications are best-effort.
 */
@Singleton
class AndroidMessageNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : MessageNotifier {

    @Volatile private var activeConversationId: String? = null

    /** Last message key we alerted for, per conversation — avoids re-buzzing on every poll. */
    private val lastNotifiedKey = ConcurrentHashMap<String, String>()

    init {
        val channel = NotificationChannel(CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH)
            .apply {
                description = "New chat messages"
                setShowBadge(true)
            }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    override fun setActiveConversation(conversationId: String?) {
        activeConversationId = conversationId
    }

    override fun notifyMessage(
        conversationId: String,
        title: String,
        body: String,
        unreadCount: Int,
        messageKey: String,
        alert: Boolean,
    ) {
        if (conversationId == activeConversationId) {
            cancel(conversationId)
            return
        }
        val previousKey = lastNotifiedKey.put(conversationId, messageKey)
        if (!alert) return // baseline seed (first sync) — record the key, show nothing
        if (previousKey == messageKey) return // already alerted for this exact message

        val notifier = NotificationManagerCompat.from(context)
        if (!notifier.areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MessageNotifier.EXTRA_CONVERSATION_ID, conversationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_message)
            .setContentTitle(title)
            .setContentText(body)
            .setNumber(unreadCount)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notifier.notify(conversationId.hashCode(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted (Android 13+); silently skip.
        }
    }

    override fun cancel(conversationId: String) {
        lastNotifiedKey.remove(conversationId)
        NotificationManagerCompat.from(context).cancel(conversationId.hashCode())
    }

    private companion object {
        const val CHANNEL_ID = "messages"
    }
}
