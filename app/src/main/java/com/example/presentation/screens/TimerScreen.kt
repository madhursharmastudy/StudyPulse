package com.example.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.timer.*
import com.example.data.db.SubjectEntity
import com.example.data.db.TopicEntity
import com.example.presentation.components.SubjectTopicPicker
import com.example.ui.theme.*

@Composable
fun TimerScreen(
    snapshot: TimerSnapshot,
    stopwatchSnapshot: StopwatchSnapshot,
    subjects: List<SubjectEntity>,
    topics: List<TopicEntity>,
    onBackToWatch: () -> Unit = {},
    onOpenManageSubjects: () -> Unit = {},
    onQuickAddSubject: (String) -> Unit = {},
    onQuickAddTopic: (Long, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("SESSION", "POMODORO", "COUNTDOWN", "STOPWATCH", "BREAKS")

    // State for Subject and Topic
    var selectedSubject by remember { mutableStateOf<SubjectEntity?>(subjects.firstOrNull()) }
    var selectedTopicName by remember { mutableStateOf("") }

    // Dialog state for Set Timer
    var showSetTimerDialog by remember { mutableStateOf(false) }

    // State for Session creation
    var sessionDurationMinutes by remember { mutableIntStateOf(50) }

    // State for Pomodoro
    var pomodoroStudyMinutes by remember { mutableIntStateOf(25) }
    var pomodoroBreakMinutes by remember { mutableIntStateOf(5) }
    var pomodoroCycles by remember { mutableIntStateOf(4) }

    // State for Countdown
    var countdownHours by remember { mutableIntStateOf(0) }
    var countdownMinutes by remember { mutableIntStateOf(30) }
    var countdownSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(subjects) {
        if (selectedSubject == null && subjects.isNotEmpty()) {
            selectedSubject = subjects.first()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(16.dp)
    ) {
        // Header (No sliders icon, back button and title only)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            IconButton(
                onClick = onBackToWatch,
                modifier = Modifier.testTag("timer_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Watch",
                    tint = StudyAccent
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "STUDY TIMERS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = StudyAccent
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Tabs Row (5 equal tabs, compact font to avoid truncation)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkCard,
            contentColor = StudyAccent,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = StudyAccent
                    )
                }
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) AmberAccent else TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("timer_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // One-Line Description
        val descriptionText = when (selectedTab) {
            0 -> "Focus on one subject for a fixed time."
            1 -> "Study in cycles with short rests to stay fresh."
            2 -> "Quick timer for any task, with an optional subject."
            3 -> "Counts up. Use it when you do not know how long you will study."
            else -> "Take a planned rest. Eye and water breaks also come automatically."
        }
        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subject/Task & Topic/Sub task picker (for non-Breaks tabs)
        if (selectedTab != 4) {
            SubjectTopicPicker(
                subjects = subjects,
                selectedSubject = selectedSubject,
                onSubjectSelected = { selectedSubject = it },
                topics = topics,
                selectedTopicName = selectedTopicName,
                onTopicSelected = { selectedTopicName = it },
                onOpenManageSubjects = onOpenManageSubjects,
                onQuickAddSubject = onQuickAddSubject,
                onQuickAddTopic = onQuickAddTopic,
                subjectOptional = (selectedTab == 2 || selectedTab == 3)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Slim Strip for Running Timer in Other Tab
        if (selectedTab in 0..2 || selectedTab == 4) {
            if (stopwatchSnapshot.isRunning || stopwatchSnapshot.isPaused) {
                OtherTimerRunningStrip(
                    title = "Stopwatch ${stopwatchSnapshot.formattedDisplay()} ${if (stopwatchSnapshot.isRunning) "running" else "paused"}",
                    isRunning = stopwatchSnapshot.isRunning,
                    accentColor = CyanAccent,
                    onTapText = { selectedTab = 3 },
                    onPauseResume = {
                        if (stopwatchSnapshot.isRunning) StopwatchEngine.pause() else StopwatchEngine.resume()
                    },
                    onStop = { StopwatchEngine.stop() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        if (selectedTab == 3 || selectedTab == 4) {
            if (snapshot.state.isActive) {
                OtherTimerRunningStrip(
                    title = "${snapshot.timerType.displayName} ${snapshot.formattedActiveDisplay()} ${if (snapshot.state == TimerState.STUDYING) "running" else "paused"}",
                    isRunning = (snapshot.state == TimerState.STUDYING),
                    accentColor = AmberAccent,
                    onTapText = {
                        selectedTab = when (snapshot.timerType) {
                            TimerType.POMODORO -> 1
                            TimerType.COUNTDOWN -> 2
                            else -> 0
                        }
                    },
                    onPauseResume = {
                        if (snapshot.state == TimerState.STUDYING) TimerEngine.pause() else TimerEngine.resume()
                    },
                    onStop = { TimerEngine.stop() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Main Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (selectedTab) {
                0 -> { // SESSION
                    val isSessionRunning = snapshot.state.isActive && snapshot.timerType == TimerType.STUDY_SESSION
                    val timeDisplay = if (isSessionRunning) snapshot.formattedActiveDisplay() else formatMinutesToHHMMSS(sessionDurationMinutes)
                    val progressFraction = if (isSessionRunning) snapshot.progressFraction else 0f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TabRoundTimer(
                            modeLabel = "SESSION",
                            subLabel = "${sessionDurationMinutes}m Focus",
                            timeDisplay = timeDisplay,
                            isActive = isSessionRunning,
                            isPaused = (snapshot.state == TimerState.PAUSED && snapshot.timerType == TimerType.STUDY_SESSION),
                            progressFraction = progressFraction,
                            accentColor = AmberAccent,
                            onPlayPause = {
                                if (!isSessionRunning) {
                                    TimerEngine.startStudySession(
                                        subjectId = selectedSubject?.id ?: 0L,
                                        topicId = 0L,
                                        subjectName = selectedSubject?.name ?: "General Study",
                                        topicName = selectedTopicName.ifBlank { "Deep Focus" },
                                        type = TimerType.STUDY_SESSION,
                                        durationMinutes = sessionDurationMinutes
                                    )
                                } else if (snapshot.state == TimerState.STUDYING) {
                                    TimerEngine.pause()
                                } else if (snapshot.state == TimerState.PAUSED) {
                                    TimerEngine.resume()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions Row
                        TimerActionsRow(
                            isActive = isSessionRunning,
                            onSetTimer = { showSetTimerDialog = true },
                            onReset = { TimerEngine.reset() },
                            onEndSession = { TimerEngine.stop() }
                        )
                    }
                }
                1 -> { // POMODORO
                    val isPomoRunning = snapshot.state.isActive && snapshot.timerType == TimerType.POMODORO
                    val timeDisplay = if (isPomoRunning) snapshot.formattedActiveDisplay() else formatMinutesToHHMMSS(pomodoroStudyMinutes)
                    val progressFraction = if (isPomoRunning) snapshot.progressFraction else 0f
                    val pomoPhaseText = if (isPomoRunning) {
                        "${if (snapshot.state.isBreak) "REST" else "FOCUS"} • Cycle ${snapshot.currentPomodoroCycle} / ${snapshot.totalPomodoroCycles}"
                    } else {
                        "Cycle 1 / $pomodoroCycles"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TabRoundTimer(
                            modeLabel = "POMODORO",
                            subLabel = pomoPhaseText,
                            timeDisplay = timeDisplay,
                            isActive = isPomoRunning,
                            isPaused = (snapshot.state == TimerState.PAUSED && snapshot.timerType == TimerType.POMODORO),
                            progressFraction = progressFraction,
                            accentColor = AmberAccent,
                            onPlayPause = {
                                if (!isPomoRunning) {
                                    TimerEngine.startStudySession(
                                        subjectId = selectedSubject?.id ?: 0L,
                                        topicId = 0L,
                                        subjectName = selectedSubject?.name ?: "Pomodoro Study",
                                        topicName = selectedTopicName.ifBlank { "Interval Focus" },
                                        type = TimerType.POMODORO,
                                        pomodoroSettings = PomodoroSettings(
                                            studyMinutes = pomodoroStudyMinutes,
                                            shortBreakMinutes = pomodoroBreakMinutes,
                                            cycles = pomodoroCycles
                                        )
                                    )
                                } else if (snapshot.state == TimerState.STUDYING || snapshot.state.isBreak) {
                                    TimerEngine.pause()
                                } else if (snapshot.state == TimerState.PAUSED) {
                                    TimerEngine.resume()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TimerActionsRow(
                            isActive = isPomoRunning,
                            onSetTimer = { showSetTimerDialog = true },
                            onReset = { TimerEngine.reset() },
                            onEndSession = { TimerEngine.stop() }
                        )
                    }
                }
                2 -> { // COUNTDOWN
                    val isCdRunning = snapshot.state.isActive && snapshot.timerType == TimerType.COUNTDOWN
                    val totalSecs = (countdownHours * 3600) + (countdownMinutes * 60) + countdownSeconds
                    val timeDisplay = if (isCdRunning) snapshot.formattedActiveDisplay() else formatTotalSecondsToDisplay(totalSecs)
                    val progressFraction = if (isCdRunning) snapshot.progressFraction else 0f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TabRoundTimer(
                            modeLabel = "COUNTDOWN",
                            subLabel = formatTotalSecondsToDisplay(totalSecs),
                            timeDisplay = timeDisplay,
                            isActive = isCdRunning,
                            isPaused = (snapshot.state == TimerState.PAUSED && snapshot.timerType == TimerType.COUNTDOWN),
                            progressFraction = progressFraction,
                            accentColor = AmberAccent,
                            onPlayPause = {
                                if (!isCdRunning) {
                                    TimerEngine.startStudySession(
                                        subjectId = selectedSubject?.id ?: 0L,
                                        topicId = 0L,
                                        subjectName = selectedSubject?.name ?: "General Task",
                                        topicName = selectedTopicName.ifBlank { "Countdown Focus" },
                                        type = TimerType.COUNTDOWN,
                                        durationMinutes = if (totalSecs > 0) (totalSecs / 60).coerceAtLeast(1) else 30,
                                        durationSeconds = if (totalSecs > 0) totalSecs else 1800
                                    )
                                } else if (snapshot.state == TimerState.STUDYING) {
                                    TimerEngine.pause()
                                } else if (snapshot.state == TimerState.PAUSED) {
                                    TimerEngine.resume()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TimerActionsRow(
                            isActive = isCdRunning,
                            onSetTimer = { showSetTimerDialog = true },
                            onReset = { TimerEngine.reset() },
                            onEndSession = { TimerEngine.stop() }
                        )
                    }
                }
                3 -> { // STOPWATCH
                    val isSwActive = stopwatchSnapshot.isRunning || stopwatchSnapshot.isPaused
                    val timeDisplay = if (isSwActive || stopwatchSnapshot.elapsedMillis > 0) stopwatchSnapshot.formattedDisplay() else "00:00:00"

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TabRoundTimer(
                            modeLabel = "STOPWATCH",
                            subLabel = if (stopwatchSnapshot.isRunning) "Running" else if (stopwatchSnapshot.isPaused) "Paused" else "Count Up",
                            timeDisplay = timeDisplay,
                            isActive = isSwActive,
                            isPaused = stopwatchSnapshot.isPaused,
                            progressFraction = if (stopwatchSnapshot.isRunning) ((stopwatchSnapshot.elapsedMillis / 1000) % 60) / 60f else 0f,
                            accentColor = CyanAccent,
                            onPlayPause = {
                                if (!stopwatchSnapshot.isRunning && !stopwatchSnapshot.isPaused) {
                                    StopwatchEngine.start(
                                        subjectId = selectedSubject?.id ?: 0L,
                                        subjectName = selectedSubject?.name ?: "Stopwatch",
                                        topicId = 0L,
                                        topicName = selectedTopicName.ifBlank { "Open Study" }
                                    )
                                } else if (stopwatchSnapshot.isRunning) {
                                    StopwatchEngine.pause()
                                } else {
                                    StopwatchEngine.resume()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stopwatch Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSwActive) {
                                TextButton(onClick = { StopwatchEngine.stop() }) {
                                    Text("End Session", color = RoseAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                TextButton(onClick = { StopwatchEngine.reset() }) {
                                    Text("Reset", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                TextButton(onClick = { StopwatchEngine.recordLap() }) {
                                    Text("Lap", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            } else {
                                TextButton(onClick = {
                                    StopwatchEngine.start(
                                        subjectId = selectedSubject?.id ?: 0L,
                                        subjectName = selectedSubject?.name ?: "Stopwatch",
                                        topicId = 0L,
                                        topicName = selectedTopicName.ifBlank { "Open Study" }
                                    )
                                }) {
                                    Text("Start Stopwatch", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        // Laps list below circle
                        if (stopwatchSnapshot.laps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Laps (${stopwatchSnapshot.laps.size})", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(stopwatchSnapshot.laps.asReversed()) { lap ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CharcoalCard)
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Lap ${lap.lapNumber}", color = TextSecondary, fontSize = 12.sp)
                                        Text(
                                            formatMillisToLapDisplay(lap.lapDurationMillis),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanAccent,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> { // BREAKS
                    BreaksTab(
                        onStartBreak = { type, durationSecs ->
                            TimerEngine.startBreak(type, (durationSecs / 60).toInt())
                        }
                    )
                }
            }
        }
    }

    // Set Timer Dialog
    if (showSetTimerDialog) {
        when (selectedTab) {
            0 -> SetSessionTimerDialog(
                currentMinutes = sessionDurationMinutes,
                onDismiss = { showSetTimerDialog = false },
                onApply = { minutes ->
                    sessionDurationMinutes = minutes
                    showSetTimerDialog = false
                }
            )
            1 -> SetPomodoroTimerDialog(
                currentStudy = pomodoroStudyMinutes,
                currentBreak = pomodoroBreakMinutes,
                currentCycles = pomodoroCycles,
                onDismiss = { showSetTimerDialog = false },
                onApply = { s, b, c ->
                    pomodoroStudyMinutes = s
                    pomodoroBreakMinutes = b
                    pomodoroCycles = c
                    showSetTimerDialog = false
                }
            )
            2 -> SetCountdownTimerDialog(
                currentH = countdownHours,
                currentM = countdownMinutes,
                currentS = countdownSeconds,
                onDismiss = { showSetTimerDialog = false },
                onApply = { h, m, s ->
                    countdownHours = h
                    countdownMinutes = m
                    countdownSeconds = s
                    showSetTimerDialog = false
                }
            )
        }
    }
}

@Composable
fun TabRoundTimer(
    modeLabel: String,
    subLabel: String?,
    timeDisplay: String,
    isActive: Boolean,
    isPaused: Boolean,
    progressFraction: Float,
    accentColor: Color,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(190.dp)
            .testTag("tab_round_timer")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Background track
            drawArc(
                color = Color(0xFF1E1E1E),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            // Active progress arc
            val sweep = progressFraction.coerceIn(0f, 1f) * 360f
            if (sweep > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = modeLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 1.sp
            )
            if (!subLabel.isNullOrBlank()) {
                Text(
                    text = subLabel,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeDisplay,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                onClick = onPlayPause,
                shape = CircleShape,
                color = if (isActive && !isPaused) CharcoalElevated else accentColor,
                contentColor = if (isActive && !isPaused) accentColor else Color.Black,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("round_timer_play_pause")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isActive && !isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isActive && !isPaused) "Pause" else "Start/Resume",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TimerActionsRow(
    isActive: Boolean,
    onSetTimer: () -> Unit,
    onReset: () -> Unit,
    onEndSession: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isActive) {
            TextButton(onClick = onEndSession) {
                Text("End Session", color = RoseAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            TextButton(onClick = onReset) {
                Text("Reset", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            TextButton(onClick = onSetTimer) {
                Text("Set Timer", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        } else {
            TextButton(onClick = onSetTimer) {
                Text("Set Timer", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun OtherTimerRunningStrip(
    title: String,
    isRunning: Boolean,
    accentColor: Color,
    onTapText: () -> Unit,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(accentColor)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onTapText() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) accentColor else TextMuted)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onPauseResume,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Pause/Resume",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = RoseAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BreaksTab(
    onStartBreak: (BreakType, Long) -> Unit
) {
    val breakPresets = listOf(
        Triple("Quick Refresh", BreakType.SHORT_BREAK, 5L * 60),
        Triple("Coffee & Stretch", BreakType.SHORT_BREAK, 10L * 60),
        Triple("Meal / Walk Break", BreakType.LONG_BREAK, 20L * 60),
        Triple("Power Nap", BreakType.LONG_BREAK, 30L * 60)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        breakPresets.forEach { (title, type, durationSecs) ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${durationSecs / 60} minutes • ${type.name}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Button(
                        onClick = { onStartBreak(type, durationSecs) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("START")
                    }
                }
            }
        }
    }
}

@Composable
fun SetSessionTimerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    val presetChips = listOf(15, 25, 30, 45, 50, 60, 90)
    var customText by remember { mutableStateOf(currentMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Session Duration", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select minutes:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetChips.take(4).forEach { chip ->
                        FilterChip(
                            selected = (customText == chip.toString()),
                            onClick = { customText = chip.toString() },
                            label = { Text("${chip}m", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberAccent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetChips.drop(4).forEach { chip ->
                        FilterChip(
                            selected = (customText == chip.toString()),
                            onClick = { customText = chip.toString() },
                            label = { Text("${chip}m", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberAccent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it.filter { char -> char.isDigit() } },
                    label = { Text("Custom Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = customText.toIntOrNull() ?: 50
                    onApply(mins.coerceIn(1, 1440))
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
            ) {
                Text("APPLY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        },
        containerColor = CharcoalCard
    )
}

@Composable
fun SetPomodoroTimerDialog(
    currentStudy: Int,
    currentBreak: Int,
    currentCycles: Int,
    onDismiss: () -> Unit,
    onApply: (study: Int, brk: Int, cycles: Int) -> Unit
) {
    val presets = listOf(Pair(25, 5), Pair(50, 10), Pair(90, 15))
    var selectedPreset by remember { mutableStateOf(Pair(currentStudy, currentBreak)) }
    var selectedCycles by remember { mutableIntStateOf(currentCycles) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Pomodoro Intervals", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Study / Rest Duration:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { pair ->
                        val isSel = (selectedPreset == pair)
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedPreset = pair },
                            label = { Text("${pair.first}/${pair.second}m", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberAccent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                Text("Number of Cycles:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 4, 6, 8).forEach { c ->
                        FilterChip(
                            selected = (selectedCycles == c),
                            onClick = { selectedCycles = c },
                            label = { Text("$c", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberAccent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(selectedPreset.first, selectedPreset.second, selectedCycles)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
            ) {
                Text("APPLY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        },
        containerColor = CharcoalCard
    )
}

@Composable
fun SetCountdownTimerDialog(
    currentH: Int,
    currentM: Int,
    currentS: Int,
    onDismiss: () -> Unit,
    onApply: (h: Int, m: Int, s: Int) -> Unit
) {
    var hoursText by remember { mutableStateOf(currentH.toString()) }
    var minutesText by remember { mutableStateOf(currentM.toString()) }
    var secondsText by remember { mutableStateOf(currentS.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Countdown Time", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it.filter { c -> c.isDigit() } },
                    label = { Text("HH") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                Text(":", fontWeight = FontWeight.Bold, color = TextPrimary)
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter { c -> c.isDigit() } },
                    label = { Text("MM") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                Text(":", fontWeight = FontWeight.Bold, color = TextPrimary)
                OutlinedTextField(
                    value = secondsText,
                    onValueChange = { secondsText = it.filter { c -> c.isDigit() } },
                    label = { Text("SS") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = (hoursText.toIntOrNull() ?: 0).coerceIn(0, 99)
                    val m = (minutesText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    val s = (secondsText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    onApply(h, m, s)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
            ) {
                Text("APPLY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        },
        containerColor = CharcoalCard
    )
}

private fun formatMinutesToHHMMSS(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) {
        String.format(java.util.Locale.getDefault(), "%02d:%02d:00", h, m)
    } else {
        String.format(java.util.Locale.getDefault(), "%02d:00", m)
    }
}

private fun formatTotalSecondsToDisplay(totalSecs: Int): String {
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return if (h > 0) {
        String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s)
    }
}

private fun formatMillisToLapDisplay(millis: Long): String {
    val totalSecs = millis / 1000
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
}
