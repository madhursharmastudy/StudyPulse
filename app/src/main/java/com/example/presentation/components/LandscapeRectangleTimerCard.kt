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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * LandscapeRectangleTimerCard (Part 4E)
 * Reusable landscape card used on Home and Timers tabs.
 *
 * Features:
 * - 3dp rounded corners, dark card color with subtle perimeter glow.
 * - Card border IS the progress bar: 5-6dp thick clockwise line starting at top center.
 * - Behind it a faint grey track.
 * - When paused, progress line dims to 50% opacity.
 * - Fixed-time timers fill the border; Stopwatch shows a plain thin grey border (no progress).
 * - Big bold HH:MM digits with small dim seconds in the corner (larger ~55% on Stopwatch).
 * - Clearly visible play/pause button below digits.
 * - Actions row inside/under the card: End Session, Reset, and Quiet Mode / Set Timer / Lap.
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
    accentColor: Color = StudyAccent,
    onPlayPause: () -> Unit,
    onEndSession: () -> Unit,
    onReset: () -> Unit,
    onQuietModeToggle: () -> Unit = {},
    onSetTimer: () -> Unit = {},
    onLap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cornerRadius = 4.dp
    val strokeWidthDp = if (hasProgress) 5.dp else 1.5.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(DarkCard)
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

                // Build clockwise rounded rectangle path starting at top center
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

                // 1. Faint grey track along the card border
                val trackColor = Color(0xFF262626)
                drawPath(
                    path = basePath,
                    color = trackColor,
                    style = Stroke(width = strokePx)
                )

                // 2. Clockwise progress line for fixed-time timers
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
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
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
                maxLines = 1
            )

            // 2. Large Digits (HH:MM big, seconds in corner)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = timeHoursMinutes,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-1).sp,
                    color = TextPrimary
                )
                if (timeSeconds.isNotEmpty()) {
                    val secondsSize = if (isStopwatch) 25.sp else 18.sp
                    val secondsColor = if (isStopwatch) CyanAccent else TextMuted
                    Text(
                        text = timeSeconds,
                        fontSize = secondsSize,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = secondsColor,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            // 3. Play / Pause Button
            Box(
                modifier = Modifier
                    .size(46.dp)
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
                    modifier = Modifier.size(26.dp)
                )
            }

            // 4. Actions Row inside/bottom edge of card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
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
