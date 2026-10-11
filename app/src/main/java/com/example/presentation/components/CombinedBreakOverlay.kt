package com.example.presentation.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.stretch.BodyZone
import com.example.core.stretch.ExerciseCatalog
import com.example.core.stretch.StretchBreakManager
import com.example.core.stretch.StretchExercise
import com.example.core.timer.TimerSnapshot
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

/**
 * CombinedBreakOverlay (Part 4B)
 * Full-screen suggestion overlay covering:
 * - Step 1: 1-minute water & walk (if Water is ON)
 * - Step 2: 6 exercises (~45s each) or 3 in short mode (if Stretch is ON)
 * - Step 3: "Shabash" completion screen with motivational one-liner and streak count
 */
@Composable
fun CombinedBreakOverlay(
    snapshot: TimerSnapshot,
    shortMode: Boolean,
    includeStanding: Boolean,
    onCompleteBreak: (completedStretches: Boolean) -> Unit,
    onSkipAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val waterOn = snapshot.waterReminderEnabled
    val stretchOn = snapshot.stretchBreakEnabled

    // Record total break start time to show accurate "Break completed: X min Y sec"
    val breakStartMonotonic = remember { SystemClock.elapsedRealtime() }
    var totalBreakElapsedMillis by remember { mutableLongStateOf(0L) }

    // Determine starting stage
    var currentStage by remember {
        mutableStateOf(if (waterOn) "WATER" else if (stretchOn) "EXERCISES" else "SHABASH")
    }

    // Exercise list loaded for this break
    val exercises = remember(shortMode, includeStanding) {
        if (stretchOn) StretchBreakManager.getExercisesForBreak(shortMode, includeStanding) else emptyList()
    }
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var anyStretchCompleted by remember { mutableStateOf(false) }

    AppBackground(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
        when (currentStage) {
            "WATER" -> {
                WaterBreakStep(
                    hasStretchAfter = stretchOn,
                    onNext = {
                        if (stretchOn) currentStage = "EXERCISES" else {
                            totalBreakElapsedMillis = SystemClock.elapsedRealtime() - breakStartMonotonic
                            currentStage = "SHABASH"
                        }
                    },
                    onSkip = {
                        if (stretchOn) currentStage = "EXERCISES" else onSkipAll()
                    },
                    onSkipAll = onSkipAll
                )
            }
            "EXERCISES" -> {
                if (exercises.isNotEmpty() && currentExerciseIndex < exercises.size) {
                    val currentExercise = exercises[currentExerciseIndex]
                    ExerciseStep(
                        exercise = currentExercise,
                        exerciseNumber = currentExerciseIndex + 1,
                        totalExercises = exercises.size,
                        onDone = {
                            StretchBreakManager.recordExerciseCompleted(currentExercise.id)
                            anyStretchCompleted = true
                            if (currentExerciseIndex + 1 < exercises.size) {
                                currentExerciseIndex++
                            } else {
                                totalBreakElapsedMillis = SystemClock.elapsedRealtime() - breakStartMonotonic
                                currentStage = "SHABASH"
                            }
                        },
                        onSkip = {
                            StretchBreakManager.recordExerciseSkipped(currentExercise.id)
                            if (currentExerciseIndex + 1 < exercises.size) {
                                currentExerciseIndex++
                            } else {
                                totalBreakElapsedMillis = SystemClock.elapsedRealtime() - breakStartMonotonic
                                currentStage = "SHABASH"
                            }
                        },
                        onSkipAll = onSkipAll
                    )
                } else {
                    totalBreakElapsedMillis = SystemClock.elapsedRealtime() - breakStartMonotonic
                    currentStage = "SHABASH"
                }
            }
            "SHABASH" -> {
                ShabashStep(
                    totalElapsedMillis = totalBreakElapsedMillis,
                    onDismiss = {
                        onCompleteBreak(anyStretchCompleted || !waterOn)
                    }
                )
            }
        }
    }
}
}

/**
 * Step 1: Water & Short Walk (1 minute)
 */
@Composable
private fun WaterBreakStep(
    hasStretchAfter: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onSkipAll: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(60) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        onNext()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "WaterPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaterScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .scale(pulseScale)
                .size(90.dp)
                .clip(CircleShape)
                .background(Color(0x2200B4FF))
                .border(2.dp, WaterAccent, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = "Hydration Reminder",
                tint = WaterAccent,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "BREAK TIME",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            ),
            color = WaterAccent
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Drink a glass of water and take a short walk to refresh.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 60-second countdown timer
        Text(
            text = formatSeconds(secondsLeft),
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = WaterAccent, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
                .testTag("water_next_button")
        ) {
            Text(
                text = if (hasStretchAfter) "Next: Stretches" else "Done",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSkip) {
                Text("Skip", color = TextSecondary, fontSize = 13.sp)
            }
            TextButton(onClick = onSkipAll) {
                Text("Skip all", color = TextMuted, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Step 2: Individual Exercise screen
 */
@Composable
private fun ExerciseStep(
    exercise: StretchExercise,
    exerciseNumber: Int,
    totalExercises: Int,
    onDone: () -> Unit,
    onSkip: () -> Unit,
    onSkipAll: () -> Unit
) {
    var secondsLeft by remember(exercise.id) { mutableIntStateOf(exercise.durationSeconds) }
    val totalSeconds = exercise.durationSeconds

    // Two-sided timing logic: First half left, 5s switch, second half right
    val halfTime = (totalSeconds - 5) / 2
    val switchStart = totalSeconds - halfTime - 5
    val switchEnd = totalSeconds - halfTime

    val sideText = if (exercise.twoSided) {
        when {
            secondsLeft > switchEnd -> "LEFT SIDE"
            secondsLeft in switchStart..switchEnd -> "SWITCH SIDES"
            else -> "RIGHT SIDE"
        }
    } else null

    // Audio Bell triggers
    var hasPlayedSwitchBell by remember(exercise.id) { mutableStateOf(false) }

    LaunchedEffect(exercise.id) {
        // Play short bell sound at start of exercise
        playBellSound()

        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
            if (exercise.twoSided && secondsLeft == switchEnd && !hasPlayedSwitchBell) {
                hasPlayedSwitchBell = true
                playBellSound()
            }
        }
        onDone()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top row: Progress & Zone pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Exercise $exerciseNumber of $totalExercises",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AmberAccent
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF1E1E1E)
            ) {
                Text(
                    text = exercise.zone.displayName,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center visual: Looping video/animation from assets or clean fallback
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF12141A))
                .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            ExerciseMediaVisual(mediaFileName = exercise.mediaFileName, exercise = exercise)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Exercise Name & Sets text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = exercise.setsText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AmberAccent
            )

            if (sideText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sideText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sideText == "SWITCH SIDES") RoseAccent else CyanAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Countdown timer
        Text(
            text = formatSeconds(secondsLeft),
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons: Done / Skip / Skip all
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("exercise_done_button")
            ) {
                Text("Done", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            OutlinedButton(
                onClick = onSkip,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = BorderStroke(1.dp, CharcoalBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(0.7f)
                    .height(46.dp)
                    .testTag("exercise_skip_button")
            ) {
                Text("Skip", fontSize = 13.sp)
            }
        }

        TextButton(onClick = onSkipAll) {
            Text("Skip all", color = TextMuted, fontSize = 12.sp)
        }

        // Disclaimer
        Text(
            text = "General wellness tips, not medical advice. Dard ho to ruk jao.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * Exercise Media Visual:
 * Attempts to load video from assets folder (copied to cache for VideoView playback);
 * Falls back to an animated pulsing stick-figure illustration if media file is absent.
 */
@Composable
private fun ExerciseMediaVisual(
    mediaFileName: String,
    exercise: StretchExercise
) {
    val context = LocalContext.current
    var cachedFileUri by remember(mediaFileName) { mutableStateOf<Uri?>(null) }

    LaunchedEffect(mediaFileName) {
        try {
            // Check if file exists in assets
            val inputStream = context.assets.open(mediaFileName)
            val outFile = File(context.cacheDir, mediaFileName)
            FileOutputStream(outFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            cachedFileUri = Uri.fromFile(outFile)
        } catch (_: Exception) {
            cachedFileUri = null
        }
    }

    if (cachedFileUri != null) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    cachedFileUri?.let { setVideoURI(it) }
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f) // Silent looping video
                        start()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // High quality placeholder animation
        val infiniteTransition = rememberInfiniteTransition(label = "StickPulse")
        val breathAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "BreathAlpha"
        )
        val pulseRing by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseRing"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(90.dp)
            ) {
                // Pulsing outline circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawCircle(
                        color = AmberAccent.copy(alpha = 0.25f * breathAlpha),
                        radius = (size.width / 2f - 4.dp.toPx()) * pulseRing
                    )
                    // Head
                    drawCircle(
                        color = AmberAccent.copy(alpha = breathAlpha),
                        radius = 12.dp.toPx(),
                        center = Offset(cx, cy - 16.dp.toPx())
                    )
                    // Spine / Torso
                    drawLine(
                        color = TextPrimary.copy(alpha = breathAlpha),
                        start = Offset(cx, cy - 4.dp.toPx()),
                        end = Offset(cx, cy + 20.dp.toPx()),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Arms stretched
                    drawLine(
                        color = AmberAccent.copy(alpha = breathAlpha),
                        start = Offset(cx - 20.dp.toPx(), cy + 4.dp.toPx()),
                        end = Offset(cx + 20.dp.toPx(), cy + 4.dp.toPx()),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = exercise.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Text(
                text = "[${exercise.mediaFileName}]",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}

/**
 * Step 3: "Shabash" Completion Screen
 */
@Composable
private fun ShabashStep(
    totalElapsedMillis: Long,
    onDismiss: () -> Unit
) {
    // 5-second countdown to auto-close or button click
    var countdownSeconds by remember { mutableIntStateOf(5) }

    // Motivational lines pool (Hindi & English)
    val motivationalQuotes = remember {
        listOf(
            "Body recharge ho gaya! Wapas lag jao.",
            "Sharir khush, dimag sharp.",
            "Small breaks, huge focus.",
            "Agle session ke liye ready!",
            "Great posture, greater focus!",
            "Thoda hile, bada fayda!",
            "Mind refreshed, ready to crush goals.",
            "Health first, excellence follows.",
            "Fresh eyes, energetic body!",
            "Energy restored. Let's conquer!",
            "Stretching complete! Back in the zone.",
            "Relaxed shoulders, sharpened mind."
        )
    }
    val randomQuote = remember { motivationalQuotes.random() }

    // Format elapsed break time
    val elapsedSec = (totalElapsedMillis / 1000).coerceAtLeast(1)
    val elapsedMin = elapsedSec / 60
    val elapsedRemainingSec = elapsedSec % 60
    val breakDurationStr = if (elapsedMin > 0) {
        "${elapsedMin} min ${elapsedRemainingSec} sec"
    } else {
        "${elapsedSec} sec"
    }

    // Streak count (resets daily)
    val streakCount = remember { StretchBreakManager.getTodayBreakCount() + 1 }
    val streakSuffix = when (streakCount) {
        1 -> "1st"
        2 -> "2nd"
        3 -> "3rd"
        else -> "${streakCount}th"
    }

    LaunchedEffect(Unit) {
        // Soft voice chime / gong at Shabash
        playChimeSound()

        while (countdownSeconds > 0) {
            delay(1000L)
            countdownSeconds--
        }
        onDismiss()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color(0x2210B981))
                .border(2.dp, EmeraldAccent, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = EmeraldAccent,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "SHABASH! 🎉",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            ),
            color = EmeraldAccent
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Break completed: $breakDurationStr",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "🔥 $streakSuffix break today",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = AmberAccent
        )

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF14171E),
            border = BorderStroke(1.dp, CharcoalBorder),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "“$randomQuote”",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
                .testTag("shabash_done_button")
        ) {
            Text(
                text = "Back to study ($countdownSeconds)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

/**
 * Short bell sound for exercise start and switch marks
 */
private fun playBellSound() {
    try {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
    } catch (_: Exception) {}
}

/**
 * Soft chime sound for Shabash screen
 */
private fun playChimeSound() {
    try {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        tg.startTone(ToneGenerator.TONE_PROP_PROMPT, 350)
    } catch (_: Exception) {}
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s)
}
