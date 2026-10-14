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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.LocalActiveTheme
import com.example.ui.theme.LocalAppThemeTokens
import com.example.ui.theme.LocalBackgroundStyle
import com.example.ui.theme.LocalBackgroundThemeTokens

/**
 * AppBackground
 * Reusable background composable wrapping every screen and overlay.
 * Handles Classic (Glow / Pure black), Dark (#0E1218), Light (#F5F6FA),
 * and Ultra Premium (native Canvas gradient, curves, thin lines, subtle particles per TH-28).
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    activeTheme: String = LocalActiveTheme.current,
    backgroundStyle: String = LocalBackgroundStyle.current,
    content: @Composable () -> Unit
) {
    when (activeTheme) {
        "Classic" -> {
            if (backgroundStyle == "Pure black") {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(AmoledBlack)
                ) {
                    content()
                }
            } else {
                val tokens = LocalBackgroundThemeTokens.current
                val baseColor = tokens.baseColor
                val glowColor = tokens.glowColor

                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .drawBehind {
                            val isLandscape = size.width > size.height
                            val centerY = if (isLandscape) size.height * 0.50f else size.height * 0.56f
                            val centerX = size.width * 0.50f
                            val glowRadius = maxOf(size.width, size.height) * 0.85f

                            drawRect(color = baseColor)

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
        "Ultra Premium" -> {
            // TH-28: Vertical gradient #02050B to #071426 to #0A1D38, 3-4 soft dark-blue shapes,
            // 2 thin curved light lines (blue ~25%, amber ~25%), and tiny particles ~10% opacity.
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .drawBehind {
                        val vertGradient = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF02050B),
                                Color(0xFF071426),
                                Color(0xFF0A1D38)
                            )
                        )
                        drawRect(brush = vertGradient)

                        // 3 broad soft dark-blue curved shapes
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x33083B77), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.15f),
                                radius = size.width * 0.7f
                            ),
                            radius = size.width * 0.7f,
                            center = Offset(size.width * 0.85f, size.height * 0.15f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x280D47A1), Color.Transparent),
                                center = Offset(size.width * 0.1f, size.height * 0.55f),
                                radius = size.width * 0.65f
                            ),
                            radius = size.width * 0.65f,
                            center = Offset(size.width * 0.1f, size.height * 0.55f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x2E0B2553), Color.Transparent),
                                center = Offset(size.width * 0.75f, size.height * 0.85f),
                                radius = size.width * 0.8f
                            ),
                            radius = size.width * 0.8f,
                            center = Offset(size.width * 0.75f, size.height * 0.85f)
                        )

                        // 2 thin curved light lines: blue #087CFF at ~25% and amber #FF8A24 at ~25%
                        val bluePath = Path().apply {
                            moveTo(0f, size.height * 0.35f)
                            cubicTo(
                                size.width * 0.35f, size.height * 0.30f,
                                size.width * 0.65f, size.height * 0.45f,
                                size.width, size.height * 0.38f
                            )
                        }
                        drawPath(
                            path = bluePath,
                            color = Color(0x40087CFF),
                            style = Stroke(width = 1.5f)
                        )

                        val amberPath = Path().apply {
                            moveTo(0f, size.height * 0.68f)
                            cubicTo(
                                size.width * 0.3f, size.height * 0.75f,
                                size.width * 0.7f, size.height * 0.60f,
                                size.width, size.height * 0.72f
                            )
                        }
                        drawPath(
                            path = amberPath,
                            color = Color(0x40FF8A24),
                            style = Stroke(width = 1.5f)
                        )

                        // Tiny particles at ~10% opacity
                        val particleColor = Color(0x1AFFFFFF)
                        val particles = listOf(
                            Offset(size.width * 0.22f, size.height * 0.18f) to 2.5f,
                            Offset(size.width * 0.78f, size.height * 0.28f) to 1.8f,
                            Offset(size.width * 0.45f, size.height * 0.42f) to 2.2f,
                            Offset(size.width * 0.15f, size.height * 0.72f) to 2.0f,
                            Offset(size.width * 0.82f, size.height * 0.64f) to 2.4f,
                            Offset(size.width * 0.60f, size.height * 0.88f) to 1.5f
                        )
                        for ((pos, r) in particles) {
                            drawCircle(color = particleColor, radius = r, center = pos)
                        }
                    }
            ) {
                content()
            }
        }
        else -> {
            val tokens = LocalAppThemeTokens.current
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(tokens.background)
            ) {
                content()
            }
        }
    }
}
