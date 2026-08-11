package com.channels.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ChannelsColorScheme = darkColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = Graphite,
    onSecondary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Slate,
    outline = Hairline,
    outlineVariant = Hairline,
)

/**
 * The single theme wrapper for the app. Monochrome, black-on-white inverted to match
 * the Light Phone III: white text on a black ground, no accent hue, one variant.
 */
@Composable
fun ChannelsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChannelsColorScheme,
        typography = ChannelsTypography,
        content = content,
    )
}
