package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimaryLight,
    onPrimary = Color(0xFF003554),
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = Color(0xFFCBE6FF),
    secondary = AmberSecondaryLight,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = EmeraldTertiaryLight,
    onTertiary = Color(0xFF003823),
    background = SlateDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = SlateDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SlateDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = SlateDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.White,
    primaryContainer = CyanContainer,
    onPrimaryContainer = Color(0xFF001E30),
    secondary = AmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = Color(0xFF2E1500),
    tertiary = EmeraldTertiary,
    onTertiary = Color.White,
    background = SlateLightBg,
    onBackground = Color(0xFF0F172A),
    surface = SlateLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SlateLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = SlateLightBorder
)

@Composable
fun AbfsTheme(
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
