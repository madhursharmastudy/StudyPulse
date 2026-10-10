package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.LocalBackgroundStyle
import com.example.ui.theme.LocalBackgroundThemeTokens

/**
 * AppBackground (Part 4D)
 * Reusable premium dark background composable wrapping every screen and overlay.
 *
 * Features:
 * - "Glow" (default): Near-black base (#0B0C10) with one soft, subtle radial glow
 *   centered slightly below middle (centered in landscape) that fades smoothly to edges.
 *   - Orange theme: warm ember glow (#4A2A0C at ~35% center strength).
 *   - Blue theme: deep navy glow (#1B2A5E at ~40% center strength).
 * - "Pure black": Plain #000000 to maximize battery savings on AMOLED screens.
 * - Performance: Static single radial gradient brush drawn in DrawScope, zero blur, zero lag.
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    backgroundStyle: String = LocalBackgroundStyle.current,
    content: @Composable () -> Unit
) {
    if (backgroundStyle == "Pure black") {
        // Pure black AMOLED mode
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AmoledBlack)
        ) {
            content()
        }
    } else {
        // Premium dark background with theme-based soft radial glow
        val tokens = LocalBackgroundThemeTokens.current
        val baseColor = tokens.baseColor
        val glowColor = tokens.glowColor

        Box(
            modifier = modifier
                .fillMaxSize()
                .drawBehind {
                    val isLandscape = size.width > size.height
                    // Centered slightly below the middle on portrait, centered on landscape
                    val centerY = if (isLandscape) size.height * 0.50f else size.height * 0.56f
                    val centerX = size.width * 0.50f
                    val glowRadius = maxOf(size.width, size.height) * 0.85f

                    // 1. Solid near-black base
                    drawRect(color = baseColor)

                    // 2. Soft radial gradient glow fading smoothly to base color
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to glowColor,
                                0.45f to glowColor.copy(alpha = glowColor.alpha * 0.45f),
                                1.0f to Color.Transparent
                            ),
                            center = Offset(centerX, centerY),
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = Offset(centerX, centerY)
                    )
                }
        ) {
            content()
        }
    }
}
