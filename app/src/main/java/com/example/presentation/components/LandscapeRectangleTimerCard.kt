package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Shared HH:MM:SS time display composable used for ALL timers (Part 5):
 * Home study timer, Session, Pomodoro, Countdown, Stopwatch, Break.
 *
 * Hours:Minutes are BIG and bold, seconds are SMALL and dim placed after them.
 * Exception: on Stopwatch the seconds are larger (about 55% of the big digits).
 */
@Composable
fun TimerDigitsDisplay(
    hoursMinutes: String,
    seconds: String,
    isStopwatch: Boolean = false,
    fontSize: TextUnit = 52.sp,
    textColor: Color = Color.White,
    isHomeStyle: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isHomeStyle) {
        // Landscape Home style: vertically stretched tall digits, seconds at bottom-right just below the last digit's bottom line
        Box(
            modifier = modifier.wrapContentSize(),
            contentAlignment = Alignment.Center
        ) {
            // Hours and Minutes: Stretched vertically (scaleY = 1.38f) target ~470px height (48% card height)
            Text(
                text = hoursMinutes,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-1).sp,
                color = textColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.graphicsLayer(scaleY = 1.38f)
            )

            // Seconds: positioned at bottom-right of big digits, just below last digit's bottom line
            if (seconds.isNotEmpty()) {
                val secondsFontSize = (fontSize.value * 0.14f).coerceIn(16f, 24f).sp
                Text(
                    text = seconds,
                    fontSize = secondsFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = textColor.copy(alpha = 0.55f),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(
                            x = (fontSize.value * 0.10f).dp,
                            y = (fontSize.value * 0.18f).dp
                        )
                )
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = modifier
        ) {
            // Hours and Minutes: BIG and bold
            Text(
                text = hoursMinutes,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-1).sp,
                color = textColor,
                maxLines = 1,
                softWrap = false
            )
            // Seconds: SMALL, dim placed after them (larger on Stopwatch)
            if (seconds.isNotEmpty()) {
                val secondsFontSize = if (isStopwatch) {
                    (fontSize.value * 0.55f).sp
                } else {
                    (fontSize.value * 0.38f).coerceAtLeast(14f).sp
                }
                val secondsColor = if (isStopwatch) CyanAccent else textColor.copy(alpha = 0.55f)
                val bottomPadding = (fontSize.value * 0.12f).dp
                Text(
                    text = seconds,
                    fontSize = secondsFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = secondsColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = bottomPadding)
                )
            }
        }
    }
}

/**
 * Normalizes any formatted timer string (HH:MM:SS, MM:SS, M:SS) into HH:MM and :SS
 * e.g., "05:00" -> ("00:05", ":00")
 *       "50:00" -> ("00:50", ":00")
 *       "00:49:34" -> ("00:49", ":34")
 */
fun formatTimeToHHMMSS(timeStr: String): Pair<String, String> {
    val clean = timeStr.trim()
    val parts = clean.split(":")
    return when (parts.size) {
        3 -> "${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}" to ":${parts[2].padStart(2, '0')}"
        2 -> "00:${parts[0].padStart(2, '0')}" to ":${parts[1].padStart(2, '0')}"
        1 -> "00:00" to ":${parts[0].padStart(2, '0')}"
        else -> "00:00" to ":00"
    }
}

/**
 * LandscapeRectangleTimerCard (Part 5)
 * Reusable landscape card used on Home and Timers tabs.
 *
 * Rules:
 * - NO fill color and NO glow behind it (completely transparent).
 * - Thick border (5-6dp) with a faint track. Fixed timers fill clockwise from top center.
 * - Stopwatch has NO progress: plain thin faint border.
 * - Bright white digits filling about 90% of card width.
 * - Rounded corners about 3dp.
 */
@Composable
fun LandscapeRectangleTimerCard(
    modeLabel: String,
    timeHoursMinutes: String,
    timeSeconds: String,
    hasProgress: Boolean,
    progressFraction: Float,
    isActive: Boolean,
    isRunning: Boolean,
    isPaused: Boolean,
    isQuietMode: Boolean = false,
    isStopwatch: Boolean = false,
    showPlayButton: Boolean = true,
    showSetTimer: Boolean = true,
    endSessionLabel: String = "End Session",
    accentColor: Color = StudyAccent,
    useHomeStyle: Boolean = false,
    onPlayPause: () -> Unit,
    onEndSession: () -> Unit,
    onReset: () -> Unit,
    onQuietModeToggle: () -> Unit = {},
    onSetTimer: () -> Unit = {},
    onLap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isClassic = isExistingTheme(activeTheme)
    val isUltra = activeTheme == "Ultra Premium"

    val activeAccent = if (isClassic) accentColor else tokens.accent
    val cornerRadius = if (isClassic) (if (useHomeStyle) 24.dp else 3.dp) else (if (isUltra) 24.dp else 20.dp)
    val strokeWidthDp = if (hasProgress) (if (useHomeStyle) 3.5.dp else 5.5.dp) else 1.5.dp

    val cardBgModifier = when {
        isClassic -> if (useHomeStyle) {
            Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF22140A), Color(0xFF130B05), Color(0xFF0C0703))
                    )
                )
                .drawBehind {
                    // Soft orange glow at the bottom of the card
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1.0f to Color(0x35FFA640)
                        ),
                        size = size,
                        cornerRadius = CornerRadius(24.dp.toPx())
                    )
                }
        } else Modifier
        isUltra -> if (useHomeStyle) Modifier.background(UltraPremiumGlass) else Modifier
        else -> if (useHomeStyle) Modifier.background(tokens.cardFill) else Modifier
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .then(cardBgModifier)
            .drawBehind {
                val strokePx = strokeWidthDp.toPx()
                val halfStroke = strokePx / 2f
                val r = cornerRadius.toPx()

                val left = halfStroke
                val top = halfStroke
                val right = size.width - halfStroke
                val bottom = size.height - halfStroke
                val w = right - left
                val h = bottom - top

                // Clockwise rounded rectangle path starting at top center
                val basePath = Path().apply {
                    moveTo(left + w / 2f, top)
                    lineTo(right - r, top)
                    quadraticTo(right, top, right, top + r)
                    lineTo(right, bottom - r)
                    quadraticTo(right, bottom, right - r, bottom)
                    lineTo(left + r, bottom)
                    quadraticTo(left, bottom, left, bottom - r)
                    lineTo(left, top + r)
                    quadraticTo(left, top, left + r, top)
                    lineTo(left + w / 2f, top)
                    close()
                }

                // 1. Border track along card border
                val trackColor = when {
                    isClassic -> if (useHomeStyle) Color(0x44FFA640) else Color(0xFF262626)
                    isUltra -> Color(0x1AFFFFFF)
                    else -> tokens.track
                }
                drawPath(
                    path = basePath,
                    color = trackColor,
                    style = Stroke(width = strokePx)
                )

                // 2. Clockwise progress line for fixed-time timers only
                if (hasProgress) {
                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(basePath, true)
                    val totalLen = pathMeasure.length
                    val progressLen = (progressFraction.coerceIn(0f, 1f)) * totalLen

                    if (progressLen > 0f) {
                        val progressPath = Path()
                        pathMeasure.getSegment(0f, progressLen, progressPath, true)

                        val drawColor = if (isPaused) {
                            activeAccent.copy(alpha = 0.5f)
                        } else {
                            activeAccent
                        }

                        drawPath(
                            path = progressPath,
                            color = drawColor,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }
            }
            .padding(
                start = if (useHomeStyle) 16.dp else 12.dp,
                end = if (useHomeStyle) 16.dp else 12.dp,
                top = if (useHomeStyle) 10.dp else 8.dp,
                bottom = if (useHomeStyle) 14.dp else 12.dp
            )
    ) {
        // Auto-scale big font size:
        // Landscape Home Style: 1.23x larger digits (target 78% card width ~1200px, 48% card height ~470px)
        val homeTargetFontSize = minOf(
            (maxWidth.value * 0.78f) / 2.55f,
            (maxHeight.value * 0.48f) / 0.98f
        ).coerceIn(48f, 220f).sp

        val innerWidth = (maxWidth.value - 24f).coerceAtLeast(100f)
        val innerHeight = (maxHeight.value - (if (useHomeStyle) 76f else 22f)).coerceAtLeast(100f)
        val widthBasedSize = (innerWidth * 0.90f) / 2.9f
        val maxSafeWidthSize = (innerWidth * 0.97f) / (if (isStopwatch) 3.9f else 3.56f)
        val heightBasedSize = (innerHeight * 0.58f) / 1.15f
        val defaultFontSize = minOf(widthBasedSize, maxSafeWidthSize, heightBasedSize).coerceIn(48f, 150f).sp
        val dynamicFontSize = if (useHomeStyle) homeTargetFontSize else defaultFontSize

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Mode Label on Top + small orange underline if Home screen
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = if (useHomeStyle) 4.dp else 2.dp)
            ) {
                Text(
                    text = modeLabel,
                    fontSize = if (useHomeStyle) 12.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = if (useHomeStyle) 2.sp else 1.5.sp,
                    color = if (isPaused) (if (isClassic) TextMuted else tokens.secondaryText) else activeAccent,
                    maxLines = 1
                )
                if (useHomeStyle) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(activeAccent)
                    )
                }
            }

            // 2. Large Digits: vertically centered between mode label and play button
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                TimerDigitsDisplay(
                    hoursMinutes = timeHoursMinutes,
                    seconds = timeSeconds,
                    isStopwatch = isStopwatch,
                    fontSize = dynamicFontSize,
                    textColor = if (isClassic) Color.White else tokens.text,
                    isHomeStyle = useHomeStyle
                )
            }

            if (useHomeStyle) {
                // Home Screen: Pause button, End Session, Reset and Quiet Mode stay in ONE line, centered, equal gaps
                // Real buttons (outlined style, rounded corners, proper padding), at least 12dp bottom padding
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showPlayButton) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) Color(0x33FFA640) else accentColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onPlayPause
                                )
                                .testTag("landscape_timer_play_pause"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "Pause" else "Start",
                                tint = if (isRunning) accentColor else Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (!isActive) {
                        if (isClassic) {
                            OutlinedButton(
                                onClick = onSetTimer,
                                border = BorderStroke(1.2.dp, StudyAccent.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = StudyAccent
                                ),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("landscape_set_timer")
                            ) {
                                Text("Set Timer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            ThemedActionPill(
                                text = "Set Timer",
                                onClick = onSetTimer,
                                textColor = tokens.accent,
                                testTag = "landscape_set_timer"
                            )
                        }
                    } else {
                        if (isClassic) {
                            OutlinedButton(
                                onClick = onEndSession,
                                border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0x18FFFFFF),
                                    contentColor = TextSecondary
                                ),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("landscape_end_session")
                            ) {
                                Text("End Session", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }

                            OutlinedButton(
                                onClick = onReset,
                                border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0x12FFFFFF),
                                    contentColor = TextMuted
                                ),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("landscape_reset")
                            ) {
                                Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }

                            if (isRunning) {
                                OutlinedButton(
                                    onClick = onQuietModeToggle,
                                    border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.75f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0x22FFA640),
                                        contentColor = AmberAccent
                                    ),
                                    modifier = Modifier
                                    .height(38.dp)
                                    .testTag("landscape_quiet_mode")
                                ) {
                                    Text("Quiet Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (isQuietMode) {
                                OutlinedButton(
                                    onClick = onQuietModeToggle,
                                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.75f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0x2200D675),
                                        contentColor = EmeraldAccent
                                    ),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("landscape_resume")
                                ) {
                                    Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            ThemedActionPill(
                                text = "End Session",
                                onClick = onEndSession,
                                testTag = "landscape_end_session"
                            )
                            ThemedActionPill(
                                text = "Reset",
                                onClick = onReset,
                                testTag = "landscape_reset"
                            )
                            if (isRunning) {
                                ThemedActionPill(
                                    text = "Quiet Mode",
                                    onClick = onQuietModeToggle,
                                    textColor = AmberAccent,
                                    testTag = "landscape_quiet_mode"
                                )
                            } else if (isQuietMode) {
                                ThemedActionPill(
                                    text = "Resume",
                                    onClick = onQuietModeToggle,
                                    isFilled = true,
                                    testTag = "landscape_resume"
                                )
                            }
                        }
                    }
                }
            } else {
                // Default style for Timers tabs
                // 3. Play / Pause Button below digits
                if (showPlayButton) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) Color(0x33FFA640) else accentColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onPlayPause
                            )
                            .testTag("landscape_timer_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start",
                            tint = if (isRunning) accentColor else Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Actions Row at the bottom: 1 row, centered, 12dp equal gap, height 32-40dp, fully inside card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isStopwatch) {
                        if (isActive) {
                            if (isClassic) {
                                OutlinedButton(
                                    onClick = onEndSession,
                                    border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0x18FFFFFF),
                                        contentColor = TextSecondary
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("landscape_sw_end")
                                ) {
                                    Text("End Session", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                OutlinedButton(
                                    onClick = onReset,
                                    border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0x12FFFFFF),
                                        contentColor = TextMuted
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("landscape_sw_reset")
                                ) {
                                    Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                if (isActive) {
                                    OutlinedButton(
                                        onClick = onLap,
                                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.75f)),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color(0x2200E5FF),
                                            contentColor = CyanAccent
                                        ),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("landscape_sw_lap")
                                    ) {
                                        Text("Lap", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                ThemedActionPill(
                                    text = "End Session",
                                    onClick = onEndSession,
                                    testTag = "landscape_sw_end"
                                )
                                ThemedActionPill(
                                    text = "Reset",
                                    onClick = onReset,
                                    testTag = "landscape_sw_reset"
                                )
                                if (isActive) {
                                    ThemedActionPill(
                                        text = "Lap",
                                        onClick = onLap,
                                        textColor = CyanAccent,
                                        testTag = "landscape_sw_lap"
                                    )
                                }
                            }
                        } else {
                            Text("Tap Play to start", color = if (isClassic) TextMuted else tokens.secondaryText, fontSize = 11.sp)
                        }
                    } else {
                        if (isActive) {
                            if (isClassic) {
                                OutlinedButton(
                                    onClick = onEndSession,
                                    border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0x18FFFFFF),
                                        contentColor = TextSecondary
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("landscape_end_session")
                                ) {
                                    Text(endSessionLabel, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                OutlinedButton(
                                    onClick = onReset,
                                    border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0x12FFFFFF),
                                        contentColor = TextMuted
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("landscape_reset")
                                ) {
                                    Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                ThemedActionPill(
                                    text = endSessionLabel,
                                    onClick = onEndSession,
                                    testTag = "landscape_end_session"
                                )
                                ThemedActionPill(
                                    text = "Reset",
                                    onClick = onReset,
                                    testTag = "landscape_reset"
                                )
                            }
                        }

                        if (isRunning) {
                            if (isClassic) {
                                OutlinedButton(
                                    onClick = onQuietModeToggle,
                                    border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.75f)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0x22FFA640),
                                        contentColor = AmberAccent
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("landscape_quiet_mode")
                                ) {
                                    Text("Quiet Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                ThemedActionPill(
                                    text = "Quiet Mode",
                                    onClick = onQuietModeToggle,
                                    textColor = AmberAccent,
                                    testTag = "landscape_quiet_mode"
                                )
                            }
                        } else if (isQuietMode) {
                            if (isClassic) {
                                OutlinedButton(
                                    onClick = onQuietModeToggle,
                                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.75f)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0x2200D675),
                                        contentColor = EmeraldAccent
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("landscape_resume")
                                ) {
                                    Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                ThemedActionPill(
                                    text = "Resume",
                                    onClick = onQuietModeToggle,
                                    isFilled = true,
                                    testTag = "landscape_resume"
                                )
                            }
                        } else if (!isActive && showSetTimer) {
                            if (isClassic) {
                                OutlinedButton(
                                    onClick = onSetTimer,
                                    border = BorderStroke(1.dp, StudyAccent.copy(alpha = 0.8f)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = StudyAccent
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("landscape_set_timer")
                                ) {
                                    Text("Set Timer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                ThemedActionPill(
                                    text = "Set Timer",
                                    onClick = onSetTimer,
                                    textColor = tokens.accent,
                                    testTag = "landscape_set_timer"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
