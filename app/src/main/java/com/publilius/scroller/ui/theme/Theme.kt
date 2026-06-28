package com.publilius.scroller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlackedColors = darkColorScheme(
    primary = Color(0xFF424242),
    onPrimary = Color.White,
    secondary = Color(0xFF212121),
    onSecondary = Color.White,
    tertiary = Color(0xFF424242),
    background = Color(0xFF000000),
    surface = Color(0xFF212121),
    onSurface = Color.White,
    outline = Color(0xFF424242),
    surfaceVariant = Color(0xFF212121),
    onSurfaceVariant = Color.White.copy(alpha = 0.72f),
)

@Composable
fun ScrollerTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BlackedColors,
        content = content,
    )
}
