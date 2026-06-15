package com.gayathrini.chatapp.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = ErrorColor,
    onError = OnError,
    outline = Outline,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    outline = OutlineDark,
)

/** Chat-specific tones not covered by the Material colour scheme (bubbles, wallpaper, ticks). */
data class ChatColors(
    val wallpaper: Color,
    val outgoingBubble: Color,
    val incomingBubble: Color,
    val readTick: Color,
)

private val LightChatColors = ChatColors(
    wallpaper = ChatWallpaperLight,
    outgoingBubble = OutgoingBubbleLight,
    incomingBubble = IncomingBubbleLight,
    readTick = ReadTick,
)

private val DarkChatColors = ChatColors(
    wallpaper = ChatWallpaperDark,
    outgoingBubble = OutgoingBubbleDark,
    incomingBubble = IncomingBubbleDark,
    readTick = ReadTick,
)

private val LocalChatColors = staticCompositionLocalOf { LightChatColors }

/** Access the chat-specific tones for the active theme. */
object ChatTheme {
    val colors: ChatColors
        @Composable @ReadOnlyComposable get() = LocalChatColors.current
}

/**
 * App-wide Compose theme — a conventional WhatsApp-style messaging look (control build). Fixed
 * brand scheme (no dynamic colour) so the control build is visually stable across the study.
 */
@Composable
fun ChatAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalChatColors provides if (darkTheme) DarkChatColors else LightChatColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = ChatTypography,
            content = content,
        )
    }
}
