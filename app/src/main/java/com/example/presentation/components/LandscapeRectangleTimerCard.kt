package com.example.presentation.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    modifier: Modifier = Modifier
) {
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
            color = textColor
        )
        // Seconds: SMALL, dim placed after them (larger on Stopwatch)
        if (seconds.isNotEmpty()) {
            val secondsFontSize = if (isStopwatch) {
                (fontSize.value * 0.55f).sp
            } else {
                (fontSize.value * 0.38f).coerceAtLeast(14f).sp
            }
            val secondsColor = if (isStopwatch) CyanAccent else Color.White.copy(alpha = 0.55f)
            val bottomPadding = (fontSize.value * 0.12f).dp
            Text(
                text = seconds,
                fontSize = secondsFontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = secondsColor,
                modifier = Modifier.padding(bottom = bottomPadding)
            )
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
    accentColor: Color = StudyAccent,
    onPlayPause: () -> Unit,
    onEndSession: () -> Unit,
    onReset: () -> Unit,
    onQuietModeToggle: () -> Unit = {},
    onSetTimer: () -> Unit = {},
    onLap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cornerRadius = 3.dp
    val strokeWidthDp = if (hasProgress) 5.5.dp else 1.5.dp

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
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

                // 1. Faint grey track along card border
                val trackColor = Color(0xFF262626)
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
                            accentColor.copy(alpha = 0.5f)
                        } else {
                            accentColor
                        }

                        drawPath(
                            path = progressPath,
                            color = drawColor,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }
            }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Calculate big font size so HH:MM fills about 90% of card width
        val dynamicFontSize = (maxWidth.value * 0.20f).coerceIn(46f, 66f).sp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Mode Label on Top
            Text(
                text = modeLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = if (isPaused) TextMuted else accentColor,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )

            // 2. Large Digits (center, bright white, HH:MM big, seconds small)
            TimerDigitsDisplay(
                hoursMinutes = timeHoursMinutes,
                seconds = timeSeconds,
                isStopwatch = isStopwatch,
                fontSize = dynamicFontSize,
                textColor = Color.White
            )

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

            // 4. Actions Row at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isStopwatch) {
                    if (isActive) {
                        TextButton(
                            onClick = onEndSession,
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                            modifier = Modifier.testTag("landscape_sw_end")
                        ) {
                            Text("End Session", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = onReset,
                            colors = ButtonDefaults.textButtonColors(contentColor = TextMuted),
                            modifier = Modifier.testTag("landscape_sw_reset")
                        ) {
                            Text("Reset", fontSize = 11.sp)
                        }
                        if (isRunning) {
                            TextButton(
                                onClick = onLap,
                                colors = ButtonDefaults.textButtonColors(contentColor = CyanAccent),
                                modifier = Modifier.testTag("landscape_sw_lap")
                            ) {
                                Text("Lap", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text("Tap Play to start", color = TextMuted, fontSize = 11.sp)
                    }
                } else {
                    if (isActive) {
                        TextButton(
                            onClick = onEndSession,
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                            modifier = Modifier.testTag("landscape_end_session")
                        ) {
                            Text("End Session", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = onReset,
                            colors = ButtonDefaults.textButtonColors(contentColor = TextMuted),
                            modifier = Modifier.testTag("landscape_reset")
                        ) {
                            Text("Reset", fontSize = 11.sp)
                        }
                    }

                    if (isRunning) {
                        TextButton(
                            onClick = onQuietModeToggle,
                            colors = ButtonDefaults.textButtonColors(contentColor = AmberAccent),
                            modifier = Modifier.testTag("landscape_quiet_mode")
                        ) {
                            Text("Quiet Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isQuietMode) {
                        TextButton(
                            onClick = onQuietModeToggle,
                            colors = ButtonDefaults.textButtonColors(contentColor = EmeraldAccent),
                            modifier = Modifier.testTag("landscape_resume")
                        ) {
                            Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (!isActive) {
                        TextButton(
                            onClick = onSetTimer,
                            colors = ButtonDefaults.textButtonColors(contentColor = StudyAccent),
                            modifier = Modifier.testTag("landscape_set_timer")
                        ) {
                            Text("Set Timer", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
