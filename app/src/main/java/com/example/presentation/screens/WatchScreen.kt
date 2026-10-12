package com.example.presentation.screens

import android.app.Activity
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.timer.*
import com.example.data.db.DailyGoalEntity
import com.example.data.db.SubjectEntity
import com.example.data.db.TopicEntity
import com.example.data.repository.AppSettings
import com.example.presentation.components.DeskClockDisplay
import com.example.presentation.components.FullscreenEyeAlert
import com.example.presentation.components.FullscreenWaterAlert
import com.example.presentation.components.AppBackground
import com.example.presentation.components.LandscapeRectangleTimerCard
import com.example.presentation.components.TimerDigitsDisplay
import com.example.presentation.components.formatTimeToHHMMSS
import com.example.ui.theme.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.db.StudySessionEntity
import com.example.core.stretch.StretchBreakManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun WatchScreen(
    snapshot: TimerSnapshot,
    settings: AppSettings,
    subjects: List<SubjectEntity>,
    topics: List<TopicEntity>,
    todayGoal: DailyGoalEntity?,
    sessions: List<StudySessionEntity> = emptyList(),
    onStartSessionClick: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSelectSubject: (SubjectEntity) -> Unit = {},
    onSelectTopic: (TopicEntity) -> Unit = {},
    onAddNewSubject: (name: String, icon: String, colorHex: String, goalMinutes: Int) -> Unit = { _, _, _, _ -> },
    onAddNewTopic: (subjectId: Long, name: String) -> Unit = { _, _ -> },
    onUpdateSubject: (SubjectEntity) -> Unit = {},
    onDeleteSubject: (Long) -> Unit = {},
    onUpdateTopic: (TopicEntity) -> Unit = {},
    onDeleteTopic: (Long) -> Unit = {},
    onUpdateDefaultStudyDuration: (Int) -> Unit = {},
    onUpdateEyeStudyInterval: (Int) -> Unit = {},
    onUpdateEyeRestDuration: (Int) -> Unit = {},
    onUpdateWaterInterval: (Int) -> Unit = {},
    onUpdateWaterBreakDuration: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSubjectPicker by remember { mutableStateOf(false) }
    var showSetTimerDialog by remember { mutableStateOf(false) }

    // Keep screen on in Desk Clock Mode
    DisposableEffect(settings.keepScreenOn) {
        val activity = context as? Activity
        if (settings.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val todaySessionsCount = remember(sessions) {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis
        sessions.count { it.startTimestamp >= startOfToday }
    }

    // Shared Subject & Topic selection across Home and Timers
    val currentSubject = remember(subjects, settings.selectedSubjectId) {
        subjects.firstOrNull { it.id == settings.selectedSubjectId } ?: subjects.firstOrNull()
    }
    val filteredTopics = remember(topics, currentSubject) {
        currentSubject?.let { sub -> topics.filter { it.subjectId == sub.id } } ?: emptyList()
    }
    val currentTopic = remember(filteredTopics, settings.selectedTopicId) {
        filteredTopics.firstOrNull { it.id == settings.selectedTopicId } ?: filteredTopics.firstOrNull()
    }

    val currentSubjectName = if (snapshot.state != TimerState.IDLE && snapshot.subjectName.isNotEmpty()) {
        snapshot.subjectName
    } else {
        currentSubject?.name ?: "General Study"
    }
    val currentTopicName = if (snapshot.state != TimerState.IDLE && snapshot.topicName.isNotEmpty()) {
        snapshot.topicName
    } else {
        currentTopic?.name ?: "Deep Focus"
    }

    val waterStretchLabel = when {
        snapshot.waterReminderEnabled && snapshot.stretchBreakEnabled -> "Water & Stretch in"
        snapshot.stretchBreakEnabled -> "Stretch Break in"
        else -> "Water Break in"
    }

    AppBackground(
        modifier = modifier.fillMaxSize()
    ) {
        if (isLandscape) {
            LandscapeWatchScreen(
                snapshot = snapshot,
                settings = settings,
                subjects = subjects,
                topics = topics,
                todayGoal = todayGoal,
                todaySessionsCount = todaySessionsCount,
                currentSubjectName = currentSubjectName,
                currentTopicName = currentTopicName,
                waterStretchLabel = waterStretchLabel,
                onOpenSettings = onOpenSettings,
                onShowSetTimerDialog = { showSetTimerDialog = true },
                onSubjectClick = { showSubjectPicker = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Main minimal watch layout
            Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Header Bar: Logo & Tagline + Settings Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "StudyPulse",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = "StudyPulse Icon",
                            tint = EyeCareAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Focus. Rest. Hydrate. Repeat.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                // Settings circular action button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161616))
                        .border(1.dp, Color(0xFF282828), CircleShape)
                        .clickable { onOpenSettings() }
                        .testTag("watch_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open Settings",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Large Digital Desk Watch (Date, Day of Week, and Hero Digits)
            DeskClockDisplay(
                use24Hour = settings.use24HourFormat,
                showSeconds = settings.showSeconds,
                clockStyle = settings.clockStyle,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Study Session Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF222222),
                    thickness = 1.dp
                )
                Text(
                    text = "S T U D Y   S E S S I O N",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF222222),
                    thickness = 1.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Hero Circular Study Timer
            CircularStudyTimer(
                snapshot = snapshot,
                defaultDurationMinutes = settings.defaultStudyDurationMinutes,
                subjects = subjects,
                topics = topics,
                onSubjectClick = { showSubjectPicker = true }
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (snapshot.state != TimerState.IDLE) {
                    TextButton(
                        onClick = { TimerEngine.stop() },
                        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                        modifier = Modifier.testTag("watch_end_session_button")
                    ) {
                        Text("End Session", fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = { TimerEngine.reset() },
                        colors = ButtonDefaults.textButtonColors(contentColor = TextMuted),
                        modifier = Modifier.testTag("watch_reset_button")
                    ) {
                        Text("Reset", fontSize = 12.sp)
                    }
                }
                if (snapshot.state == TimerState.STUDYING) {
                    TextButton(
                        onClick = { TimerEngine.enterQuietMode() },
                        colors = ButtonDefaults.textButtonColors(contentColor = AmberAccent),
                        modifier = Modifier.testTag("watch_quiet_mode_button")
                    ) {
                        Text("Quiet Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (snapshot.isQuietMode) {
                    TextButton(
                        onClick = { TimerEngine.exitQuietMode() },
                        colors = ButtonDefaults.textButtonColors(contentColor = EmeraldAccent),
                        modifier = Modifier.testTag("watch_quiet_resume_button")
                    ) {
                        Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(
                        onClick = { showSetTimerDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = StudyAccent),
                        modifier = Modifier.testTag("watch_set_timer_button")
                    ) {
                        Text("Set Timer", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Two Compact Cards: Eye Rest and Water Break
            val isWaterOrStretchOn = snapshot.waterReminderEnabled || snapshot.stretchBreakEnabled
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EyeRestMiniCard(
                    snapshot = snapshot,
                    modifier = if (isWaterOrStretchOn) Modifier.weight(1f) else Modifier.fillMaxWidth()
                )

                if (isWaterOrStretchOn) {
                    WaterBreakMiniCard(
                        snapshot = snapshot,
                        label = waterStretchLabel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6. User-Selected Subject & Topic Card (shared state across Home and Timers)
            SubjectTopicCard(
                subjectName = currentSubjectName,
                topicName = currentTopicName,
                onClick = { showSubjectPicker = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Footer Motivation Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "—— A BETTER YOU ——",
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4A4A4A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ONE FOCUSED SESSION AT A TIME",
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = Color(0xFF4A4A4A)
                )
            }
        }
        }

        // Fullscreen Eye Rest Alert
        if (snapshot.state == TimerState.EYE_REST) {
            FullscreenEyeAlert(
                snapshot = snapshot,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Fullscreen Water Break Alert
        if (snapshot.state == TimerState.WATER_BREAK) {
            FullscreenWaterAlert(
                snapshot = snapshot,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Subject & Topic Picker Dialog
        if (showSubjectPicker) {
            SubjectPickerDialog(
                subjects = subjects,
                topics = topics,
                currentSubjectId = snapshot.subjectId,
                defaultDurationMinutes = settings.defaultStudyDurationMinutes,
                onDismiss = { showSubjectPicker = false },
                onSelectSubjectAndTopic = { sub, topic, durationMins ->
                    TimerEngine.startStudySession(
                        subjectId = sub.id,
                        topicId = topic?.id ?: 0L,
                        subjectName = sub.name,
                        topicName = topic?.name ?: "General Study",
                        type = snapshot.timerType,
                        durationMinutes = durationMins
                    )
                    showSubjectPicker = false
                },
                onAddNewSubject = onAddNewSubject,
                onAddNewTopic = onAddNewTopic,
                onUpdateSubject = onUpdateSubject,
                onDeleteSubject = onDeleteSubject,
                onUpdateTopic = onUpdateTopic,
                onDeleteTopic = onDeleteTopic
            )
        }

        if (showSetTimerDialog) {
            SetTimerDialog(
                settings = settings,
                onDismiss = { showSetTimerDialog = false },
                onSave = { studyMinutes, eyeIntervalMinutes, eyeRestSeconds, waterIntervalMinutes, waterBreakMinutes ->
                    // Apply immediately to current running session
                    TimerEngine.setPlannedDuration(studyMinutes)
                    TimerEngine.eyeStudyIntervalSeconds = eyeIntervalMinutes * 60L
                    TimerEngine.eyeRestDurationSeconds = eyeRestSeconds.toLong()
                    TimerEngine.waterIntervalSeconds = waterIntervalMinutes * 60L
                    TimerEngine.waterBreakDurationSeconds = waterBreakMinutes * 60L

                    // Persist to datastore (reusing existing update functions)
                    onUpdateDefaultStudyDuration(studyMinutes)
                    onUpdateEyeStudyInterval(eyeIntervalMinutes)
                    onUpdateEyeRestDuration(eyeRestSeconds)
                    onUpdateWaterInterval(waterIntervalMinutes)
                    onUpdateWaterBreakDuration(waterBreakMinutes * 60)

                    showSetTimerDialog = false
                }
            )
        }
    }
}

@Composable
fun CircularStudyTimer(
    snapshot: TimerSnapshot,
    defaultDurationMinutes: Int,
    subjects: List<SubjectEntity>,
    topics: List<TopicEntity>,
    onSubjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = snapshot.progressFraction

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(230.dp)
            .testTag("circular_study_timer")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Background track
            drawArc(
                color = Color(0xFF161616),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            // Active progress arc
            val sweep = if (snapshot.state == TimerState.IDLE) {
                0f
            } else {
                progress * 360f
            }

            if (sweep > 0f) {
                drawArc(
                    color = StudyAccent,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Inner Timer Display and Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "STUDY TIMER",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (snapshot.isQuietMode) {
                Text(
                    text = "QUIET",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = AmberAccent
                )
            } else {
                val displayStr = if (snapshot.state == TimerState.IDLE) {
                    String.format(Locale.getDefault(), "%02d:00", defaultDurationMinutes)
                } else {
                    snapshot.formattedActiveDisplay()
                }
                val (hhMm, ss) = formatTimeToHHMMSS(displayStr)
                TimerDigitsDisplay(
                    hoursMinutes = hhMm,
                    seconds = ss,
                    fontSize = 42.sp,
                    textColor = Color.White
                )
            }

            Text(
                text = if (snapshot.isQuietMode) "PAUSED IN QUIET MODE" else "/ ${snapshot.formatPlanned()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (snapshot.isQuietMode) AmberAccent else TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Play / Pause circular button
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (snapshot.state == TimerState.STUDYING) Color(0x33FFA640) else Color(0xFF1E1E1E)
                    )
                    .border(
                        1.dp,
                        if (snapshot.state == TimerState.STUDYING) StudyAccent else Color(0xFF333333),
                        CircleShape
                    )
                    .clickable {
                        if (snapshot.isQuietMode) {
                            TimerEngine.exitQuietMode()
                        } else {
                            when (snapshot.state) {
                                TimerState.STUDYING -> TimerEngine.pause()
                                TimerState.PAUSED -> TimerEngine.resume()
                            TimerState.IDLE -> {
                                val selectedSub = subjects.firstOrNull { it.id == snapshot.subjectId }
                                    ?: subjects.firstOrNull()
                                    ?: SubjectEntity(name = "English Literature")
                                val selectedTop = topics.firstOrNull { it.subjectId == selectedSub.id }
                                    ?: TopicEntity(subjectId = selectedSub.id, name = "Romantic Poetry")

                                TimerEngine.startStudySession(
                                    subjectId = selectedSub.id,
                                    topicId = selectedTop.id,
                                    subjectName = selectedSub.name,
                                    topicName = selectedTop.name,
                                    type = snapshot.timerType,
                                    durationMinutes = defaultDurationMinutes
                                )
                            }
                            TimerState.STOPPED, TimerState.COMPLETED -> TimerEngine.reset()
                            else -> {}
                        }
                    }
                }
                    .testTag("watch_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (snapshot.state == TimerState.STUDYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (snapshot.state == TimerState.STUDYING) "Pause" else "Play",
                    tint = if (snapshot.state == TimerState.STUDYING) StudyAccent else TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun EyeRestMiniCard(
    snapshot: TimerSnapshot,
    modifier: Modifier = Modifier
) {
    val totalEyeIntervalSecs = (TimerEngine.eyeStudyIntervalSeconds).coerceAtLeast(1L)
    val remainingSecs = snapshot.nextEyeRestCountdownSeconds
    val eyeProgress = ((totalEyeIntervalSecs - remainingSecs).toFloat() / totalEyeIntervalSecs.toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
        ),
        modifier = modifier.testTag("eye_rest_mini_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x1A00D675)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Eye Rest",
                        tint = EyeCareAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Eye Rest in",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = snapshot.formattedEyeCountdown(),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EyeCareAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { eyeProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = EyeCareAccent,
                trackColor = Color(0xFF222222)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "20-20-20 Rule",
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WaterBreakMiniCard(
    snapshot: TimerSnapshot,
    label: String = "Water Break in",
    modifier: Modifier = Modifier
) {
    val totalWaterIntervalSecs = (TimerEngine.waterIntervalSeconds).coerceAtLeast(1L)
    val remainingSecs = snapshot.nextWaterCountdownSeconds
    val waterProgress = ((totalWaterIntervalSecs - remainingSecs).toFloat() / totalWaterIntervalSecs.toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
        ),
        modifier = modifier.testTag("water_break_mini_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x1A00B4FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Water Break",
                        tint = WaterAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = snapshot.formattedWaterCountdown(),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WaterAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { waterProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = WaterAccent,
                trackColor = Color(0xFF222222)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Stay Hydrated",
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SubjectTopicCard(
    subjectName: String,
    topicName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("subject_topic_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF202020)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Subject/Task",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = topicName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select Subject/Task",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectPickerDialog(
    subjects: List<SubjectEntity>,
    topics: List<TopicEntity>,
    currentSubjectId: Long,
    defaultDurationMinutes: Int,
    onDismiss: () -> Unit,
    onSelectSubjectAndTopic: (SubjectEntity, TopicEntity?, Int) -> Unit,
    onAddNewSubject: (name: String, icon: String, colorHex: String, goalMinutes: Int) -> Unit,
    onAddNewTopic: (subjectId: Long, name: String) -> Unit,
    onUpdateSubject: (SubjectEntity) -> Unit = {},
    onDeleteSubject: (Long) -> Unit = {},
    onUpdateTopic: (TopicEntity) -> Unit = {},
    onDeleteTopic: (Long) -> Unit = {}
) {
    var selectedSubject by remember(subjects) {
        mutableStateOf(subjects.firstOrNull { it.id == currentSubjectId } ?: subjects.firstOrNull())
    }
    val filteredTopics = remember(selectedSubject, topics) {
        selectedSubject?.let { sub -> topics.filter { it.subjectId == sub.id } } ?: emptyList()
    }
    var selectedTopic by remember(selectedSubject) {
        mutableStateOf(filteredTopics.firstOrNull())
    }
    var selectedDuration by remember { mutableIntStateOf(defaultDurationMinutes) }

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }
    var topicToEdit by remember { mutableStateOf<TopicEntity?>(null) }
    var topicToDelete by remember { mutableStateOf<TopicEntity?>(null) }

    val durationPresets = listOf(15, 25, 30, 45, 50, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text(
                text = "Select Subject/Task",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Subjects Header & Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUBJECT/TASK",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = StudyAccent
                    )
                    TextButton(
                        onClick = { showAddSubjectDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = StudyAccent)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Subject/Task", fontSize = 12.sp, color = StudyAccent)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                subjects.forEach { subject ->
                    val isSelected = selectedSubject?.id == subject.id
                    val hours = subject.totalStudySeconds / 3600
                    val minutes = (subject.totalStudySeconds % 3600) / 60

                    Surface(
                        onClick = { selectedSubject = subject },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0x33FFA640) else Color(0xFF1A1A1A),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) StudyAccent else Color(0xFF282828)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subject.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) StudyAccent else TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (subject.totalStudySeconds > 0) {
                                    Text(
                                        text = "${hours}h ${minutes}m",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { subjectToEdit = subject },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Subject/Task",
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                                IconButton(
                                    onClick = { subjectToDelete = subject },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Subject/Task",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Topic Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOPIC",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = StudyAccent
                    )
                    selectedSubject?.let { sub ->
                        TextButton(
                            onClick = { showAddTopicDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = StudyAccent)
                            Spacer(Modifier.width(4.dp))
                            Text("Add Topic", fontSize = 12.sp, color = StudyAccent)
                        }
                    }
                }

                if (filteredTopics.isNotEmpty()) {
                    filteredTopics.forEach { topic ->
                        val isSelected = selectedTopic?.id == topic.id
                        Surface(
                            onClick = { selectedTopic = topic },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0x33FFA640) else Color(0xFF181818),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) StudyAccent else Color(0xFF242424)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = topic.name,
                                    fontSize = 13.sp,
                                    color = if (isSelected) StudyAccent else TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { topicToEdit = topic },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Topic",
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                    IconButton(
                                        onClick = { topicToDelete = topic },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Topic",
                                            tint = Color.Red.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No specific topics added yet.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Duration selection
                Text(
                    text = "SESSION DURATION",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = StudyAccent
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    durationPresets.take(4).forEach { duration ->
                        val isSelected = selectedDuration == duration
                        Surface(
                            onClick = { selectedDuration = duration },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) StudyAccent else Color(0xFF1E1E1E),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${duration}m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    durationPresets.drop(4).forEach { duration ->
                        val isSelected = selectedDuration == duration
                        Surface(
                            onClick = { selectedDuration = duration },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) StudyAccent else Color(0xFF1E1E1E),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${duration}m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedSubject?.let { sub ->
                        onSelectSubjectAndTopic(sub, selectedTopic, selectedDuration)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudyAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                enabled = selectedSubject != null
            ) {
                Text("Start Study Session", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("Cancel")
            }
        }
    )

    // Add Subject Dialog
    if (showAddSubjectDialog) {
        var newSubjectName by remember { mutableStateOf("") }
        var dailyGoalTarget by remember { mutableStateOf("60") }

        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            containerColor = DarkCard,
            title = { Text("Add New Subject/Task", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newSubjectName,
                        onValueChange = { newSubjectName = it },
                        label = { Text("Subject/Task Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudyAccent,
                            unfocusedBorderColor = Color(0xFF444444),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dailyGoalTarget,
                        onValueChange = { dailyGoalTarget = it.filter { char -> char.isDigit() } },
                        label = { Text("Daily Goal (Minutes)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudyAccent,
                            unfocusedBorderColor = Color(0xFF444444),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubjectName.isNotBlank()) {
                            onAddNewSubject(
                                newSubjectName.trim(),
                                "menu_book",
                                "#FFA640",
                                dailyGoalTarget.toIntOrNull() ?: 60
                            )
                            showAddSubjectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyAccent, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubjectDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Add Topic Dialog
    if (showAddTopicDialog && selectedSubject != null) {
        var newTopicName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            containerColor = DarkCard,
            title = { Text("Add Topic/Sub task to ${selectedSubject?.name}", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newTopicName,
                    onValueChange = { newTopicName = it },
                    label = { Text("Topic/Sub task Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudyAccent,
                        unfocusedBorderColor = Color(0xFF444444),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTopicName.isNotBlank()) {
                            onAddNewTopic(selectedSubject!!.id, newTopicName.trim())
                            showAddTopicDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyAccent, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add Topic/Sub task", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTopicDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Edit Subject Dialog
    if (subjectToEdit != null) {
        var editSubjectName by remember(subjectToEdit) { mutableStateOf(subjectToEdit!!.name) }
        var editDailyGoalTarget by remember(subjectToEdit) { mutableStateOf(subjectToEdit!!.dailyGoalMinutes.toString()) }

        AlertDialog(
            onDismissRequest = { subjectToEdit = null },
            containerColor = DarkCard,
            title = { Text("Edit Subject/Task", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editSubjectName,
                        onValueChange = { editSubjectName = it },
                        label = { Text("Subject/Task Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudyAccent,
                            unfocusedBorderColor = Color(0xFF444444),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editDailyGoalTarget,
                        onValueChange = { editDailyGoalTarget = it.filter { char -> char.isDigit() } },
                        label = { Text("Daily Goal (Minutes)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudyAccent,
                            unfocusedBorderColor = Color(0xFF444444),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editSubjectName.isNotBlank() && subjectToEdit != null) {
                            val updated = subjectToEdit!!.copy(
                                name = editSubjectName.trim(),
                                dailyGoalMinutes = editDailyGoalTarget.toIntOrNull() ?: 60
                            )
                            onUpdateSubject(updated)
                            if (selectedSubject?.id == updated.id) {
                                selectedSubject = updated
                            }
                            subjectToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyAccent, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToEdit = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete Subject Confirmation Dialog
    if (subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            containerColor = DarkCard,
            title = { Text("Delete Subject/Task?", color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete \"${subjectToDelete!!.name}\"? This will also delete all its associated topics/sub tasks and study history.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subjectToDelete != null) {
                            onDeleteSubject(subjectToDelete!!.id)
                            if (selectedSubject?.id == subjectToDelete!!.id) {
                                val remaining = subjects.filter { it.id != subjectToDelete!!.id }
                                selectedSubject = remaining.firstOrNull()
                            }
                            subjectToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Edit Topic Dialog
    if (topicToEdit != null) {
        var editTopicName by remember(topicToEdit) { mutableStateOf(topicToEdit!!.name) }

        AlertDialog(
            onDismissRequest = { topicToEdit = null },
            containerColor = DarkCard,
            title = { Text("Edit Topic/Sub task", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = editTopicName,
                    onValueChange = { editTopicName = it },
                    label = { Text("Topic/Sub task Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudyAccent,
                        unfocusedBorderColor = Color(0xFF444444),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTopicName.isNotBlank() && topicToEdit != null) {
                            val updated = topicToEdit!!.copy(name = editTopicName.trim())
                            onUpdateTopic(updated)
                            if (selectedTopic?.id == updated.id) {
                                selectedTopic = updated
                            }
                            topicToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyAccent, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToEdit = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete Topic Confirmation Dialog
    if (topicToDelete != null) {
        AlertDialog(
            onDismissRequest = { topicToDelete = null },
            containerColor = DarkCard,
            title = { Text("Delete Topic/Sub task?", color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete \"${topicToDelete!!.name}\"?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (topicToDelete != null) {
                            onDeleteTopic(topicToDelete!!.id)
                            if (selectedTopic?.id == topicToDelete!!.id) {
                                val remaining = filteredTopics.filter { it.id != topicToDelete!!.id }
                                selectedTopic = remaining.firstOrNull()
                            }
                            topicToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetTimerDialog(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (
        studyMinutes: Int,
        eyeIntervalMinutes: Int,
        eyeRestSeconds: Int,
        waterIntervalMinutes: Int,
        waterBreakMinutes: Int
    ) -> Unit
) {
    var studyMinutesStr by remember { mutableStateOf(settings.defaultStudyDurationMinutes.toString()) }
    var eyeIntervalStr by remember { mutableStateOf(settings.eyeStudyIntervalMinutes.toString()) }
    var eyeRestStr by remember { mutableStateOf(settings.eyeRestDurationSeconds.toString()) }
    var waterIntervalStr by remember { mutableStateOf(settings.waterIntervalMinutes.toString()) }
    var waterBreakStr by remember { mutableStateOf((settings.waterBreakDurationSeconds / 60).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Timer Settings",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Section: Main Study Timer
                Column {
                    Text(
                        text = "STUDY SESSION DURATION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = StudyAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = studyMinutesStr,
                        onValueChange = { studyMinutesStr = it },
                        label = { Text("Study Duration (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudyAccent,
                            focusedLabelColor = StudyAccent,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("set_timer_study_input")
                    )
                }

                // Section: Eye Rest
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "EYE REST (20-20-20 SYSTEM)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = EyeCareAccent
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = eyeIntervalStr,
                            onValueChange = { eyeIntervalStr = it },
                            label = { Text("Interval (mins)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EyeCareAccent,
                                focusedLabelColor = EyeCareAccent,
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f).testTag("set_timer_eye_interval_input")
                        )
                        OutlinedTextField(
                            value = eyeRestStr,
                            onValueChange = { eyeRestStr = it },
                            label = { Text("Rest (secs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EyeCareAccent,
                                focusedLabelColor = EyeCareAccent,
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f).testTag("set_timer_eye_rest_input")
                        )
                    }
                }

                // Section: Water Break
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "HYDRATION WATER BREAK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = WaterAccent
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = waterIntervalStr,
                            onValueChange = { waterIntervalStr = it },
                            label = { Text("Interval (mins)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WaterAccent,
                                focusedLabelColor = WaterAccent,
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f).testTag("set_timer_water_interval_input")
                        )
                        OutlinedTextField(
                            value = waterBreakStr,
                            onValueChange = { waterBreakStr = it },
                            label = { Text("Break (mins)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WaterAccent,
                                focusedLabelColor = WaterAccent,
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f).testTag("set_timer_water_break_input")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val studyMinVal = studyMinutesStr.toIntOrNull() ?: settings.defaultStudyDurationMinutes
                    val eyeIntVal = eyeIntervalStr.toIntOrNull() ?: settings.eyeStudyIntervalMinutes
                    val eyeRestVal = eyeRestStr.toIntOrNull() ?: settings.eyeRestDurationSeconds
                    val waterIntVal = waterIntervalStr.toIntOrNull() ?: settings.waterIntervalMinutes
                    val waterBreakVal = waterBreakStr.toIntOrNull() ?: (settings.waterBreakDurationSeconds / 60)

                    onSave(studyMinVal, eyeIntVal, eyeRestVal, waterIntVal, waterBreakVal)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudyAccent,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("set_timer_dialog_save")
            ) {
                Text("Save & Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextMuted),
                modifier = Modifier.testTag("set_timer_dialog_cancel")
            ) {
                Text("Cancel")
            }
        },
        containerColor = DarkCard
    )
}

/**
 * LandscapeWatchScreen (Part 4C)
 * Two-column adaptive layout for Home in Landscape mode:
 * - Left column (55%): Big round analog watch dial + actions row
 * - Right column (45%): 3 equal stacked cards (Eye Rest, Water+Stretch, Clock)
 */
@Composable
fun LandscapeWatchScreen(
    snapshot: TimerSnapshot,
    settings: AppSettings,
    subjects: List<SubjectEntity>,
    topics: List<TopicEntity>,
    todayGoal: DailyGoalEntity?,
    todaySessionsCount: Int,
    currentSubjectName: String,
    currentTopicName: String,
    waterStretchLabel: String,
    onOpenSettings: () -> Unit,
    onShowSetTimerDialog: () -> Unit,
    onSubjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val activeDisplay = if (snapshot.state == TimerState.IDLE) {
        val h = settings.defaultStudyDurationMinutes / 60
        val m = settings.defaultStudyDurationMinutes % 60
        String.format(Locale.getDefault(), "%02d:%02d:00", h, m)
    } else {
        snapshot.formattedActiveDisplay()
    }
    val (hhMm, ss) = formatTimeToHHMMSS(activeDisplay)

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT COLUMN (takes about 78% width): Wide Study Timer Card
        LandscapeRectangleTimerCard(
            modeLabel = if (snapshot.state.isBreak) "BREAK" else "STUDY TIMER",
            timeHoursMinutes = hhMm,
            timeSeconds = ss,
            hasProgress = true,
            progressFraction = snapshot.progressFraction,
            isActive = snapshot.state != TimerState.IDLE,
            isRunning = snapshot.state == TimerState.STUDYING,
            isPaused = snapshot.state == TimerState.PAUSED,
            isQuietMode = snapshot.isQuietMode,
            accentColor = StudyAccent,
            useHomeStyle = true,
            onPlayPause = {
                when (snapshot.state) {
                    TimerState.IDLE -> {
                        val sub = subjects.firstOrNull { it.name == currentSubjectName } ?: subjects.firstOrNull()
                        val top = topics.firstOrNull { it.name == currentTopicName } ?: topics.firstOrNull()
                        if (sub != null && top != null) {
                            TimerEngine.startStudySession(sub.id, top.id, sub.name, top.name, TimerType.STUDY_SESSION, settings.defaultStudyDurationMinutes)
                        }
                    }
                    TimerState.STUDYING -> TimerEngine.pause()
                    TimerState.PAUSED -> TimerEngine.resume()
                    else -> {}
                }
            },
            onEndSession = { TimerEngine.stop() },
            onReset = { TimerEngine.reset() },
            onQuietModeToggle = {
                if (snapshot.isQuietMode) TimerEngine.exitQuietMode() else TimerEngine.enterQuietMode()
            },
            onSetTimer = onShowSetTimerDialog,
            modifier = Modifier
                .weight(0.78f)
                .fillMaxHeight()
        )

        // RIGHT COLUMN (about 22% width): 4 equal cards stacked with 8dp gap
        Column(
            modifier = Modifier
                .weight(0.22f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // 1. CLOCK card (equal height)
            LandscapeCurrentClockCard(
                settings = settings,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // 2. EYE REST card (equal height)
            LandscapeEyeRestCard(
                snapshot = snapshot,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // 3. WATER & STRETCH card (equal height)
            LandscapeWaterStretchCard(
                snapshot = snapshot,
                label = waterStretchLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // 4. SUMMARY card (equal height)
            LandscapeBottomStrip(
                todayGoal = todayGoal,
                breaksToday = StretchBreakManager.getTodayBreakCount(),
                subjectName = currentSubjectName,
                topicName = currentTopicName,
                onSubjectClick = onSubjectClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

/**
 * LandscapeAnalogWatch: Big analog-style round watch dial using all available height.
 */
@Composable
fun LandscapeAnalogWatch(
    snapshot: TimerSnapshot,
    settings: AppSettings,
    subjects: List<SubjectEntity>,
    topics: List<TopicEntity>,
    modifier: Modifier = Modifier
) {
    val progress = snapshot.progressFraction

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val sizeDp = minOf(maxWidth, maxHeight)

        Box(
            modifier = Modifier.size(sizeDp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = (size.minDimension / 2f) - 4.dp.toPx()

                // Dial Background surface
                drawCircle(color = Color(0xFF0C0E14), radius = radius)
                drawCircle(color = Color(0xFF222632), radius = radius, style = Stroke(1.5.dp.toPx()))

                // 60 minute tick marks (bold tick every 5 minutes)
                for (i in 0 until 60) {
                    val angleRad = (i * 6.0) * (Math.PI / 180.0)
                    val isBold = i % 5 == 0
                    val tickLen = if (isBold) 8.dp.toPx() else 3.5.dp.toPx()
                    val tickWidth = if (isBold) 2.dp.toPx() else 1.dp.toPx()
                    val tickColor = if (isBold) StudyAccent else Color(0xFF4A4E5C)

                    val startX = cx + ((radius - tickLen) * Math.cos(angleRad)).toFloat()
                    val startY = cy + ((radius - tickLen) * Math.sin(angleRad)).toFloat()
                    val endX = cx + (radius * Math.cos(angleRad)).toFloat()
                    val endY = cy + (radius * Math.sin(angleRad)).toFloat()

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round
                    )
                }

                // Thin progress ring in accent color
                val ringRadius = radius - 11.dp.toPx()
                drawArc(
                    color = Color(0x22FFA640),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(cx - ringRadius, cy - ringRadius),
                    size = Size(ringRadius * 2, ringRadius * 2),
                    style = Stroke(width = 2.5.dp.toPx())
                )
                if (progress > 0f) {
                    drawArc(
                        color = StudyAccent,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        topLeft = Offset(cx - ringRadius, cy - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Moving hand showing elapsed time
                val handAngleRad = (progress * 360.0 - 90.0) * (Math.PI / 180.0)
                val handLen = radius * 0.60f
                val hx = cx + (handLen * Math.cos(handAngleRad)).toFloat()
                val hy = cy + (handLen * Math.sin(handAngleRad)).toFloat()

                drawLine(
                    color = StudyAccent,
                    start = Offset(cx, cy),
                    end = Offset(hx, hy),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = StudyAccent, radius = 4.dp.toPx(), center = Offset(cx, cy))
            }

            // Center Content: Mode label, Digital remaining time HH:MM:SS, Play/Pause button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (snapshot.isQuietMode) "QUIET MODE" else "STUDY TIMER",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (snapshot.isQuietMode) AmberAccent else TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (snapshot.isQuietMode) "QUIET" else snapshot.formattedActiveDisplay(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (snapshot.isQuietMode) AmberAccent else TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                val isStudying = snapshot.state == TimerState.STUDYING
                Surface(
                    onClick = {
                        if (snapshot.isQuietMode) {
                            TimerEngine.exitQuietMode()
                        } else {
                            when (snapshot.state) {
                                TimerState.STUDYING -> TimerEngine.pause()
                                TimerState.PAUSED -> TimerEngine.resume()
                                TimerState.IDLE -> {
                                    val sub = subjects.firstOrNull { it.id == snapshot.subjectId } ?: subjects.firstOrNull() ?: SubjectEntity(name = "English Literature")
                                    val top = topics.firstOrNull { it.subjectId == sub.id } ?: TopicEntity(subjectId = sub.id, name = "Romantic Poetry")
                                    TimerEngine.startStudySession(sub.id, top.id, sub.name, top.name, TimerType.STUDY_SESSION, settings.defaultStudyDurationMinutes)
                                }
                                else -> {}
                            }
                        }
                    },
                    shape = CircleShape,
                    color = if (isStudying) Color(0x33FFA640) else Color(0xFF1E212A),
                    border = BorderStroke(1.dp, if (isStudying) StudyAccent else CharcoalBorder),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isStudying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isStudying) "Pause" else "Play",
                            tint = if (isStudying) StudyAccent else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper to split formatted time into Big HH:MM and small corner :SS digits
 */
fun splitLandscapeDigits(time: String): Pair<String, String> {
    val parts = time.split(":")
    return when (parts.size) {
        3 -> "${parts[0]}:${parts[1]}" to ":${parts[2]}"
        2 -> parts[0] to ":${parts[1]}"
        else -> time to ""
    }
}

/**
 * Right Column Card 1: Current Clock Card (Large bold time, small seconds, date & weekday)
 */
@Composable
fun LandscapeCurrentClockCard(
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    val timeFormat = if (settings.use24HourFormat) "HH:mm" else "hh:mm"
    val amPm = if (settings.use24HourFormat) "" else SimpleDateFormat("a", Locale.US).format(cal.time).uppercase()
    val hhMm = SimpleDateFormat(timeFormat, Locale.getDefault()).format(cal.time)
    val dateWeekday = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(cal.time)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF22140A), Color(0xFF130B05))
                )
            )
            .border(
                BorderStroke(1.dp, Color(0x66FFA640)),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                drawRoundRect(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color(0x28FFA640), Color.Transparent),
                        radius = size.maxDimension * 0.9f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon on the left (about 34dp circle)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFA640)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Clock",
                    tint = AmberAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Two stacked lines
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Line 1: Date
                Text(
                    text = dateWeekday,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    maxLines = 1,
                    softWrap = false
                )
                // Line 2: Time big & bold, with AM/PM next to it
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = hhMm,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (amPm.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = amPm,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Right Column Card 2: Eye Rest Card (dark green gradient + glow, 16dp rounded, 3dp progress bar)
 */
@Composable
fun LandscapeEyeRestCard(
    snapshot: TimerSnapshot,
    modifier: Modifier = Modifier
) {
    val totalEyeIntervalSecs = (TimerEngine.eyeStudyIntervalSeconds).coerceAtLeast(1L)
    val remainingSecs = snapshot.nextEyeRestCountdownSeconds
    val eyeProgress = ((totalEyeIntervalSecs - remainingSecs).toFloat() / totalEyeIntervalSecs.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF091F14), Color(0xFF04120A))
                )
            )
            .border(
                BorderStroke(1.dp, Color(0x6600D675)),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                drawRoundRect(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color(0x2800D675), Color.Transparent),
                        radius = size.maxDimension * 0.9f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon on the left
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x3300D675)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Eye Rest",
                        tint = EyeCareAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Line 1: Eye Rest in, Line 2: Countdown big & bold green
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Eye Rest in",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = snapshot.formattedEyeCountdown(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = EyeCareAccent,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Progress line at bottom (3dp thick, 8dp side padding, 6dp bottom padding)
            LinearProgressIndicator(
                progress = { eyeProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = EyeCareAccent,
                trackColor = Color(0xFF14241B)
            )
        }
    }
}

/**
 * Right Column Card 3: Water + Stretch Card (dark blue gradient + glow, 16dp rounded, 3dp progress bar)
 */
@Composable
fun LandscapeWaterStretchCard(
    snapshot: TimerSnapshot,
    label: String,
    modifier: Modifier = Modifier
) {
    val totalWaterIntervalSecs = (TimerEngine.waterIntervalSeconds).coerceAtLeast(1L)
    val remainingSecs = snapshot.nextWaterCountdownSeconds
    val waterProgress = ((totalWaterIntervalSecs - remainingSecs).toFloat() / totalWaterIntervalSecs.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF091C2E), Color(0xFF040F1A))
                )
            )
            .border(
                BorderStroke(1.dp, Color(0x6600B4FF)),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                drawRoundRect(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color(0x2800B4FF), Color.Transparent),
                        radius = size.maxDimension * 0.9f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon on the left
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x3300B4FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalDrink,
                        contentDescription = "Water Break",
                        tint = WaterAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Line 1: Water & Stretch in, Line 2: Countdown big & bold blue
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Water & Stretch in",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = snapshot.formattedWaterCountdown(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = WaterAccent,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Progress line at bottom (3dp thick, 8dp side padding, 6dp bottom padding)
            LinearProgressIndicator(
                progress = { waterProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = WaterAccent,
                trackColor = Color(0xFF10202F)
            )
        }
    }
}

/**
 * Right Column Item 4: Summary Card (dark orange/brown gradient + glow, 16dp rounded, 3dp progress bar)
 * Icon on left, 3 stacked lines, ">" arrow on far right, opens Subject selection
 */
@Composable
fun LandscapeBottomStrip(
    todayGoal: DailyGoalEntity?,
    breaksToday: Int,
    subjectName: String,
    topicName: String,
    onSubjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMins = todayGoal?.achievedMinutes ?: 0
    val targetMins = todayGoal?.targetMinutes ?: 360
    val summaryProgress = if (targetMins > 0) (totalMins.toFloat() / targetMins.toFloat()).coerceIn(0f, 1f) else 0f

    val subjectTopicPart = if (topicName.isNotBlank() && topicName != "General") {
        "$subjectName • $topicName"
    } else {
        subjectName
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF22140A), Color(0xFF130B05))
                )
            )
            .border(
                BorderStroke(1.dp, Color(0x66FFA640)),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                drawRoundRect(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color(0x28FFA640), Color.Transparent),
                        radius = size.maxDimension * 0.9f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            }
            .clickable(onClick = onSubjectClick)
            .testTag("landscape_bottom_strip")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 8.dp, end = 26.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon on the left (about 34dp circle)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFA640)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Subject and Topic",
                        tint = StudyAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Three stacked lines (tight line spacing, proper sizes per requirement 11)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Study",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        maxLines = 1,
                        softWrap = false,
                        lineHeight = 13.sp
                    )
                    Text(
                        text = "${totalMins}m / ${targetMins}m",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 19.sp
                    )
                    Text(
                        text = "Breaks $breaksToday • $subjectTopicPart",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 12.sp
                    )
                }
            }

            // 6dp gap between last text line and progress line (requirement 10)
            Spacer(modifier = Modifier.height(6.dp))

            // Progress line at very bottom, in its own space (requirement 10)
            LinearProgressIndicator(
                progress = { summaryProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = StudyAccent,
                trackColor = Color(0xFF26190E)
            )
        }

        // ">" arrow on far right of card, vertically centered, aligned properly, never on top of text (requirement 9)
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Select Subject",
            tint = AmberAccent,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .size(18.dp)
        )
    }
}
