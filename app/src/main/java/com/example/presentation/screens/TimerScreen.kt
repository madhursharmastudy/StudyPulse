package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("SESSION", "POMODORO", "COUNTDOWN", "STOPWATCH", "BREAKS")

    // State for Subject and Topic
    var selectedSubject by remember { mutableStateOf<SubjectEntity?>(subjects.firstOrNull()) }
    var selectedTopicName by remember { mutableStateOf("") }

    // State for Session creation
    var sessionDurationMinutes by remember { mutableIntStateOf(50) }
    var sessionNotes by remember { mutableStateOf("") }

    // State for Pomodoro
    var pomodoroStudyMinutes by remember { mutableIntStateOf(25) }
    var pomodoroBreakMinutes by remember { mutableIntStateOf(5) }
    var pomodoroCycles by remember { mutableIntStateOf(4) }

    // State for Countdown
    var countdownMinutes by remember { mutableIntStateOf(30) }

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
                color = StudyAccent,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onOpenManageSubjects,
                modifier = Modifier.testTag("manage_subjects_header_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Manage Subjects",
                    tint = AmberAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Dual Running Timers Cards at Top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isTimerActive = snapshot.state.isActive
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTimerActive) CharcoalElevated else CharcoalCard
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isTimerActive) AmberAccent else CharcoalBorder
                    )
                ),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TIMER",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = if (isTimerActive) AmberAccent else TextMuted
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isTimerActive) AmberSoft else Color(0xFF1E1E1E))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isTimerActive) snapshot.state.name else "IDLE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTimerActive) AmberAccent else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isTimerActive) snapshot.formattedActiveDisplay() else "--:--",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isTimerActive) TextPrimary else TextMuted
                    )

                    if (isTimerActive) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = snapshot.subjectName.ifBlank { snapshot.timerType.displayName },
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (snapshot.state == TimerState.STUDYING) {
                                Button(
                                    onClick = { TimerEngine.pause() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalCard, contentColor = TextPrimary),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(28.dp)
                                ) {
                                    Text("Pause", fontSize = 11.sp)
                                }
                            } else if (snapshot.state == TimerState.PAUSED) {
                                Button(
                                    onClick = { TimerEngine.resume() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(28.dp)
                                ) {
                                    Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = { TimerEngine.stop() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAccent),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(28.dp)
                            ) {
                                Text("Stop", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            val isStopwatchActive = stopwatchSnapshot.isRunning || stopwatchSnapshot.isPaused
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isStopwatchActive) CharcoalElevated else CharcoalCard
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isStopwatchActive) CyanAccent else CharcoalBorder
                    )
                ),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STOPWATCH",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = if (isStopwatchActive) CyanAccent else TextMuted
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isStopwatchActive) CyanSoft else Color(0xFF1E1E1E))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stopwatchSnapshot.stateText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isStopwatchActive) CyanAccent else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isStopwatchActive || stopwatchSnapshot.elapsedMillis > 0) stopwatchSnapshot.formattedDisplay() else "00:00:00",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isStopwatchActive) TextPrimary else TextMuted
                    )

                    if (isStopwatchActive) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stopwatchSnapshot.subjectName.ifBlank { "Stopwatch" },
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (stopwatchSnapshot.isRunning) {
                                Button(
                                    onClick = { StopwatchEngine.pause() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalCard, contentColor = TextPrimary),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(28.dp)
                                ) {
                                    Text("Pause", fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = { StopwatchEngine.resume() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(28.dp)
                                ) {
                                    Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = { StopwatchEngine.reset() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(28.dp)
                            ) {
                                Text("Reset", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkCard,
            contentColor = StudyAccent,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = StudyAccent
                )
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
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("timer_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> StudySessionTab(
                    subjects = subjects,
                    selectedSubject = selectedSubject,
                    onSubjectSelected = { selectedSubject = it },
                    topics = topics,
                    topicName = selectedTopicName,
                    onTopicChange = { selectedTopicName = it },
                    durationMinutes = sessionDurationMinutes,
                    onDurationChange = { sessionDurationMinutes = it },
                    notes = sessionNotes,
                    onNotesChange = { sessionNotes = it },
                    onOpenManageSubjects = onOpenManageSubjects,
                    onQuickAddSubject = onQuickAddSubject,
                    onQuickAddTopic = onQuickAddTopic,
                    onStart = {
                        TimerEngine.startStudySession(
                            subjectId = selectedSubject?.id ?: 0L,
                            topicId = 0L,
                            subjectName = selectedSubject?.name ?: "General Study",
                            topicName = selectedTopicName.ifBlank { "Deep Focus" },
                            type = TimerType.STUDY_SESSION,
                            durationMinutes = sessionDurationMinutes,
                            notes = sessionNotes
                        )
                    }
                )
                1 -> PomodoroTab(
                    subjects = subjects,
                    selectedSubject = selectedSubject,
                    onSubjectSelected = { selectedSubject = it },
                    topics = topics,
                    topicName = selectedTopicName,
                    onTopicChange = { selectedTopicName = it },
                    studyMinutes = pomodoroStudyMinutes,
                    breakMinutes = pomodoroBreakMinutes,
                    cycles = pomodoroCycles,
                    onPresetSelect = { s, b ->
                        pomodoroStudyMinutes = s
                        pomodoroBreakMinutes = b
                    },
                    onCyclesChange = { pomodoroCycles = it },
                    onOpenManageSubjects = onOpenManageSubjects,
                    onQuickAddSubject = onQuickAddSubject,
                    onQuickAddTopic = onQuickAddTopic,
                    onStart = {
                        TimerEngine.startStudySession(
                            subjectId = selectedSubject?.id ?: 0L,
                            topicId = 0L,
                            subjectName = selectedSubject?.name ?: "Pomodoro Study",
                            topicName = selectedTopicName.ifBlank { "Focus Interval" },
                            type = TimerType.POMODORO,
                            pomodoroSettings = PomodoroSettings(
                                studyMinutes = pomodoroStudyMinutes,
                                shortBreakMinutes = pomodoroBreakMinutes,
                                cycles = pomodoroCycles
                            )
                        )
                    }
                )
                2 -> CountdownTab(
                    subjects = subjects,
                    selectedSubject = selectedSubject,
                    onSubjectSelected = { selectedSubject = it },
                    topics = topics,
                    topicName = selectedTopicName,
                    onTopicChange = { selectedTopicName = it },
                    countdownMinutes = countdownMinutes,
                    onMinutesChange = { countdownMinutes = it },
                    onOpenManageSubjects = onOpenManageSubjects,
                    onQuickAddSubject = onQuickAddSubject,
                    onQuickAddTopic = onQuickAddTopic,
                    onStart = {
                        TimerEngine.startStudySession(
                            subjectId = selectedSubject?.id ?: 0L,
                            topicId = 0L,
                            subjectName = selectedSubject?.name ?: "General Task",
                            topicName = selectedTopicName.ifBlank { "Countdown Focus" },
                            type = TimerType.COUNTDOWN,
                            durationMinutes = countdownMinutes
                        )
                    }
                )
                3 -> StopwatchTab(
                    stopwatchSnapshot = stopwatchSnapshot,
                    subjects = subjects,
                    selectedSubject = selectedSubject,
                    onSubjectSelected = { selectedSubject = it },
                    topics = topics,
                    topicName = selectedTopicName,
                    onTopicChange = { selectedTopicName = it },
                    onOpenManageSubjects = onOpenManageSubjects,
                    onQuickAddSubject = onQuickAddSubject,
                    onQuickAddTopic = onQuickAddTopic,
                    onStart = {
                        StopwatchEngine.start(
                            subjectId = selectedSubject?.id ?: 0L,
                            subjectName = selectedSubject?.name ?: "Stopwatch",
                            topicId = 0L,
                            topicName = selectedTopicName.ifBlank { "Open Study" }
                        )
                    },
                    onPause = { StopwatchEngine.pause() },
                    onResume = { StopwatchEngine.resume() },
                    onLap = { StopwatchEngine.recordLap() },
                    onStop = { StopwatchEngine.stop() },
                    onReset = { StopwatchEngine.reset() }
                )
                4 -> BreaksTab(
                    onStartBreak = { type, durationSecs ->
                        TimerEngine.startBreak(type, (durationSecs / 60).toInt())
                    }
                )
            }
        }
    }
}

@Composable
fun StudySessionTab(
    subjects: List<SubjectEntity>,
    selectedSubject: SubjectEntity?,
    onSubjectSelected: (SubjectEntity) -> Unit,
    topics: List<TopicEntity>,
    topicName: String,
    onTopicChange: (String) -> Unit,
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onOpenManageSubjects: () -> Unit,
    onQuickAddSubject: (String) -> Unit,
    onQuickAddTopic: (Long, String) -> Unit,
    onStart: () -> Unit
) {
    val presets = listOf(25, 45, 50, 60, 90, 120)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Focus on one subject for a fixed time.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        item {
            SubjectTopicPicker(
                subjects = subjects,
                selectedSubject = selectedSubject,
                onSubjectSelected = onSubjectSelected,
                topics = topics,
                selectedTopicName = topicName,
                onTopicSelected = onTopicChange,
                onOpenManageSubjects = onOpenManageSubjects,
                onQuickAddSubject = onQuickAddSubject,
                onQuickAddTopic = onQuickAddTopic
            )
        }

        item {
            Text("Session Duration (minutes)", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                presets.take(3).forEach { p ->
                    val isSelected = durationMinutes == p
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AmberAccent else CharcoalCard)
                            .clickable { onDurationChange(p) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${p}m",
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                presets.drop(3).forEach { p ->
                    val isSelected = durationMinutes == p
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AmberAccent else CharcoalCard)
                            .clickable { onDurationChange(p) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${p}m",
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextPrimary
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Session Notes / Objectives (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_study_session_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("START ${durationMinutes}M FOCUS SESSION", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PomodoroTab(
    subjects: List<SubjectEntity>,
    selectedSubject: SubjectEntity?,
    onSubjectSelected: (SubjectEntity) -> Unit,
    topics: List<TopicEntity>,
    topicName: String,
    onTopicChange: (String) -> Unit,
    studyMinutes: Int,
    breakMinutes: Int,
    cycles: Int,
    onPresetSelect: (study: Int, brk: Int) -> Unit,
    onCyclesChange: (Int) -> Unit,
    onOpenManageSubjects: () -> Unit,
    onQuickAddSubject: (String) -> Unit,
    onQuickAddTopic: (Long, String) -> Unit,
    onStart: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Study in cycles with short rests to stay fresh.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        item {
            SubjectTopicPicker(
                subjects = subjects,
                selectedSubject = selectedSubject,
                onSubjectSelected = onSubjectSelected,
                topics = topics,
                selectedTopicName = topicName,
                onTopicSelected = onTopicChange,
                onOpenManageSubjects = onOpenManageSubjects,
                onQuickAddSubject = onQuickAddSubject,
                onQuickAddTopic = onQuickAddTopic
            )
        }

        item {
            Text("Pomodoro Cycle Interval", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(Pair(25, 5), Pair(50, 10), Pair(30, 5)).forEach { pair ->
                    val isSel = studyMinutes == pair.first && breakMinutes == pair.second
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) AmberAccent else CharcoalElevated)
                            .clickable { onPresetSelect(pair.first, pair.second) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${pair.first} / ${pair.second}m",
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.Black else TextSecondary
                        )
                    }
                }
            }
        }

        item {
            Text("Number of Cycles", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(2, 4, 6, 8).forEach { c ->
                    val isSel = cycles == c
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) AmberAccent else CharcoalElevated)
                            .clickable { onCyclesChange(c) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$c Cycles",
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.Black else TextSecondary
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_pomodoro_button")
            ) {
                Icon(Icons.Default.Timer, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LAUNCH POMODORO ($studyMinutes / $breakMinutes min)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CountdownTab(
    subjects: List<SubjectEntity>,
    selectedSubject: SubjectEntity?,
    onSubjectSelected: (SubjectEntity) -> Unit,
    topics: List<TopicEntity>,
    topicName: String,
    onTopicChange: (String) -> Unit,
    countdownMinutes: Int,
    onMinutesChange: (Int) -> Unit,
    onOpenManageSubjects: () -> Unit,
    onQuickAddSubject: (String) -> Unit,
    onQuickAddTopic: (Long, String) -> Unit,
    onStart: () -> Unit
) {
    val presets = listOf(10, 15, 20, 25, 30, 45, 60, 90)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Quick timer for any task, with an optional subject.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        item {
            SubjectTopicPicker(
                subjects = subjects,
                selectedSubject = selectedSubject,
                onSubjectSelected = onSubjectSelected,
                topics = topics,
                selectedTopicName = topicName,
                onTopicSelected = onTopicChange,
                onOpenManageSubjects = onOpenManageSubjects,
                onQuickAddSubject = onQuickAddSubject,
                onQuickAddTopic = onQuickAddTopic,
                subjectOptional = true
            )
        }

        item {
            Text("Quick Countdown Durations", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    presets.take(4).forEach { m ->
                        val isSel = countdownMinutes == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) AmberAccent else CharcoalCard)
                                .border(1.dp, if (isSel) AmberAccent else CharcoalBorder, RoundedCornerShape(8.dp))
                                .clickable { onMinutesChange(m) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${m}m",
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else TextPrimary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    presets.drop(4).take(4).forEach { m ->
                        val isSel = countdownMinutes == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) AmberAccent else CharcoalCard)
                                .border(1.dp, if (isSel) AmberAccent else CharcoalBorder, RoundedCornerShape(8.dp))
                                .clickable { onMinutesChange(m) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${m}m",
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else TextPrimary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_countdown_button")
            ) {
                Icon(Icons.Default.HourglassBottom, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("START ${countdownMinutes}M COUNTDOWN", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StopwatchTab(
    stopwatchSnapshot: StopwatchSnapshot,
    subjects: List<SubjectEntity>,
    selectedSubject: SubjectEntity?,
    onSubjectSelected: (SubjectEntity) -> Unit,
    topics: List<TopicEntity>,
    topicName: String,
    onTopicChange: (String) -> Unit,
    onOpenManageSubjects: () -> Unit,
    onQuickAddSubject: (String) -> Unit,
    onQuickAddTopic: (Long, String) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onLap: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    val isRunning = stopwatchSnapshot.isRunning
    val isPaused = stopwatchSnapshot.isPaused

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Counts up. Use it when you do not know how long you will study.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        item {
            SubjectTopicPicker(
                subjects = subjects,
                selectedSubject = selectedSubject,
                onSubjectSelected = onSubjectSelected,
                topics = topics,
                selectedTopicName = topicName,
                onTopicSelected = onTopicChange,
                onOpenManageSubjects = onOpenManageSubjects,
                onQuickAddSubject = onQuickAddSubject,
                onQuickAddTopic = onQuickAddTopic,
                subjectOptional = true
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stopwatchSnapshot.formattedDisplay(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Text(
                        text = "INDEPENDENT RUNNING STOPWATCH",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = CyanAccent
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isRunning && !isPaused) {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("stopwatch_start_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("START STOPWATCH", fontWeight = FontWeight.Bold)
                    }
                } else {
                    if (isRunning) {
                        Button(
                            onClick = onPause,
                            colors = ButtonDefaults.buttonColors(containerColor = CharcoalElevated, contentColor = TextPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("PAUSE")
                        }

                        OutlinedButton(
                            onClick = onLap,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("stopwatch_lap_btn")
                        ) {
                            Text("LAP")
                        }
                    } else {
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("RESUME", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onReset,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("RESET")
                        }
                    }

                    OutlinedButton(
                        onClick = onStop,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("STOP")
                    }
                }
            }
        }

        if (stopwatchSnapshot.laps.isNotEmpty()) {
            item {
                Text("Laps (${stopwatchSnapshot.laps.size})", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
            items(stopwatchSnapshot.laps.asReversed()) { lap ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CharcoalCard)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lap ${lap.lapNumber}", color = TextSecondary, fontWeight = FontWeight.Medium)
                    Text(
                        formatMillisToDisplay(lap.lapDurationMillis),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
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
        Text(
            text = "Take a planned rest. Eye and water breaks also come automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

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

private fun formatMillisToDisplay(millis: Long): String {
    val totalSecs = millis / 1000
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    val ms = (millis % 1000) / 100
    return String.format(java.util.Locale.getDefault(), "%02d:%02d.%d", m, s, ms)
}
