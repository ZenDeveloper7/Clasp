package com.zen.clasp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ClaspInkDark,
    onPrimary = ClaspCanvasDark,
    secondary = ClaspInkMutedDark,
    onSecondary = ClaspCanvasDark,
    tertiary = ClaspSignalDark,
    onTertiary = ClaspCanvasDark,
    background = ClaspCanvasDark,
    onBackground = ClaspInkDark,
    surface = ClaspSurfaceDark,
    onSurface = ClaspInkDark,
    surfaceVariant = ClaspSurfaceRaisedDark,
    onSurfaceVariant = ClaspInkMutedDark,
    outline = ClaspOutlineDark,
    error = ClaspSignalDark
)

private val LightColorScheme = lightColorScheme(
    primary = ClaspInkLight,
    onPrimary = ClaspSurfaceLight,
    secondary = ClaspInkMutedLight,
    onSecondary = ClaspSurfaceLight,
    tertiary = ClaspSignalLight,
    onTertiary = ClaspSurfaceLight,
    background = ClaspCanvasLight,
    onBackground = ClaspInkLight,
    surface = ClaspSurfaceLight,
    onSurface = ClaspInkLight,
    surfaceVariant = ClaspSurfaceRaisedLight,
    onSurfaceVariant = ClaspInkMutedLight,
    outline = ClaspOutlineLight,
    error = ClaspSignalLight
)

@Composable
fun ClaspTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
