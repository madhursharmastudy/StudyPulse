package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * BackgroundThemeTokens (Part 4D)
 * Holds base near-black color (#0B0C10) and theme-dependent soft glow color.
 */
data class BackgroundThemeTokens(
    val baseColor: Color = Color(0xFF0B0C10),
    val glowColor: Color = Color(0x594A2A0C) // Default: Orange ember glow (#4A2A0C at ~35% alpha)
)

/**
 * AppThemeTokens (TH-2)
 * Central place that defines colors for each theme using named roles.
 */
data class AppThemeTokens(
    val themeName: String,
    val background: Color,
    val cardFill: Color,
    val chipFill: Color,
    val text: Color,
    val secondaryText: Color,
    val accent: Color,
    val textOnAccent: Color,
    val danger: Color
)

val DarkThemeTokens = AppThemeTokens(
    themeName = "Dark",
    background = Color(0xFF0E1218),
    cardFill = Color(0xFF171D26),
    chipFill = Color(0xFF212A36),
    text = Color(0xFFEAF0F7),
    secondaryText = Color(0xFF9AA7B8),
    accent = Color(0xFF8F86FF),
    textOnAccent = Color(0xFF0E1218),
    danger = Color(0xFFF2706E)
)

val LightThemeTokens = AppThemeTokens(
    themeName = "Light",
    background = Color(0xFFF5F6FA),
    cardFill = Color(0xFFEAEDF5),
    chipFill = Color(0xFFDDE2EE),
    text = Color(0xFF161B26),
    secondaryText = Color(0xFF5A6578),
    accent = Color(0xFF5B50E6),
    textOnAccent = Color(0xFFFFFFFF),
    danger = Color(0xFFD64545)
)

val UltraPremiumThemeTokens = AppThemeTokens(
    themeName = "Ultra Premium",
    background = Color(0xFF02050B),
    cardFill = Color.Transparent,
    chipFill = Color(0x14FFFFFF), // white at 8% fill
    text = Color(0xFFFFFFFF),
    secondaryText = Color(0xFFA3B3D1),
    accent = Color(0xFFFFB52E), // amber
    textOnAccent = Color(0xFF02050B),
    danger = Color(0xFFF2706E)
)

fun classicThemeTokens(isBlue: Boolean) = AppThemeTokens(
    themeName = "Classic",
    background = AmoledBlack,
    cardFill = DarkCard,
    chipFill = Color(0xFF1E1E1E),
    text = TextPrimary,
    secondaryText = TextSecondary,
    accent = if (isBlue) WaterAccent else StudyAccent,
    textOnAccent = Color.Black,
    danger = RoseAccent
)

val LocalBackgroundStyle = compositionLocalOf { "Glow" }
val LocalThemeName = compositionLocalOf { "Orange" }
val LocalActiveTheme = compositionLocalOf { "Dark" }
val LocalBackgroundThemeTokens = staticCompositionLocalOf { BackgroundThemeTokens() }
val LocalAppThemeTokens = staticCompositionLocalOf { DarkThemeTokens }

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
    activeTheme: String = "Dark",
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

    val themeTokens = when (activeTheme) {
        "Light" -> LightThemeTokens
        "Ultra Premium" -> UltraPremiumThemeTokens
        "Classic" -> classicThemeTokens(themeName.equals("Blue", ignoreCase = true))
        else -> DarkThemeTokens // Default to Dark
    }

    // TH-14: System bar icons: light icons on Dark and Ultra Premium, dark icons on Light
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                val darkIcons = activeTheme == "Light"
                insetsController.isAppearanceLightStatusBars = darkIcons
                insetsController.isAppearanceLightNavigationBars = darkIcons
            }
        }
    }

    CompositionLocalProvider(
        LocalActiveTheme provides activeTheme,
        LocalAppThemeTokens provides themeTokens,
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

/**
 * ThemedCard (TH-18, TH-29, Addendum B)
 * In Dark & Light: flat fill (card fill), corner radius 20dp, inner padding 16dp, no border, no shadow.
 * In Ultra Premium: NO fill, border or shadow. Content sits directly on background.
 * In Classic: 16dp shape, DarkCard fill, DarkBorder 1.dp border, 14dp padding.
 */
@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    innerPadding: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    val cardModifier = modifier.then(clickModifier).then(tagModifier)

    when (activeTheme) {
        "Classic" -> {
            Card(
                shape = RoundedCornerShape(cornerRadius),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                ),
                modifier = cardModifier
            ) {
                Column(modifier = Modifier.padding(innerPadding)) {
                    content()
                }
            }
        }
        "Ultra Premium" -> {
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = null,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = cardModifier
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
        else -> {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = tokens.cardFill),
                border = null,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = cardModifier
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }
}

/**
 * ThemedActionPill (TH-20, TH-33)
 * Plain-text actions become pill-shaped tonal buttons: chip fill, no border, height 40dp, horizontal padding 16dp.
 * Filled actions (Done, Resume, START) become accent-filled pills with text on accent.
 * In Classic: uses standard button/text button.
 */
@Composable
fun ThemedActionPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
    isFilled: Boolean = false,
    testTag: String? = null
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier

    if (activeTheme == "Classic") {
        if (isFilled) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudyAccent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = modifier.then(tagModifier)
            ) {
                Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            TextButton(
                onClick = onClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = textColor ?: TextSecondary
                ),
                modifier = modifier.then(tagModifier)
            ) {
                Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    } else {
        val fill = if (isFilled) tokens.accent else tokens.chipFill
        val txtColor = if (isFilled) tokens.textOnAccent else (textColor ?: tokens.text)

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = fill,
            modifier = modifier
                .height(40.dp)
                .then(tagModifier)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = txtColor
                )
            }
        }
    }
}

/**
 * ThemedOptionChip (TH-19, TH-33)
 * In Dark & Light: no border, chip fill, corner radius 14dp. Selected = accent fill with text on accent.
 * In Ultra Premium: white at 8% fill, no border, corner radius 14dp. Selected = accent fill with text on accent.
 * In Classic: unchanged (existing RoundedCornerShape(8.dp) with DarkCard/CharcoalElevated).
 */
@Composable
fun ThemedOptionChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier

    if (activeTheme == "Classic") {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) StudyAccent else Color(0xFF1E1E1E),
            modifier = modifier.then(tagModifier)
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.Black else TextPrimary,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    } else {
        val bg = if (isSelected) tokens.accent else tokens.chipFill
        val fg = if (isSelected) tokens.textOnAccent else tokens.text

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            color = bg,
            modifier = modifier.then(tagModifier)
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = fg,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

