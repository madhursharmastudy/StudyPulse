package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * BackgroundThemeTokens (Part 4D)
 * Holds base near-black color (#0B0C10) and theme-dependent soft glow color.
 */
data class BackgroundThemeTokens(
    val baseColor: Color = Color(0xFF0B0C10),
    val glowColor: Color = Color(0x594A2A0C) // Default: Orange ember glow (#4A2A0C at ~35% alpha)
)

val LocalBackgroundStyle = compositionLocalOf { "Glow" }
val LocalThemeName = compositionLocalOf { "Orange" }
val LocalBackgroundThemeTokens = staticCompositionLocalOf { BackgroundThemeTokens() }

private val StudyWatchDarkColorScheme = darkColorScheme(
    primary = AmberAccent,
    onPrimary = Color.Black,
    primaryContainer = AmberGlow,
    onPrimaryContainer = Color.White,
    secondary = CyanAccent,
    onSecondary = Color.Black,
    secondaryContainer = CyanSoft,
    onSecondaryContainer = CyanAccent,
    tertiary = EmeraldAccent,
    onTertiary = Color.Black,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = CharcoalCard,
    onSurface = TextPrimary,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = TextSecondary,
    outline = CharcoalBorder,
    error = RoseAccent,
    onError = Color.White
)

private val StudyWatchLightColorScheme = lightColorScheme(
    primary = AmberGlow,
    onPrimary = Color.White,
    secondary = CyanAccent,
    onSecondary = Color.Black,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    outline = Color(0xFFE2E8F0)
)

@Composable
fun StudyWatchTheme(
    isAmoled: Boolean = true,
    themeName: String = "Orange",
    backgroundStyle: String = "Glow",
    content: @Composable () -> Unit
) {
    val colorScheme = if (isAmoled) StudyWatchDarkColorScheme else StudyWatchLightColorScheme

    // Part 4D: Color tokens following the theme:
    // Blue theme: deep navy glow (#1B2A5E at ~40% strength at center)
    // Orange theme: deep warm ember glow (#4A2A0C at ~35% strength at center)
    val backgroundTokens = if (themeName.equals("Blue", ignoreCase = true)) {
        BackgroundThemeTokens(
            baseColor = Color(0xFF0B0C10),
            glowColor = Color(0x661B2A5E) // ~40% strength #1B2A5E
        )
    } else {
        BackgroundThemeTokens(
            baseColor = Color(0xFF0B0C10),
            glowColor = Color(0x594A2A0C) // ~35% strength #4A2A0C
        )
    }

    CompositionLocalProvider(
        LocalBackgroundStyle provides backgroundStyle,
        LocalThemeName provides themeName,
        LocalBackgroundThemeTokens provides backgroundTokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    StudyWatchTheme(isAmoled = darkTheme, content = content)
}

