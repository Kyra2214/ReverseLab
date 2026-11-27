package br.com.reverselab.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ReverseLabColors = darkColorScheme(
    primary = Color(0xFF42D9E8),
    onPrimary = Color(0xFF002F35),
    secondary = Color(0xFFB8A2FF),
    onSecondary = Color(0xFF28145E),
    background = Color(0xFF0B111A),
    onBackground = Color(0xFFE8F0F7),
    surface = Color(0xFF121C28),
    onSurface = Color(0xFFE8F0F7),
    surfaceVariant = Color(0xFF1B2938),
    onSurfaceVariant = Color(0xFFB2C0CE),
)

@Composable
fun ReverseLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ReverseLabColors, content = content)
}
