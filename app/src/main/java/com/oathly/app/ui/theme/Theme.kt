package com.oathly.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.oathly.app.data.ThemeMode

// Navy and warm gold: calm, official-feeling, but clearly not a government brand.
private val Navy = Color(0xFF1F3A5F)
private val NavyLight = Color(0xFFA9C7F0)
private val Gold = Color(0xFFB7862B)
private val GoldLight = Color(0xFFF1C46B)

val Success = Color(0xFF1E7D4F)
val SuccessDark = Color(0xFF6FD39E)
val Danger = Color(0xFFB3261E)
val DangerDark = Color(0xFFF2B8B5)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3F7),
    onPrimaryContainer = Color(0xFF0A1D33),
    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBE7C0),
    onSecondaryContainer = Color(0xFF3B2A06),
    background = Color(0xFFF8F6F1),
    onBackground = Color(0xFF1B1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1C1E),
    surfaceVariant = Color(0xFFE9E6DF),
    onSurfaceVariant = Color(0xFF47464A),
    outline = Color(0xFF79767D),
)

private val DarkColors = darkColorScheme(
    primary = NavyLight,
    onPrimary = Color(0xFF0A1D33),
    primaryContainer = Color(0xFF223B5C),
    onPrimaryContainer = Color(0xFFD6E3F7),
    secondary = GoldLight,
    onSecondary = Color(0xFF3B2A06),
    secondaryContainer = Color(0xFF5A4313),
    onSecondaryContainer = Color(0xFFFBE7C0),
    background = Color(0xFF10151C),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF161C24),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF242B35),
    onSurfaceVariant = Color(0xFFC6C5CC),
    outline = Color(0xFF8F9099),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Big, readable text for the question itself. */
val QuestionStyle = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium)

@Composable
fun OathlyTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

@Composable
fun successColor(): Color = if (MaterialTheme.colorScheme.background.luminanceLow()) SuccessDark else Success

@Composable
fun dangerColor(): Color = if (MaterialTheme.colorScheme.background.luminanceLow()) DangerDark else Danger

private fun Color.luminanceLow(): Boolean = (red * 0.299f + green * 0.587f + blue * 0.114f) < 0.5f
