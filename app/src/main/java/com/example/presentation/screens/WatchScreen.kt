package com.example.presentation.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.*
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
import com.example.ui.theme.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun WatchScreen(
    snapshot: TimerSnapshot,
    settings: AppSettings,
    subjects: List<SubjectEntity>,
    topics: List<TopicEntity>,
    todayGoal: DailyGoalEntity?,
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        // Main minimal watch layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
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
                TextButton(
                    onClick = { showSetTimerDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = StudyAccent),
                    modifier = Modifier.testTag("watch_set_timer_button")
                ) {
                    Text("Set Timer", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Two Compact Cards: Eye Rest and Water Break
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EyeRestMiniCard(
                    snapshot = snapshot,
                    modifier = Modifier.weight(1f)
                )

                WaterBreakMiniCard(
                    snapshot = snapshot,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. User-Selected Subject & Topic Card
            val currentSubjectName = if (snapshot.state != TimerState.IDLE && snapshot.subjectName.isNotEmpty()) {
                snapshot.subjectName
            } else {
                subjects.firstOrNull()?.name ?: "English Literature"
            }
            val currentTopicName = if (snapshot.state != TimerState.IDLE && snapshot.topicName.isNotEmpty()) {
                snapshot.topicName
            } else {
                topics.firstOrNull()?.name ?: "Romantic Poetry"
            }

            SubjectTopicCard(
                subjectName = currentSubjectName,
                topicName = currentTopicName,
                onClick = { showSubjectPicker = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 7. Footer Motivation Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
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

            Text(
                text = snapshot.formattedActiveDisplay(),
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )

            Text(
                text = "/ ${snapshot.formatPlanned()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
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
                        text = "Water Break in",
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
                    contentDescription = "Subject",
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
                contentDescription = "Select Subject",
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
                text = "Select Study Subject",
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
                        text = "SUBJECT",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = StudyAccent
                    )
                    TextButton(
                        onClick = { showAddSubjectDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = StudyAccent)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Subject", fontSize = 12.sp, color = StudyAccent)
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
                                        contentDescription = "Edit Subject",
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
                                        contentDescription = "Delete Subject",
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
            title = { Text("Add New Subject", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newSubjectName,
                        onValueChange = { newSubjectName = it },
                        label = { Text("Subject Name (e.g. Physics)") },
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
            title = { Text("Add Topic to ${selectedSubject?.name}", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newTopicName,
                    onValueChange = { newTopicName = it },
                    label = { Text("Topic Name (e.g. Thermodynamics)") },
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
                    Text("Add Topic", fontWeight = FontWeight.Bold)
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
            title = { Text("Edit Subject", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editSubjectName,
                        onValueChange = { editSubjectName = it },
                        label = { Text("Subject Name") },
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
            title = { Text("Delete Subject?", color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete \"${subjectToDelete!!.name}\"? This will also delete all its associated topics and study history.",
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
            title = { Text("Edit Topic", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = editTopicName,
                    onValueChange = { editTopicName = it },
                    label = { Text("Topic Name") },
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
            title = { Text("Delete Topic?", color = TextPrimary) },
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
