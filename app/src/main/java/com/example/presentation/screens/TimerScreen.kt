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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.timer.*
import com.example.data.db.SubjectEntity
import com.example.data.db.TopicEntity
import com.example.ui.theme.*

@Composable
fun TimerScreen(
    snapshot: TimerSnapshot,
    subjects: List<SubjectEntity>,
    topics: List<TopicEntity>,
    onBackToWatch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("SESSION", "POMODORO", "COUNTDOWN", "STOPWATCH", "BREAKS")

    // State for Session creation
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull() ?: SubjectEntity(name = "English Literature")) }
    var selectedTopicName by remember { mutableStateOf("Romantic Poetry") }
    var sessionDurationMinutes by remember { mutableIntStateOf(50) }
    var sessionNotes by remember { mutableStateOf("") }

    // State for Pomodoro
    var pomodoroStudyMinutes by remember { mutableIntStateOf(25) }
    var pomodoroBreakMinutes by remember { mutableIntStateOf(5) }
    var pomodoroCycles by remember { mutableIntStateOf(4) }

    // State for Countdown
    var countdownMinutes by remember { mutableIntStateOf(30) }

    LaunchedEffect(subjects) {
        if (subjects.isNotEmpty() && selectedSubject.id == 0L) {
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
                color = StudyAccent
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        // Active Timer Hero Card (if running)
        if (snapshot.state != TimerState.IDLE) {
            ActiveHeroTimer(snapshot = snapshot)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> StudySessionTab(
                    snapshot = snapshot,
                    subjects = subjects,
                    selectedSubject = selectedSubject,
                    onSubjectSelected = { selectedSubject = it },
                    topicName = selectedTopicName,
                    onTopicChange = { selectedTopicName = it },
                    durationMinutes = sessionDurationMinutes,
                    onDurationChange = { sessionDurationMinutes = it },
                    notes = sessionNotes,
                    onNotesChange = { sessionNotes = it },
                    onStart = {
                        TimerEngine.startStudySession(
                            subjectId = selectedSubject.id,
                            topicId = 0,
                            subjectName = selectedSubject.name,
                            topicName = selectedTopicName,
                            type = TimerType.STUDY_SESSION,
                            durationMinutes = sessionDurationMinutes,
                            notes = sessionNotes
                        )
                    }
                )
                1 -> PomodoroTab(
                    snapshot = snapshot,
                    studyMinutes = pomodoroStudyMinutes,
                    breakMinutes = pomodoroBreakMinutes,
                    cycles = pomodoroCycles,
                    onPresetSelect = { s, b ->
                        pomodoroStudyMinutes = s
                        pomodoroBreakMinutes = b
                    },
                    onCyclesChange = { pomodoroCycles = it },
                    onStart = {
                        TimerEngine.startStudySession(
                            subjectId = selectedSubject.id,
                            topicId = 0,
                            subjectName = selectedSubject.name,
                            topicName = selectedTopicName,
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
                    countdownMinutes = countdownMinutes,
                    onMinutesChange = { countdownMinutes = it },
                    onStart = {
                        TimerEngine.startStudySession(
                            subjectId = selectedSubject.id,
                            topicId = 0,
                            subjectName = selectedSubject.name,
                            topicName = selectedTopicName,
                            type = TimerType.COUNTDOWN,
                            durationMinutes = countdownMinutes
                        )
                    }
                )
                3 -> StopwatchTab(
                    snapshot = snapshot,
                    onStart = {
                        TimerEngine.startStudySession(
                            subjectId = selectedSubject.id,
                            topicId = 0,
                            subjectName = selectedSubject.name,
                            topicName = "Stopwatch Track",
                            type = TimerType.STOPWATCH
                        )
                    },
                    onLap = { TimerEngine.recordLap() },
                    onStop = { TimerEngine.stop() },
                    onReset = { TimerEngine.reset() }
                )
                4 -> BreaksTab(
                    onStartBreak = { type, duration ->
                        TimerEngine.startBreak(type, duration)
                    }
                )
            }
        }
    }
}

@Composable
fun ActiveHeroTimer(snapshot: TimerSnapshot) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AmberAccent)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${snapshot.timerType.displayName} • ${snapshot.state.name}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AmberAccent
                )
                Text(
                    text = snapshot.subjectName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }

            Text(
                text = snapshot.formattedActiveDisplay(),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (snapshot.state == TimerState.STUDYING) {
                    Button(
                        onClick = { TimerEngine.pause() },
                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalElevated, contentColor = TextPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text("PAUSE")
                    }
                } else if (snapshot.state == TimerState.PAUSED) {
                    Button(
                        onClick = { TimerEngine.resume() },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text("RESUME", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { TimerEngine.stop() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAccent),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text("STOP")
                }
            }
        }
    }
}

@Composable
fun StudySessionTab(
    snapshot: TimerSnapshot,
    subjects: List<SubjectEntity>,
    selectedSubject: SubjectEntity,
    onSubjectSelected: (SubjectEntity) -> Unit,
    topicName: String,
    onTopicChange: (String) -> Unit,
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onStart: () -> Unit
) {
    val presets = listOf(25, 45, 50, 60, 90, 120)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Select Subject", style = MaterialTheme.typography.labelLarge, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                subjects.take(3).forEach { s ->
                    val isSelected = s.id == selectedSubject.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AmberSoft else CharcoalCard)
                            .border(1.dp, if (isSelected) AmberAccent else CharcoalBorder, RoundedCornerShape(8.dp))
                            .clickable { onSubjectSelected(s) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = s.name.take(14),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) AmberAccent else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        item {
            Text("Topic / Goal", style = MaterialTheme.typography.labelLarge, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = topicName,
                onValueChange = onTopicChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("study_session_topic_input"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberAccent,
                    unfocusedBorderColor = CharcoalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        item {
            Text("Duration Preset", style = MaterialTheme.typography.labelLarge, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                presets.forEach { m ->
                    val isSel = durationMinutes == m
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) AmberAccent else CharcoalCard)
                            .border(1.dp, if (isSel) AmberAccent else CharcoalBorder, RoundedCornerShape(8.dp))
                            .clickable { onDurationChange(m) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${m}m",
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.Black else TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Text("Session Notes / Tasks", style = MaterialTheme.typography.labelLarge, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What will you accomplish in this session?", color = TextMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberAccent,
                    unfocusedBorderColor = CharcoalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_session_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("START ${durationMinutes}M STUDY SESSION", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun PomodoroTab(
    snapshot: TimerSnapshot,
    studyMinutes: Int,
    breakMinutes: Int,
    cycles: Int,
    onPresetSelect: (Int, Int) -> Unit,
    onCyclesChange: (Int) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Standard Pomodoro Presets", style = MaterialTheme.typography.labelLarge, color = TextMuted)

        val pomoPresets = listOf(Pair(25, 5), Pair(50, 10), Pair(90, 15))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            pomoPresets.forEach { pair ->
                val isSel = studyMinutes == pair.first && breakMinutes == pair.second
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isSel) AmberSoft else CharcoalCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (isSel) AmberAccent else CharcoalBorder)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPresetSelect(pair.first, pair.second) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${pair.first}/${pair.second}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) AmberAccent else TextPrimary
                        )
                        Text(
                            text = "${pair.first}m Study • ${pair.second}m Rest",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Text("Target Cycles: $cycles Cycles (${cycles * (studyMinutes + breakMinutes)}m total)", style = MaterialTheme.typography.labelLarge, color = TextMuted)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
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

        Spacer(modifier = Modifier.weight(1f))

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

@Composable
fun CountdownTab(
    countdownMinutes: Int,
    onMinutesChange: (Int) -> Unit,
    onStart: () -> Unit
) {
    val presets = listOf(10, 15, 20, 25, 30, 45, 60, 90)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Quick Countdown Durations", style = MaterialTheme.typography.labelLarge, color = TextMuted)

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
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${m}m",
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.Black else TextPrimary,
                            fontSize = 16.sp
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
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${m}m",
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.Black else TextPrimary,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

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

@Composable
fun StopwatchTab(
    snapshot: TimerSnapshot,
    onStart: () -> Unit,
    onLap: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    val isRunning = snapshot.timerType == TimerType.STOPWATCH && snapshot.state == TimerState.STUDYING
    val isPaused = snapshot.timerType == TimerType.STOPWATCH && snapshot.state == TimerState.PAUSED

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Large Stopwatch Elapsed Display
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
                    text = if (snapshot.timerType == TimerType.STOPWATCH) snapshot.formattedActiveDisplay() else "00:00:00",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Text(
                    text = "ACCURATE MONOTONIC TIME",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = TextMuted
                )
            }
        }

        // Stopwatch Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isRunning && !isPaused) {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black),
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
                        onClick = { TimerEngine.pause() },
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
                        onClick = { TimerEngine.resume() },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black),
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

        // Laps Table
        Text("Laps & Splits (${snapshot.laps.size})", style = MaterialTheme.typography.labelLarge, color = TextMuted)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(snapshot.laps) { lap ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CharcoalElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lap ${lap.lapNumber}", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(lap.formatDuration(), color = CyanAccent, fontFamily = FontFamily.Monospace)
                    Text(lap.formatTotal(), color = TextSecondary, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun BreaksTab(
    onStartBreak: (BreakType, Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Manual Rest Breaks", style = MaterialTheme.typography.labelLarge, color = TextMuted)

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalCard),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStartBreak(BreakType.SHORT_BREAK, 5) }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Coffee, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Short Break (5 minutes)", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Quick recharge, stretch, or tea break.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalCard),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStartBreak(BreakType.LONG_BREAK, 15) }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Bedtime, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Long Break (15 minutes)", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Walk around, meditate, or take a mental reset.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalCard),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStartBreak(BreakType.CUSTOM_BREAK, 30) }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Restaurant, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Lunch / Meal Break (30 minutes)", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Step completely away from your study desk.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}
