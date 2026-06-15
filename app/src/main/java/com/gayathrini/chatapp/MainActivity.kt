package com.gayathrini.chatapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gayathrini.chatapp.core.designsystem.theme.ChatAppTheme
import com.gayathrini.chatapp.core.navigation.ChatNavHost
import com.gayathrini.chatapp.core.notifications.MessageNotifier
import com.gayathrini.chatapp.data.presence.PresenceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The single host Activity. It hosts the Compose [ChatNavHost]; all screens are destinations
 * within it (TDD §4.4).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var presenceRepository: PresenceRepository

    /** Deep-link target from a tapped message notification (TDD §6.6); consumed by the nav host. */
    private val deepLinkConversationId = mutableStateOf<String?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        deepLinkConversationId.value = intent.conversationIdExtra()
        requestNotificationPermissionIfNeeded()

        // App-level presence heartbeat (TDD §6.5): runs only while the activity is foregrounded
        // (STARTED), independent of which screen is shown, and auto-cancels when backgrounded.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    presenceRepository.heartbeat()
                    delay(PRESENCE_HEARTBEAT_MS)
                }
            }
        }

        setContent {
            ChatAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ChatNavHost(
                        deepLinkConversationId = deepLinkConversationId.value,
                        onDeepLinkHandled = { deepLinkConversationId.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // singleTop: a notification tap re-delivers here; surface the new deep-link target.
        intent.conversationIdExtra()?.let { deepLinkConversationId.value = it }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun Intent.conversationIdExtra(): String? =
        getStringExtra(MessageNotifier.EXTRA_CONVERSATION_ID)

    private companion object {
        // < the server's 30s online window so a foreground user stays "online" between beats.
        const val PRESENCE_HEARTBEAT_MS = 20_000L
    }
}
