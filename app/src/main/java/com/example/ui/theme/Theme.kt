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
    val danger: Color,
    val divider: Color = chipFill,
    val track: Color = chipFill
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
    danger = Color(0xFFF2706E),
    divider = Color(0xFF212A36),
    track = Color(0xFF212A36)
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
    danger = Color(0xFFD64545),
    divider = Color(0xFFDDE2EE),
    track = Color(0xFFDDE2EE)
)

val UltraPremiumThemeTokens = AppThemeTokens(
    themeName = "Ultra Premium",
    background = Color(0xFF02050B),
    cardFill = UltraPremiumGlass,
    chipFill = Color(0x14FFFFFF), // white at 8% fill
    text = Color(0xFFFFFFFF),
    secondaryText = Color(0xFFA3B3D1),
    accent = Color(0xFFFFB52E), // amber
    textOnAccent = Color(0xFF02050B),
    danger = Color(0xFFF2706E),
    divider = Color(0x1FFFFFFF),
    track = Color(0x14FFFFFF)
)

val DarkBlackThemeTokens = AppThemeTokens(
    themeName = "Dark Black",
    background = AmoledBlack,
    cardFill = DarkCard,
    chipFill = Color(0xFF1E1E1E),
    text = TextPrimary,
    secondaryText = TextSecondary,
    accent = StudyAccent,
    textOnAccent = Color.Black,
    danger = RoseAccent,
    divider = DarkBorder,
    track = DarkBorder
)

val GlowingBlueThemeTokens = AppThemeTokens(
    themeName = "Glowing Blue",
    background = Color(0xFF0B0C10),
    cardFill = DarkCard,
    chipFill = Color(0xFF1E1E1E),
    text = TextPrimary,
    secondaryText = TextSecondary,
    accent = WaterAccent,
    textOnAccent = Color.Black,
    danger = RoseAccent,
    divider = DarkBorder,
    track = DarkBorder
)

val GlowingGoldThemeTokens = AppThemeTokens(
    themeName = "Glowing Gold",
    background = Color(0xFF0B0C10),
    cardFill = DarkCard,
    chipFill = Color(0xFF1E1E1E),
    text = TextPrimary,
    secondaryText = TextSecondary,
    accent = StudyAccent,
    textOnAccent = Color.Black,
    danger = RoseAccent,
    divider = DarkBorder,
    track = DarkBorder
)

fun isExistingTheme(theme: String): Boolean =
    theme in listOf("Dark Black", "Glowing Blue", "Glowing Gold", "Classic")

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
    val backgroundTokens = if (activeTheme == "Glowing Blue" || themeName.equals("Blue", ignoreCase = true)) {
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
        "Dark Black" -> DarkBlackThemeTokens
        "Glowing Blue" -> GlowingBlueThemeTokens
        "Glowing Gold" -> GlowingGoldThemeTokens
        "Classic" -> DarkBlackThemeTokens
        else -> DarkThemeTokens // Default to Dark
    }

    val colorScheme = if (isExistingTheme(activeTheme)) {
        StudyWatchDarkColorScheme
    } else if (activeTheme == "Light") {
        lightColorScheme(
            primary = themeTokens.accent,
            onPrimary = themeTokens.textOnAccent,
            background = themeTokens.background,
            onBackground = themeTokens.text,
            surface = themeTokens.cardFill,
            onSurface = themeTokens.text,
            surfaceVariant = themeTokens.chipFill,
            onSurfaceVariant = themeTokens.secondaryText,
            outline = themeTokens.divider,
            error = themeTokens.danger
        )
    } else {
        darkColorScheme(
            primary = themeTokens.accent,
            onPrimary = themeTokens.textOnAccent,
            background = themeTokens.background,
            onBackground = themeTokens.text,
            surface = themeTokens.cardFill,
            onSurface = themeTokens.text,
            surfaceVariant = themeTokens.chipFill,
            onSurfaceVariant = themeTokens.secondaryText,
            outline = themeTokens.divider,
            error = themeTokens.danger
        )
    }

    // F9: System bar icons: light icons on every theme except Light, no scrim
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                val darkIcons = activeTheme == "Light"
                insetsController.isAppearanceLightStatusBars = darkIcons
                insetsController.isAppearanceLightNavigationBars = darkIcons
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }
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
 * ThemedCard (C1, M4)
 * In Dark & Light: flat fill (card fill), corner radius 20dp, inner padding 16dp, no border, no shadow.
 * In Ultra Premium: white at 6% fill, corner radius 24dp, inner padding 16dp, no border, no shadow.
 * In Existing themes: keeps current card look (cornerRadius, DarkCard fill, DarkBorder 1.dp border, 14dp padding).
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

    if (isExistingTheme(activeTheme)) {
        Card(
            shape = RoundedCornerShape(cornerRadius),
            colors = CardDefaults.cardColors(containerColor = tokens.cardFill),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(tokens.divider)
            ),
            modifier = cardModifier
        ) {
            Column(modifier = Modifier.padding(innerPadding)) {
                content()
            }
        }
    } else if (activeTheme == "Ultra Premium") {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = UltraPremiumGlass),
            border = null,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = cardModifier
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    } else {
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

/**
 * ThemedActionPill (C3)
 * Plain-text actions become pill-shaped tonal buttons: chip fill, no border, height 40dp, horizontal padding 16dp.
 * Filled actions (Done, Resume, START) become accent-filled pills with text on accent.
 * In Existing themes: keeps current button shape.
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

    if (isExistingTheme(activeTheme)) {
        if (isFilled) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.accent,
                    contentColor = tokens.textOnAccent
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
                    contentColor = textColor ?: tokens.secondaryText
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
 * ThemedOptionChip (C2)
 * In Dark & Light: no border, chip fill, corner radius 14dp. Selected = accent fill with text on accent.
 * In Ultra Premium: white at 8% fill, no border, corner radius 14dp. Selected = accent fill with text on accent.
 * In Existing themes: unchanged shape (RoundedCornerShape(8.dp) with chipFill / accent).
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

    if (isExistingTheme(activeTheme)) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) tokens.accent else tokens.chipFill,
            modifier = modifier.then(tagModifier)
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) tokens.textOnAccent else tokens.text,
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

