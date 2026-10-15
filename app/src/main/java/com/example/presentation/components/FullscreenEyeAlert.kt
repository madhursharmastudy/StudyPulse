package com.example.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import com.example.core.timer.TimerEngine
import com.example.core.timer.TimerSnapshot
import com.example.ui.theme.*

@Composable
fun FullscreenEyeAlert(
    snapshot: TimerSnapshot,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EyePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScalePulse"
    )

    val secondsRemaining = (snapshot.currentSegmentRemainingMillis / 1000).coerceAtLeast(0)
    val totalSeconds = snapshot.eyeRestDurationSeconds.coerceAtLeast(1)
    val progress = (secondsRemaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isClassic = isExistingTheme(activeTheme)
    val textColor = if (isClassic) TextPrimary else tokens.text
    val subTextColor = if (isClassic) TextSecondary else tokens.secondaryText
    val trackCol = if (isClassic) Color(0xFF1C1C1E) else if (activeTheme == "Ultra Premium") Color.White.copy(alpha = 0.10f) else tokens.chipFill
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    AppBackground(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isLandscape) 8.dp else 24.dp),
            contentAlignment = Alignment.Center
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Animated Eye Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(pulseScale)
                    .size(if (isLandscape) 44.dp else 96.dp)
                    .clip(CircleShape)
                    .background(Color(0x2600D675))
                    .border(2.dp, EyeCareAccent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Eye Care Alert",
                    tint = EyeCareAccent,
                    modifier = Modifier.size(if (isLandscape) 24.dp else 52.dp)
                )
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 24.dp))

            Text(
                text = "EYE REST",
                style = if (isLandscape) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    else MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 4.sp),
                color = EyeCareAccent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 2.dp else 8.dp))

            Text(
                text = "Look away from the screen.",
                style = if (isLandscape) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    else MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 32.dp))

            // Countdown circle & digits
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(if (isLandscape) 72.dp else 140.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = EyeCareAccent,
                    strokeWidth = if (isLandscape) 4.dp else 6.dp,
                    trackColor = trackCol
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$secondsRemaining",
                        fontSize = if (isLandscape) 26.sp else 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = textColor
                    )
                    Text(
                        text = "SECONDS",
                        style = if (isLandscape) MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            else MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                        color = EyeCareAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 36.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(if (isLandscape) 0.65f else 0.85f)
            ) {
                if (isClassic) {
                    OutlinedButton(
                        onClick = { TimerEngine.skipEyeRest() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isLandscape) 36.dp else 48.dp)
                            .testTag("skip_eye_rest_button")
                    ) {
                        Text("SKIP")
                    }

                    Button(
                        onClick = { TimerEngine.completeEyeRest(autoResumed = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = EyeCareAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isLandscape) 36.dp else 48.dp)
                            .testTag("resume_early_eye_button")
                    ) {
                        Text("RESUME", fontWeight = FontWeight.Bold)
                    }
                } else {
                    ThemedActionPill(
                        text = "Skip",
                        onClick = { TimerEngine.skipEyeRest() },
                        modifier = Modifier.weight(1f),
                        testTag = "skip_eye_rest_button"
                    )
                    ThemedActionPill(
                        text = "Resume",
                        onClick = { TimerEngine.completeEyeRest(autoResumed = false) },
                        isFilled = true,
                        modifier = Modifier.weight(1f),
                        testTag = "resume_early_eye_button"
                    )
                }
            }
        }
    }
}
}
