package com.gayathrini.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.gayathrini.chatapp.core.designsystem.theme.ChatAppTheme
import com.gayathrini.chatapp.core.navigation.ChatNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single host Activity. It hosts the Compose [ChatNavHost]; all screens are destinations
 * within it (TDD §4.4).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ChatAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ChatNavHost()
                }
            }
        }
    }
}
