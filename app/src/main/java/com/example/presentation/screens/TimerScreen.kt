package com.example.presentation.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.example.core.timer.*
import com.example.data.db.SubjectEntity
import com.example.data.db.TopicEntity
import com.example.presentation.components.AppBackground
import com.example.presentation.components.AppNoticeDialog
import com.example.presentation.components.LandscapeRectangleTimerCard
import com.example.presentation.components.TimerDigitsDisplay
import com.example.presentation.components.formatTimeToHHMMSS
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.*

/**
 * Timers Screen (Part 4A v2)
 * Features 5 tabs, compact Subject/Task button with bottom sheet,
 * a large unified round timer on all tabs, and dual timer strip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    snapshot: TimerSnapshot,
    stopwatchSnapshot: StopwatchSnapshot,
    subjects: List<SubjectEntity>,
    topics: List<TopicEntity>,
    selectedSubjectId: Long = 0L,
    selectedTopicId: Long = 0L,
    onBackToWatch: () -> Unit = {},
    onOpenManageSubjects: () -> Unit = {},
    onSelectSubject: (SubjectEntity) -> Unit = {},
    onSelectTopic: (TopicEntity) -> Unit = {},
    onQuickAddSubject: (String) -> Unit = {},
    onQuickAddTopic: (Long, String) -> Unit = { _, _ -> },
    onUpdateSubject: (SubjectEntity) -> Unit = {},
    onDeleteSubject: (Long) -> Unit = {},
    onUpdateTopic: (TopicEntity) -> Unit = {},
    onDeleteTopic: (Long) -> Unit = {},
    initialSessionMinutes: Int = 50,
    initialPomodoroStudyMinutes: Int = 25,
    initialPomodoroBreakMinutes: Int = 5,
    initialPomodoroCycles: Int = 4,
    initialCountdownMinutes: Int = 30,
    onSaveSessionMinutes: (Int) -> Unit = {},
    onSavePomodoroSettings: (Int, Int, Int) -> Unit = { _, _, _ -> },
    onSaveCountdownMinutes: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 5 tabs with clean title case
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Session", "Pomo", "Count", "Watch", "Break")

    // Current selected Subject and Topic
    val currentSubject = remember(subjects, selectedSubjectId) {
        subjects.firstOrNull { it.id == selectedSubjectId } ?: subjects.firstOrNull()
    }
    val filteredTopics = remember(topics, currentSubject) {
        currentSubject?.let { sub -> topics.filter { it.subjectId == sub.id } } ?: emptyList()
    }
    val currentTopic = remember(filteredTopics, selectedTopicId) {
        filteredTopics.firstOrNull { it.id == selectedTopicId } ?: filteredTopics.firstOrNull()
    }

    // Bottom sheet state for picking Subject/Task & Topic/Sub task
    var showSubjectSheet by remember { mutableStateOf(false) }

    // Dialog state for Set Timer
    var showSetTimerDialog by remember { mutableStateOf(false) }

    // Session duration (minutes)
    var sessionDurationMinutes by remember(initialSessionMinutes) { mutableIntStateOf(initialSessionMinutes) }

    // Pomodoro settings
    var pomodoroStudyMinutes by remember(initialPomodoroStudyMinutes) { mutableIntStateOf(initialPomodoroStudyMinutes) }
    var pomodoroBreakMinutes by remember(initialPomodoroBreakMinutes) { mutableIntStateOf(initialPomodoroBreakMinutes) }
    var pomodoroCycles by remember(initialPomodoroCycles) { mutableIntStateOf(initialPomodoroCycles) }

    // Countdown settings (hours, minutes, seconds)
    var countdownHours by remember(initialCountdownMinutes) { mutableIntStateOf(initialCountdownMinutes / 60) }
    var countdownMinutes by remember(initialCountdownMinutes) { mutableIntStateOf(initialCountdownMinutes % 60) }
    var countdownSeconds by remember { mutableIntStateOf(0) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val context = LocalContext.current
    var noticeMessage by remember { mutableStateOf<String?>(null) }

    AppBackground(modifier = modifier) {
        if (isLandscape) {
            LandscapeTimerScreen(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                snapshot = snapshot,
                stopwatchSnapshot = stopwatchSnapshot,
                currentSubject = currentSubject,
                currentTopic = currentTopic,
                sessionDurationMinutes = sessionDurationMinutes,
                pomodoroStudyMinutes = pomodoroStudyMinutes,
                pomodoroBreakMinutes = pomodoroBreakMinutes,
                pomodoroCycles = pomodoroCycles,
                countdownHours = countdownHours,
                countdownMinutes = countdownMinutes,
                countdownSeconds = countdownSeconds,
                onOpenSubjectSheet = { showSubjectSheet = true },
                onOpenSetTimerDialog = { showSetTimerDialog = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
        // 1. Header (back button and title only; top cards and sliders icon removed)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            IconButton(
                onClick = onBackToWatch,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("timer_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Watch",
                    tint = if (isExistingTheme(activeTheme)) StudyAccent else tokens.accent
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            val headingColor = if (isExistingTheme(activeTheme)) StudyAccent else tokens.accent
            Text(
                text = "STUDY TIMERS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = headingColor
            )
        }

        // 2. Tabs Row (equal width, single line, 11sp, no cut-off)
        val tabContainer = if (isExistingTheme(activeTheme)) DarkCard else if (activeTheme == "Ultra Premium") Color.Transparent else tokens.cardFill
        val tabAccent = if (isExistingTheme(activeTheme)) StudyAccent else tokens.accent

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = tabContainer,
            contentColor = tabAccent,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = tabAccent
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("timer_tab_$index")
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) (if (isExistingTheme(activeTheme)) AmberAccent else tokens.accent) else (if (isExistingTheme(activeTheme)) TextSecondary else tokens.secondaryText),
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Short One-Line Description
        val descriptionText = when (selectedTab) {
            0 -> "Focus on one subject for a fixed time."
            1 -> "Study in cycles with short rests."
            2 -> "Quick timer for any task."
            3 -> "Counts up. No fixed time."
            else -> "Take a planned rest."
        }
        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Compact Subject/Task Button (48dp high, single line, same on all tabs except Breaks)
        if (selectedTab != 4) {
            val subName = currentSubject?.name
            val topName = currentTopic?.name
            val buttonText = if (!subName.isNullOrBlank() && !topName.isNullOrBlank()) {
                "$subName • $topName"
            } else if (!subName.isNullOrBlank()) {
                subName
            } else {
                "Select Subject/Task"
            }

            OutlinedButton(
                onClick = { showSubjectSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("compact_subject_task_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CharcoalCard,
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, CharcoalBorder),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Subject and Topic",
                        tint = AmberAccent
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 5. Dual Running Timer Strip (T1, T8: slim strip between button and round timer)
        val isCurrentTabTimerActive = when (selectedTab) {
            0 -> snapshot.state.isActive && snapshot.timerType == TimerType.STUDY_SESSION
            1 -> snapshot.state.isActive && snapshot.timerType == TimerType.POMODORO
            2 -> snapshot.state.isActive && snapshot.timerType == TimerType.COUNTDOWN
            else -> false
        }
        val showOtherStopwatch = (stopwatchSnapshot.isRunning || stopwatchSnapshot.isPaused) && selectedTab != 3
        val showOtherTimer = snapshot.state.isActive && !isCurrentTabTimerActive

        if (showOtherStopwatch) {
            val swSecs = stopwatchSnapshot.elapsedMillis / 1000
            val swFormatted = if (swSecs >= 3600) {
                String.format(Locale.getDefault(), "%02d:%02d:%02d", swSecs / 3600, (swSecs % 3600) / 60, swSecs % 60)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", (swSecs % 3600) / 60, swSecs % 60)
            }
            OtherTimerRunningStrip(
                title = "Stopwatch $swFormatted ${if (stopwatchSnapshot.isRunning) "running" else "paused"}",
                isRunning = stopwatchSnapshot.isRunning,
                accentColor = CyanAccent,
                onTapText = { selectedTab = 3 },
                onPauseResume = {
                    if (stopwatchSnapshot.isRunning) StopwatchEngine.pause() else StopwatchEngine.resume()
                },
                onStop = { StopwatchEngine.stop() }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        if (showOtherTimer) {
            val timerSecs = (snapshot.currentSegmentRemainingMillis / 1000).coerceAtLeast(0)
            val timerFormatted = if (timerSecs >= 3600) {
                String.format(Locale.getDefault(), "%02d:%02d:%02d", timerSecs / 3600, (timerSecs % 3600) / 60, timerSecs % 60)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", (timerSecs % 3600) / 60, timerSecs % 60)
            }
            OtherTimerRunningStrip(
                title = "${snapshot.timerType.displayName} $timerFormatted ${if (snapshot.state == TimerState.STUDYING) "running" else "paused"}",
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
            Spacer(modifier = Modifier.height(6.dp))
        }

        // 6. Main Tab Content (Round timer + actions; fixed layout that fits 800dp screen)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (selectedTab) {
                0 -> { // SESSION TAB
                    val isSessionRunning = snapshot.state.isActive && snapshot.timerType == TimerType.STUDY_SESSION
                    val timeDisplay = if (isSessionRunning) snapshot.formattedActiveDisplay() else formatMinutesToHHMMSS(sessionDurationMinutes)
                    val progressFraction = if (isSessionRunning) snapshot.progressFraction else 0f
                    val statusText = if (isSessionRunning) {
                        if (snapshot.state == TimerState.PAUSED) "Session Paused" else "Focusing"
                    } else {
                        "${sessionDurationMinutes}m Focus Session"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TabRoundTimer(
                            modeLabel = "SESSION",
                            statusLine = statusText,
                            timeDisplay = timeDisplay,
                            isActive = isSessionRunning,
                            isPaused = (snapshot.state == TimerState.PAUSED && snapshot.timerType == TimerType.STUDY_SESSION),
                            progressFraction = progressFraction,
                            accentColor = if (isExistingTheme(activeTheme)) AmberAccent else tokens.accent,
                            onPlayPause = {
                                if (!isSessionRunning) {
                                    if (subjects.isEmpty() || currentSubject == null) {
                                        noticeMessage = "No subject found. Please add a subject in Manage Subjects, then press Play again."
                                    } else if (filteredTopics.isEmpty() || currentTopic == null) {
                                        noticeMessage = "No topic found. Please add a topic in Manage Subjects, then press Play again."
                                    } else {
                                        TimerEngine.startStudySession(
                                            subjectId = currentSubject.id,
                                            topicId = currentTopic.id,
                                            subjectName = currentSubject.name,
                                            topicName = currentTopic.name,
                                            type = TimerType.STUDY_SESSION,
                                            durationMinutes = sessionDurationMinutes
                                        )
                                    }
                                } else if (snapshot.state == TimerState.STUDYING) {
                                    TimerEngine.pause()
                                } else if (snapshot.state == TimerState.PAUSED) {
                                    TimerEngine.resume()
                                } else {
                                    Toast.makeText(context, "This action is not available right now.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Target $sessionDurationMinutes min",
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Reserved fixed height action row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TimerActionsRow(
                                isActive = isSessionRunning,
                                onSetTimer = { showSetTimerDialog = true },
                                onReset = { TimerEngine.reset() },
                                onEndSession = { TimerEngine.stop() }
                            )
                        }
                    }
                }
                1 -> { // POMODORO TAB
                    val isPomoRunning = snapshot.state.isActive && snapshot.timerType == TimerType.POMODORO
                    val timeDisplay = if (isPomoRunning) snapshot.formattedActiveDisplay() else formatMinutesToHHMMSS(pomodoroStudyMinutes)
                    val progressFraction = if (isPomoRunning) snapshot.progressFraction else 0f
                    val phase = if (snapshot.state.isBreak) "REST" else "FOCUS"
                    val cycleText = if (isPomoRunning) {
                        "$phase • ${snapshot.currentPomodoroCycle} / ${snapshot.totalPomodoroCycles}"
                    } else {
                        "Cycle 1 / $pomodoroCycles"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TabRoundTimer(
                            modeLabel = "POMODORO",
                            statusLine = cycleText,
                            timeDisplay = timeDisplay,
                            isActive = isPomoRunning,
                            isPaused = (snapshot.state == TimerState.PAUSED && snapshot.timerType == TimerType.POMODORO),
                            progressFraction = progressFraction,
                            accentColor = AmberAccent,
                            onPlayPause = {
                                if (!isPomoRunning) {
                                    if (subjects.isEmpty() || currentSubject == null) {
                                        noticeMessage = "No subject found. Please add a subject in Manage Subjects, then press Play again."
                                    } else if (filteredTopics.isEmpty() || currentTopic == null) {
                                        noticeMessage = "No topic found. Please add a topic in Manage Subjects, then press Play again."
                                    } else {
                                        TimerEngine.startStudySession(
                                            subjectId = currentSubject.id,
                                            topicId = currentTopic.id,
                                            subjectName = currentSubject.name,
                                            topicName = currentTopic.name,
                                            type = TimerType.POMODORO,
                                            pomodoroSettings = PomodoroSettings(
                                                studyMinutes = pomodoroStudyMinutes,
                                                shortBreakMinutes = pomodoroBreakMinutes,
                                                cycles = pomodoroCycles
                                            )
                                        )
                                    }
                                } else if (snapshot.state == TimerState.STUDYING || snapshot.state.isBreak) {
                                    TimerEngine.pause()
                                } else if (snapshot.state == TimerState.PAUSED) {
                                    TimerEngine.resume()
                                } else {
                                    Toast.makeText(context, "This action is not available right now.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Target ${pomodoroStudyMinutes}m study / ${pomodoroBreakMinutes}m rest • $pomodoroCycles cycles",
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TimerActionsRow(
                                isActive = isPomoRunning,
                                onSetTimer = { showSetTimerDialog = true },
                                onReset = { TimerEngine.reset() },
                                onEndSession = { TimerEngine.stop() }
                            )
                        }
                    }
                }
                2 -> { // COUNTDOWN TAB
                    val isCdRunning = snapshot.state.isActive && snapshot.timerType == TimerType.COUNTDOWN
                    val totalSecs = (countdownHours * 3600) + (countdownMinutes * 60) + countdownSeconds
                    val timeDisplay = if (isCdRunning) snapshot.formattedActiveDisplay() else formatTotalSecondsToDisplay(totalSecs)
                    val progressFraction = if (isCdRunning) snapshot.progressFraction else 0f
                    val statusText = if (isCdRunning) {
                        if (snapshot.state == TimerState.PAUSED) "Paused" else "Running"
                    } else {
                        "Quick Countdown"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TabRoundTimer(
                            modeLabel = "COUNTDOWN",
                            statusLine = statusText,
                            timeDisplay = timeDisplay,
                            isActive = isCdRunning,
                            isPaused = (snapshot.state == TimerState.PAUSED && snapshot.timerType == TimerType.COUNTDOWN),
                            progressFraction = progressFraction,
                            accentColor = AmberAccent,
                            onPlayPause = {
                                if (!isCdRunning) {
                                    if (subjects.isEmpty() || currentSubject == null) {
                                        noticeMessage = "No subject found. Please add a subject in Manage Subjects, then press Play again."
                                    } else if (filteredTopics.isEmpty() || currentTopic == null) {
                                        noticeMessage = "No topic found. Please add a topic in Manage Subjects, then press Play again."
                                    } else {
                                        val safeSecs = if (totalSecs > 0) totalSecs else 1800
                                        TimerEngine.startStudySession(
                                            subjectId = currentSubject.id,
                                            topicId = currentTopic.id,
                                            subjectName = currentSubject.name,
                                            topicName = currentTopic.name,
                                            type = TimerType.COUNTDOWN,
                                            durationMinutes = (safeSecs / 60).coerceAtLeast(1),
                                            durationSeconds = safeSecs
                                        )
                                    }
                                } else if (snapshot.state == TimerState.STUDYING) {
                                    TimerEngine.pause()
                                } else if (snapshot.state == TimerState.PAUSED) {
                                    TimerEngine.resume()
                                } else {
                                    Toast.makeText(context, "This action is not available right now.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (totalSecs > 0) "Target ${totalSecs / 60}m ${totalSecs % 60}s" else "Target 30 min",
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TimerActionsRow(
                                isActive = isCdRunning,
                                onSetTimer = { showSetTimerDialog = true },
                                onReset = { TimerEngine.reset() },
                                onEndSession = { TimerEngine.stop() }
                            )
                        }
                    }
                }
                3 -> { // STOPWATCH TAB
                    val isSwActive = stopwatchSnapshot.isRunning || stopwatchSnapshot.isPaused
                    val timeDisplay = if (isSwActive || stopwatchSnapshot.elapsedMillis > 0) stopwatchSnapshot.formattedDisplay() else "00:00:00"
                    val statusText = if (stopwatchSnapshot.isRunning) "Running" else if (stopwatchSnapshot.isPaused) "Paused" else "Counts Up"

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TabRoundTimer(
                            modeLabel = "STOPWATCH",
                            statusLine = statusText,
                            timeDisplay = timeDisplay,
                            isActive = isSwActive,
                            isPaused = stopwatchSnapshot.isPaused,
                            progressFraction = if (isSwActive) ((stopwatchSnapshot.elapsedMillis % 60000L).toFloat() / 60000f) else 0f,
                            accentColor = CyanAccent,
                            hasProgress = true,
                            isStopwatch = true,
                            onPlayPause = {
                                if (!stopwatchSnapshot.isRunning && !stopwatchSnapshot.isPaused) {
                                    if (subjects.isEmpty() || currentSubject == null) {
                                        noticeMessage = "No subject found. Please add a subject in Manage Subjects, then press Play again."
                                    } else if (filteredTopics.isEmpty() || currentTopic == null) {
                                        noticeMessage = "No topic found. Please add a topic in Manage Subjects, then press Play again."
                                    } else {
                                        StopwatchEngine.start(
                                            subjectId = currentSubject.id,
                                            subjectName = currentSubject.name,
                                            topicId = currentTopic.id,
                                            topicName = currentTopic.name
                                        )
                                    }
                                } else if (stopwatchSnapshot.isRunning) {
                                    StopwatchEngine.pause()
                                } else if (stopwatchSnapshot.isPaused) {
                                    StopwatchEngine.resume()
                                } else {
                                    Toast.makeText(context, "This action is not available right now.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stopwatch Actions Row (End Session / Reset / Lap)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSwActive) {
                                if (activeTheme == "Classic") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { StopwatchEngine.stop() },
                                            modifier = Modifier.testTag("stopwatch_end_button")
                                        ) {
                                            Text("End Session", color = RoseAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        TextButton(
                                            onClick = { StopwatchEngine.reset() },
                                            modifier = Modifier.testTag("stopwatch_reset_button")
                                        ) {
                                            Text("Reset", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        TextButton(
                                            onClick = {
                                                if (stopwatchSnapshot.isRunning) {
                                                    StopwatchEngine.recordLap()
                                                } else {
                                                    Toast.makeText(context, "This action is not available right now.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.testTag("stopwatch_lap_button")
                                        ) {
                                            Text("Lap", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ThemedActionPill(
                                            text = "End Session",
                                            onClick = { StopwatchEngine.stop() },
                                            textColor = tokens.danger,
                                            testTag = "stopwatch_end_button"
                                        )
                                        ThemedActionPill(
                                            text = "Reset",
                                            onClick = { StopwatchEngine.reset() },
                                            testTag = "stopwatch_reset_button"
                                        )
                                        ThemedActionPill(
                                            text = "Lap",
                                            onClick = {
                                                if (stopwatchSnapshot.isRunning) {
                                                    StopwatchEngine.recordLap()
                                                } else {
                                                    Toast.makeText(context, "This action is not available right now.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            textColor = CyanAccent,
                                            testTag = "stopwatch_lap_button"
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Tap Play to Start",
                                    color = if (activeTheme == "Classic") TextMuted else tokens.secondaryText,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Laps list (scrollable, only shown in Stopwatch tab)
                        if (stopwatchSnapshot.laps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(stopwatchSnapshot.laps.asReversed()) { lap ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CharcoalCard)
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
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
                4 -> { // BREAKS TAB
                    if (snapshot.state.isBreak) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            TabRoundTimer(
                                modeLabel = "BREAK",
                                statusLine = "Break in Progress",
                                timeDisplay = snapshot.formattedActiveDisplay(),
                                isActive = true,
                                isPaused = (snapshot.state == TimerState.PAUSED),
                                progressFraction = snapshot.progressFraction,
                                accentColor = EmeraldAccent,
                                hasProgress = true,
                                isStopwatch = false,
                                showPlayButton = true,
                                onPlayPause = {
                                    if (snapshot.state == TimerState.PAUSED) {
                                        TimerEngine.resume()
                                    } else if (snapshot.state.isBreak) {
                                        TimerEngine.pause()
                                    } else {
                                        Toast.makeText(context, "This action is not available right now.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { TimerEngine.stop() }) {
                                        Text("End Break", color = RoseAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    TextButton(onClick = { TimerEngine.reset() }) {
                                        Text("Reset", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // When no break is running, show ONLY the three break choices (no duplicate play button)
                        BreaksTab(
                            onStartBreak = { type, durationSecs ->
                                TimerEngine.startBreak(type, (durationSecs / 60).toInt())
                            }
                        )
                    }
                }
            }
        }
        }
    }

    // Bottom Sheet for Subject/Task and Topic/Sub task
    if (showSubjectSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSubjectSheet = false },
            containerColor = if (isExistingTheme(activeTheme)) CharcoalElevated else if (activeTheme == "Ultra Premium") Color(0xFF171D26) else tokens.cardFill,
            dragHandle = { BottomSheetDefaults.DragHandle(color = if (isExistingTheme(activeTheme)) CharcoalBorder else tokens.divider) }
        ) {
            SubjectTopicBottomSheetContent(
                subjects = subjects,
                topics = topics,
                selectedSubject = currentSubject,
                selectedTopic = currentTopic,
                onSelectSubject = { sub ->
                    onSelectSubject(sub)
                },
                onSelectTopic = { top ->
                    onSelectTopic(top)
                },
                onOpenManage = {
                    showSubjectSheet = false
                    onOpenManageSubjects()
                },
                onQuickAddSubject = onQuickAddSubject,
                onQuickAddTopic = onQuickAddTopic,
                onUpdateSubject = onUpdateSubject,
                onDeleteSubject = onDeleteSubject,
                onUpdateTopic = onUpdateTopic,
                onDeleteTopic = onDeleteTopic,
                onDone = { showSubjectSheet = false }
            )
        }
    }

    // Set Timer Dialogs
    if (showSetTimerDialog) {
        when (selectedTab) {
            0 -> SetSessionTimerDialog(
                currentMinutes = sessionDurationMinutes,
                onDismiss = { showSetTimerDialog = false },
                onApply = { minutes ->
                    sessionDurationMinutes = minutes
                    onSaveSessionMinutes(minutes)
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
                    onSavePomodoroSettings(s, b, c)
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
                    val totalMinutes = (h * 60) + m
                    onSaveCountdownMinutes(if (totalMinutes > 0) totalMinutes else 1)
                    showSetTimerDialog = false
                }
            )
        }
    }
    if (noticeMessage != null) {
        AppNoticeDialog(
            message = noticeMessage!!,
            onDismiss = { noticeMessage = null }
        )
    }
}
}

/**
 * Reusable Round Timer (one fixed size ~270dp on all tabs)
 * Stacks mode label, time (shown ONCE), small status line, and large play/pause button.
 */
@Composable
fun TabRoundTimer(
    modeLabel: String,
    statusLine: String,
    timeDisplay: String,
    isActive: Boolean,
    isPaused: Boolean,
    progressFraction: Float,
    accentColor: Color,
    hasProgress: Boolean = true,
    isStopwatch: Boolean = false,
    showPlayButton: Boolean = true,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isClassic = isExistingTheme(activeTheme)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth(0.80f)
            .aspectRatio(1f)
            .testTag("tab_round_timer")
    ) {
        // Circular Progress Ring (T3, T7: matching size, thickness 8dp, position)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val diameter = size.minDimension - strokeWidth - 8.dp.toPx()
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Background track (S3: chip-fill role)
            val trackColor = if (isClassic) Color(0xFF1E1E1E) else tokens.chipFill
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            // Progress arc (fixed timers only, Stopwatch has NO progress)
            if (hasProgress) {
                val sweep = progressFraction.coerceIn(0f, 1f) * 360f
                if (sweep > 0f) {
                    val drawColor = if (isPaused) accentColor.copy(alpha = 0.5f) else accentColor
                    drawArc(
                        color = drawColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Inside content: mode label, time, status line, play/pause button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text(
                text = modeLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Time formatted with big HH:MM and small dim seconds
            val (hhMm, ss) = formatTimeToHHMMSS(timeDisplay)
            TimerDigitsDisplay(
                hoursMinutes = hhMm,
                seconds = ss,
                isStopwatch = isStopwatch,
                fontSize = 38.sp,
                textColor = if (isClassic) Color.White else tokens.text
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = statusLine,
                fontSize = 11.sp,
                color = if (isClassic) TextSecondary else tokens.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Play/Pause button
            if (showPlayButton) {
                Surface(
                    onClick = onPlayPause,
                    shape = CircleShape,
                    color = if (isActive && !isPaused) (if (isClassic) CharcoalCard else tokens.cardFill) else accentColor,
                    contentColor = if (isActive && !isPaused) accentColor else (if (isClassic) Color.Black else tokens.textOnAccent),
                    border = if (isActive && !isPaused) BorderStroke(1.5.dp, accentColor) else null,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("round_timer_play_pause")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isActive && !isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isActive && !isPaused) "Pause" else "Play",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(52.dp))
            }
        }
    }
}

/**
 * Action buttons below the circle:
 * Idle: "Set Timer" centered
 * Active/Paused: "End Session", "Reset", "Set Timer"
 */
@Composable
fun TimerActionsRow(
    isActive: Boolean,
    onSetTimer: () -> Unit,
    onReset: () -> Unit,
    onEndSession: () -> Unit
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isClassic = activeTheme == "Classic"

    if (isClassic) {
        if (isActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEndSession) {
                    Text("End Session", color = RoseAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                TextButton(onClick = onReset) {
                    Text("Reset", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                TextButton(onClick = onSetTimer) {
                    Text("Set Timer", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        } else {
            TextButton(onClick = onSetTimer) {
                Text("Set Timer", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    } else {
        if (isActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemedActionPill(
                    text = "End Session",
                    onClick = onEndSession,
                    textColor = tokens.danger
                )
                ThemedActionPill(
                    text = "Reset",
                    onClick = onReset
                )
                ThemedActionPill(
                    text = "Set Timer",
                    onClick = onSetTimer,
                    textColor = tokens.accent
                )
            }
        } else {
            ThemedActionPill(
                text = "Set Timer",
                onClick = onSetTimer,
                textColor = tokens.accent
            )
        }
    }
}

/**
 * Slim strip shown when another timer is running in the background.
 */
@Composable
fun OtherTimerRunningStrip(
    title: String,
    isRunning: Boolean,
    accentColor: Color,
    onTapText: () -> Unit,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isClassic = isExistingTheme(activeTheme)

    val cardShape = if (isClassic) RoundedCornerShape(8.dp) else if (activeTheme == "Ultra Premium") RoundedCornerShape(16.dp) else RoundedCornerShape(14.dp)
    val cardBg = if (isClassic) CharcoalElevated else if (activeTheme == "Ultra Premium") UltraPremiumGlass else tokens.cardFill
    val cardBorder = if (isClassic) BorderStroke(1.dp, accentColor) else null

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
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
                        .background(if (isRunning) accentColor else (if (isClassic) TextMuted else tokens.secondaryText))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isClassic) TextPrimary else tokens.text,
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

/**
 * Bottom Sheet content for Subject/Task and Topic/Sub task selection, inline adding,
 * renaming, and deleting.
 */
@Composable
fun SubjectTopicBottomSheetContent(
    subjects: List<SubjectEntity>,
    topics: List<TopicEntity>,
    selectedSubject: SubjectEntity?,
    selectedTopic: TopicEntity?,
    onSelectSubject: (SubjectEntity) -> Unit,
    onSelectTopic: (TopicEntity) -> Unit,
    onOpenManage: () -> Unit,
    onQuickAddSubject: (String) -> Unit,
    onQuickAddTopic: (Long, String) -> Unit,
    onUpdateSubject: (SubjectEntity) -> Unit,
    onDeleteSubject: (Long) -> Unit,
    onUpdateTopic: (TopicEntity) -> Unit,
    onDeleteTopic: (Long) -> Unit,
    onDone: () -> Unit
) {
    var subjectExpanded by remember { mutableStateOf(false) }
    var topicExpanded by remember { mutableStateOf(false) }

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var topicToEdit by remember { mutableStateOf<TopicEntity?>(null) }

    val currentFilteredTopics = remember(topics, selectedSubject) {
        selectedSubject?.let { sub -> topics.filter { it.subjectId == sub.id } } ?: emptyList()
    }

    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isClassic = isExistingTheme(activeTheme)
    val textColor = if (isClassic) TextPrimary else tokens.text
    val subTextColor = if (isClassic) TextSecondary else tokens.secondaryText
    val accentColor = if (isClassic) AmberAccent else tokens.accent
    val cardBg = if (isClassic) CharcoalCard else if (activeTheme == "Ultra Premium") Color(0x1AFFFFFF) else tokens.cardFill
    val borderColor = if (isClassic) CharcoalBorder else tokens.divider
    val dialogBg = if (isClassic) CharcoalCard else if (activeTheme == "Ultra Premium") Color(0xFF171D26) else tokens.cardFill
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Subject/Task",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textColor
            )
            TextButton(onClick = onOpenManage) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Manage",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Manage", color = accentColor, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Subject/Task Selector
        Text(
            text = "Subject/Task",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedCard(
                onClick = { subjectExpanded = true },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedSubject?.name ?: "Select Subject/Task",
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = subTextColor)
                }
            }

            DropdownMenu(
                expanded = subjectExpanded,
                onDismissRequest = { subjectExpanded = false },
                modifier = Modifier
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            ) {
                subjects.forEach { subject ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subject.name,
                                    color = if (subject.id == selectedSubject?.id) accentColor else textColor,
                                    fontWeight = if (subject.id == selectedSubject?.id) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            subjectExpanded = false
                                            subjectToEdit = subject
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = subTextColor, modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            onDeleteSubject(subject.id)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseAccent, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        },
                        onClick = {
                            onSelectSubject(subject)
                            subjectExpanded = false
                        }
                    )
                }
                HorizontalDivider(color = borderColor)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Add new Subject/Task...", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    onClick = {
                        subjectExpanded = false
                        showAddSubjectDialog = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Topic/Sub task Selector
        Text(
            text = "Topic/Sub task",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedCard(
                onClick = { topicExpanded = true },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedTopic?.name ?: "Select or Add Topic/Sub task",
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = subTextColor)
                }
            }

            DropdownMenu(
                expanded = topicExpanded,
                onDismissRequest = { topicExpanded = false },
                modifier = Modifier
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            ) {
                currentFilteredTopics.forEach { topic ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = topic.name,
                                    color = if (topic.id == selectedTopic?.id) accentColor else textColor,
                                    fontWeight = if (topic.id == selectedTopic?.id) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            topicExpanded = false
                                            topicToEdit = topic
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = subTextColor, modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            onDeleteTopic(topic.id)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseAccent, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        },
                        onClick = {
                            onSelectTopic(topic)
                            topicExpanded = false
                        }
                    )
                }
                HorizontalDivider(color = borderColor)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Add new Topic/Sub task...", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    onClick = {
                        topicExpanded = false
                        showAddTopicDialog = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Done button
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = if (isClassic) Color.Black else tokens.textOnAccent),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("DONE", fontWeight = FontWeight.Bold)
        }
    }

    // Add Subject Dialog
    if (showAddSubjectDialog) {
        var newSubName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("Add Subject/Task", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSubName,
                    onValueChange = { newSubName = it },
                    label = { Text("Subject/Task Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubName.isNotBlank()) {
                            onQuickAddSubject(newSubName.trim())
                            showAddSubjectDialog = false
                        } else {
                            Toast.makeText(context, "Please fill the required fields.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = if (isClassic) Color.Black else tokens.textOnAccent)
                ) {
                    Text("ADD", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubjectDialog = false }) {
                    Text("CANCEL", color = subTextColor)
                }
            },
            containerColor = dialogBg
        )
    }

    // Rename Subject Dialog
    subjectToEdit?.let { sub ->
        var editName by remember { mutableStateOf(sub.name) }
        AlertDialog(
            onDismissRequest = { subjectToEdit = null },
            title = { Text("Rename Subject/Task", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onUpdateSubject(sub.copy(name = editName.trim()))
                            subjectToEdit = null
                        } else {
                            Toast.makeText(context, "Please fill the required fields.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = if (isClassic) Color.Black else tokens.textOnAccent)
                ) {
                    Text("SAVE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToEdit = null }) {
                    Text("CANCEL", color = subTextColor)
                }
            },
            containerColor = dialogBg
        )
    }

    // Add Topic Dialog
    if (showAddTopicDialog && selectedSubject != null) {
        var newTopName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            title = { Text("Add Topic/Sub task to ${selectedSubject.name}", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTopName,
                    onValueChange = { newTopName = it },
                    label = { Text("Topic/Sub task Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTopName.isNotBlank()) {
                            onQuickAddTopic(selectedSubject.id, newTopName.trim())
                            showAddTopicDialog = false
                        } else {
                            Toast.makeText(context, "Please fill the required fields.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = if (isClassic) Color.Black else tokens.textOnAccent)
                ) {
                    Text("ADD", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTopicDialog = false }) {
                    Text("CANCEL", color = subTextColor)
                }
            },
            containerColor = dialogBg
        )
    }

    // Rename Topic Dialog
    topicToEdit?.let { top ->
        var editTopName by remember { mutableStateOf(top.name) }
        AlertDialog(
            onDismissRequest = { topicToEdit = null },
            title = { Text("Rename Topic/Sub task", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editTopName,
                    onValueChange = { editTopName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTopName.isNotBlank()) {
                            onUpdateTopic(top.copy(name = editTopName.trim()))
                            topicToEdit = null
                        } else {
                            Toast.makeText(context, "Please fill the required fields.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = if (isClassic) Color.Black else tokens.textOnAccent)
                ) {
                    Text("SAVE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToEdit = null }) {
                    Text("CANCEL", color = subTextColor)
                }
            },
            containerColor = dialogBg
        )
    }
}

/**
 * Breaks Tab: planned breaks (Part 5 item 8).
 * Show ONLY the three break choices (Short 5 min, Long 15 min, Lunch 30 min),
 * each as a compact transparent outline box with its icon, name, minutes and one START button.
 * Box height about 56-64dp.
 */
@Composable
fun BreaksTab(
    onStartBreak: (BreakType, Long) -> Unit
) {
    val breakPresets = listOf(
        Triple("Short", 5, BreakType.SHORT_BREAK),
        Triple("Long", 15, BreakType.LONG_BREAK),
        Triple("Lunch", 30, BreakType.LONG_BREAK)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        breakPresets.forEach { (name, mins, type) ->
            val icon = when (name) {
                "Short" -> Icons.Default.Coffee
                "Long" -> Icons.Default.SelfImprovement
                else -> Icons.Default.Restaurant
            }
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, CharcoalBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0x2200D675)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = "$name Break",
                                tint = EmeraldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        val activeTheme = LocalActiveTheme.current
                        val tokens = LocalAppThemeTokens.current
                        val isClassic = activeTheme == "Classic"

                        Column {
                            Text(
                                text = "$name Break",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isClassic) TextPrimary else tokens.text
                            )
                            Text(
                                text = "$mins min",
                                fontSize = 11.sp,
                                color = if (isClassic) TextSecondary else tokens.secondaryText
                            )
                        }
                    }

                    val activeTheme = LocalActiveTheme.current
                    if (activeTheme == "Classic") {
                        Button(
                            onClick = { onStartBreak(type, mins * 60L) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("START", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        ThemedActionPill(
                            text = "START",
                            onClick = { onStartBreak(type, mins * 60L) },
                            isFilled = true,
                            testTag = "start_break_${name.lowercase()}"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Set Timer Dialog for Session
 */
@Composable
fun SetSessionTimerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isClassic = isExistingTheme(activeTheme)
    val dialogBg = if (isClassic) CharcoalCard else if (activeTheme == "Ultra Premium") Color(0xFF171D26) else tokens.cardFill
    val titleColor = if (isClassic) TextPrimary else tokens.text
    val bodyColor = if (isClassic) TextSecondary else tokens.secondaryText
    val accentCol = if (isClassic) AmberAccent else tokens.accent
    val context = LocalContext.current

    val presetChips = listOf(15, 25, 30, 45, 50, 60, 90)
    var customText by remember { mutableStateOf(currentMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Session Duration", color = titleColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select minutes:", style = MaterialTheme.typography.bodyMedium, color = bodyColor)
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
                                selectedContainerColor = accentCol,
                                selectedLabelColor = if (isClassic) Color.Black else tokens.textOnAccent
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
                                selectedContainerColor = accentCol,
                                selectedLabelColor = if (isClassic) Color.Black else tokens.textOnAccent
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
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = titleColor, unfocusedTextColor = titleColor)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = customText.toIntOrNull()
                    if (mins == null || mins <= 0) {
                        Toast.makeText(context, "Please enter a valid number of minutes.", Toast.LENGTH_SHORT).show()
                    } else {
                        onApply(mins.coerceIn(1, 1440))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentCol, contentColor = if (isClassic) Color.Black else tokens.textOnAccent)
            ) {
                Text("APPLY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = bodyColor)
            }
        },
        containerColor = dialogBg
    )
}

/**
 * Set Timer Dialog for Pomodoro
 */
@Composable
fun SetPomodoroTimerDialog(
    currentStudy: Int,
    currentBreak: Int,
    currentCycles: Int,
    onDismiss: () -> Unit,
    onApply: (study: Int, brk: Int, cycles: Int) -> Unit
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isClassic = isExistingTheme(activeTheme)
    val dialogBg = if (isClassic) CharcoalCard else if (activeTheme == "Ultra Premium") Color(0xFF171D26) else tokens.cardFill
    val titleColor = if (isClassic) TextPrimary else tokens.text
    val bodyColor = if (isClassic) TextSecondary else tokens.secondaryText
    val accentCol = if (isClassic) AmberAccent else tokens.accent

    val presets = listOf(Pair(25, 5), Pair(50, 10), Pair(90, 15))
    var selectedPreset by remember { mutableStateOf(Pair(currentStudy, currentBreak)) }
    var selectedCycles by remember { mutableIntStateOf(currentCycles) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Pomodoro Intervals", color = titleColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Study / Rest Duration:", style = MaterialTheme.typography.bodyMedium, color = bodyColor)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { pair ->
                        val isSel = (selectedPreset == pair)
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedPreset = pair },
                            label = { Text("${pair.first}/${pair.second}m", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentCol,
                                selectedLabelColor = if (isClassic) Color.Black else tokens.textOnAccent
                            )
                        )
                    }
                }
                Text("Number of Cycles:", style = MaterialTheme.typography.bodyMedium, color = bodyColor)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 4, 6, 8).forEach { c ->
                        FilterChip(
                            selected = (selectedCycles == c),
                            onClick = { selectedCycles = c },
                            label = { Text("$c", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentCol,
                                selectedLabelColor = if (isClassic) Color.Black else tokens.textOnAccent
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
                colors = ButtonDefaults.buttonColors(containerColor = accentCol, contentColor = if (isClassic) Color.Black else tokens.textOnAccent)
            ) {
                Text("APPLY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = bodyColor)
            }
        },
        containerColor = dialogBg
    )
}

/**
 * Set Timer Dialog for Countdown
 */
@Composable
fun SetCountdownTimerDialog(
    currentH: Int,
    currentM: Int,
    currentS: Int,
    onDismiss: () -> Unit,
    onApply: (h: Int, m: Int, s: Int) -> Unit
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isClassic = isExistingTheme(activeTheme)
    val dialogBg = if (isClassic) CharcoalCard else if (activeTheme == "Ultra Premium") Color(0xFF171D26) else tokens.cardFill
    val titleColor = if (isClassic) TextPrimary else tokens.text
    val bodyColor = if (isClassic) TextSecondary else tokens.secondaryText
    val accentCol = if (isClassic) AmberAccent else tokens.accent
    val context = LocalContext.current

    var hoursText by remember { mutableStateOf(currentH.toString()) }
    var minutesText by remember { mutableStateOf(currentM.toString()) }
    var secondsText by remember { mutableStateOf(currentS.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Countdown Time", color = titleColor, fontWeight = FontWeight.Bold) },
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
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = titleColor, unfocusedTextColor = titleColor)
                )
                Text(":", fontWeight = FontWeight.Bold, color = titleColor)
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter { c -> c.isDigit() } },
                    label = { Text("MM") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = titleColor, unfocusedTextColor = titleColor)
                )
                Text(":", fontWeight = FontWeight.Bold, color = titleColor)
                OutlinedTextField(
                    value = secondsText,
                    onValueChange = { secondsText = it.filter { c -> c.isDigit() } },
                    label = { Text("SS") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = titleColor, unfocusedTextColor = titleColor)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = (hoursText.toIntOrNull() ?: 0).coerceIn(0, 99)
                    val m = (minutesText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    val s = (secondsText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    val totalSecs = (h * 3600) + (m * 60) + s
                    if (totalSecs <= 0) {
                        Toast.makeText(context, "Please enter a valid number of minutes.", Toast.LENGTH_SHORT).show()
                    } else {
                        onApply(h, m, s)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentCol, contentColor = if (isClassic) Color.Black else tokens.textOnAccent)
            ) {
                Text("APPLY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = bodyColor)
            }
        },
        containerColor = dialogBg
    )
}

private fun formatMinutesToHHMMSS(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d:00", h, m)
}

private fun formatTotalSecondsToDisplay(totalSecs: Int): String {
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
}

private fun formatMillisToLapDisplay(millis: Long): String {
    val totalSecs = millis / 1000
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
}

/**
 * LandscapeTimerScreen (Part 4E)
 * Two-column layout for Timers screen in landscape mode:
 * - Left (57%): Big rectangle timer card with perimeter progress border
 * - Right (43%): Short tabs row ("Session", "Pomo", "Count", "Watch", "Break"),
 *   one-line description, compact Subject/Task button, dual timer strip,
 *   and tab-specific content (Lap list for Stopwatch, 3 break choices for Breaks).
 */
@Composable
fun LandscapeTimerScreen(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    snapshot: TimerSnapshot,
    stopwatchSnapshot: StopwatchSnapshot,
    currentSubject: SubjectEntity?,
    currentTopic: TopicEntity?,
    sessionDurationMinutes: Int,
    pomodoroStudyMinutes: Int,
    pomodoroBreakMinutes: Int,
    pomodoroCycles: Int,
    countdownHours: Int,
    countdownMinutes: Int,
    countdownSeconds: Int,
    onOpenSubjectSheet: () -> Unit,
    onOpenSetTimerDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val context = LocalContext.current
    var noticeMessage by remember { mutableStateOf<String?>(null) }
    val totalCdSecs = (countdownHours * 3600) + (countdownMinutes * 60) + countdownSeconds

    // Tab labels and metadata
    val tabTitles = listOf("Session", "Pomo", "Count", "Watch", "Break")
    val descriptionText = when (selectedTab) {
        0 -> "Focus on one subject for a fixed time."
        1 -> "Study in cycles with short rests."
        2 -> "Quick timer for any task."
        3 -> "Counts up. No fixed time."
        else -> "Take a planned rest."
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT COLUMN (about 57%): Big rectangle timer card for the selected tab
        when (selectedTab) {
            0 -> { // SESSION
                val isSessionRunning = snapshot.state.isActive && snapshot.timerType == TimerType.STUDY_SESSION
                val timeDisplay = if (isSessionRunning) snapshot.formattedActiveDisplay() else formatMinutesToHHMMSS(sessionDurationMinutes)
                val (hhMm, ss) = formatTimeToHHMMSS(timeDisplay)
                LandscapeRectangleTimerCard(
                    modeLabel = "SESSION",
                    timeHoursMinutes = hhMm,
                    timeSeconds = ss,
                    hasProgress = true,
                    progressFraction = if (isSessionRunning) snapshot.progressFraction else 0f,
                    isActive = isSessionRunning,
                    isRunning = snapshot.state == TimerState.STUDYING && snapshot.timerType == TimerType.STUDY_SESSION,
                    isPaused = snapshot.state == TimerState.PAUSED && snapshot.timerType == TimerType.STUDY_SESSION,
                    isQuietMode = snapshot.isQuietMode,
                    accentColor = if (isExistingTheme(activeTheme)) AmberAccent else tokens.accent,
                    onPlayPause = {
                        if (!isSessionRunning) {
                            if (currentSubject == null) {
                                noticeMessage = "No subject found. Please add a subject in Manage Subjects, then press Play again."
                            } else if (currentTopic == null) {
                                noticeMessage = "No topic found. Please add a topic in Manage Subjects, then press Play again."
                            } else {
                                TimerEngine.startStudySession(
                                    subjectId = currentSubject.id,
                                    topicId = currentTopic.id,
                                    subjectName = currentSubject.name,
                                    topicName = currentTopic.name,
                                    type = TimerType.STUDY_SESSION,
                                    durationMinutes = sessionDurationMinutes
                                )
                            }
                        } else if (snapshot.state == TimerState.STUDYING) {
                            TimerEngine.pause()
                        } else if (snapshot.state == TimerState.PAUSED) {
                            TimerEngine.resume()
                        } else {
                            Toast.makeText(context, "This action is not available right now.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEndSession = { TimerEngine.stop() },
                    onReset = { TimerEngine.reset() },
                    onQuietModeToggle = {
                        if (snapshot.isQuietMode) TimerEngine.exitQuietMode() else TimerEngine.enterQuietMode()
                    },
                    onSetTimer = onOpenSetTimerDialog,
                    modifier = Modifier
                        .weight(0.57f)
                        .fillMaxHeight()
                )
            }
            1 -> { // POMODORO
                val isPomoRunning = snapshot.state.isActive && snapshot.timerType == TimerType.POMODORO
                val timeDisplay = if (isPomoRunning) snapshot.formattedActiveDisplay() else formatMinutesToHHMMSS(pomodoroStudyMinutes)
                val (hhMm, ss) = formatTimeToHHMMSS(timeDisplay)
                val phase = if (snapshot.state.isBreak) "REST" else "FOCUS"
                val modeLabel = if (isPomoRunning) {
                    "POMODORO • $phase ${snapshot.currentPomodoroCycle} / ${snapshot.totalPomodoroCycles}"
                } else {
                    "POMODORO • CYCLE 1 / $pomodoroCycles"
                }
                LandscapeRectangleTimerCard(
                    modeLabel = modeLabel,
                    timeHoursMinutes = hhMm,
                    timeSeconds = ss,
                    hasProgress = true,
                    progressFraction = if (isPomoRunning) snapshot.progressFraction else 0f,
                    isActive = isPomoRunning,
                    isRunning = (snapshot.state == TimerState.STUDYING || snapshot.state.isBreak) && snapshot.timerType == TimerType.POMODORO,
                    isPaused = snapshot.state == TimerState.PAUSED && snapshot.timerType == TimerType.POMODORO,
                    isQuietMode = snapshot.isQuietMode,
                    accentColor = AmberAccent,
                    onPlayPause = {
                        if (!isPomoRunning) {
                            if (currentSubject == null) {
                                noticeMessage = "No subject found. Please add a subject in Manage Subjects, then press Play again."
                            } else if (currentTopic == null) {
                                noticeMessage = "No topic found. Please add a topic in Manage Subjects, then press Play again."
                            } else {
                                TimerEngine.startStudySession(
                                    subjectId = currentSubject.id,
                                    topicId = currentTopic.id,
                                    subjectName = currentSubject.name,
                                    topicName = currentTopic.name,
                                    type = TimerType.POMODORO,
                                    pomodoroSettings = PomodoroSettings(
                                        studyMinutes = pomodoroStudyMinutes,
                                        shortBreakMinutes = pomodoroBreakMinutes,
                                        cycles = pomodoroCycles
                                    )
                                )
                            }
                        } else if (snapshot.state == TimerState.STUDYING || snapshot.state.isBreak) {
                            TimerEngine.pause()
                        } else if (snapshot.state == TimerState.PAUSED) {
                            TimerEngine.resume()
                        } else {
                            Toast.makeText(context, "This action is not available right now.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEndSession = { TimerEngine.stop() },
                    onReset = { TimerEngine.reset() },
                    onQuietModeToggle = {
                        if (snapshot.isQuietMode) TimerEngine.exitQuietMode() else TimerEngine.enterQuietMode()
                    },
                    onSetTimer = onOpenSetTimerDialog,
                    modifier = Modifier
                        .weight(0.57f)
                        .fillMaxHeight()
                )
            }
            2 -> { // COUNTDOWN
                val isCdRunning = snapshot.state.isActive && snapshot.timerType == TimerType.COUNTDOWN
                val timeDisplay = if (isCdRunning) snapshot.formattedActiveDisplay() else formatTotalSecondsToDisplay(totalCdSecs)
                val (hhMm, ss) = formatTimeToHHMMSS(timeDisplay)
                LandscapeRectangleTimerCard(
                    modeLabel = "COUNTDOWN",
                    timeHoursMinutes = hhMm,
                    timeSeconds = ss,
                    hasProgress = true,
                    progressFraction = if (isCdRunning) snapshot.progressFraction else 0f,
                    isActive = isCdRunning,
                    isRunning = snapshot.state == TimerState.STUDYING && snapshot.timerType == TimerType.COUNTDOWN,
                    isPaused = snapshot.state == TimerState.PAUSED && snapshot.timerType == TimerType.COUNTDOWN,
                    isQuietMode = snapshot.isQuietMode,
                    accentColor = AmberAccent,
                    onPlayPause = {
                        if (!isCdRunning) {
                            if (currentSubject == null) {
                                noticeMessage = "No subject found. Please add a subject in Manage Subjects, then press Play again."
                            } else if (currentTopic == null) {
                                noticeMessage = "No topic found. Please add a topic in Manage Subjects, then press Play again."
                            } else {
                                val safeSecs = if (totalCdSecs > 0) totalCdSecs else 1800
                                TimerEngine.startStudySession(
                                    subjectId = currentSubject.id,
                                    topicId = currentTopic.id,
                                    subjectName = currentSubject.name,
                                    topicName = currentTopic.name,
                                    type = TimerType.COUNTDOWN,
                                    durationMinutes = (safeSecs / 60).coerceAtLeast(1),
                                    durationSeconds = safeSecs
                                )
                            }
                        } else if (snapshot.state == TimerState.STUDYING) {
                            TimerEngine.pause()
                        } else if (snapshot.state == TimerState.PAUSED) {
                            TimerEngine.resume()
                        } else {
                            Toast.makeText(context, "This action is not available right now.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEndSession = { TimerEngine.stop() },
                    onReset = { TimerEngine.reset() },
                    onQuietModeToggle = {
                        if (snapshot.isQuietMode) TimerEngine.exitQuietMode() else TimerEngine.enterQuietMode()
                    },
                    onSetTimer = onOpenSetTimerDialog,
                    modifier = Modifier
                        .weight(0.57f)
                        .fillMaxHeight()
                )
            }
            3 -> { // STOPWATCH (No progress border, plain thin grey border)
                val isSwActive = stopwatchSnapshot.isRunning || stopwatchSnapshot.isPaused
                val timeDisplay = if (isSwActive || stopwatchSnapshot.elapsedMillis > 0) stopwatchSnapshot.formattedDisplay() else "00:00:00"
                val (hhMm, ss) = formatTimeToHHMMSS(timeDisplay)
                LandscapeRectangleTimerCard(
                    modeLabel = "STOPWATCH",
                    timeHoursMinutes = hhMm,
                    timeSeconds = ss,
                    hasProgress = false,
                    progressFraction = 0f,
                    isActive = isSwActive,
                    isRunning = stopwatchSnapshot.isRunning,
                    isPaused = stopwatchSnapshot.isPaused,
                    isStopwatch = true,
                    accentColor = CyanAccent,
                    onPlayPause = {
                        if (!stopwatchSnapshot.isRunning && !stopwatchSnapshot.isPaused) {
                            if (currentSubject == null) {
                                noticeMessage = "No subject found. Please add a subject in Manage Subjects, then press Play again."
                            } else if (currentTopic == null) {
                                noticeMessage = "No topic found. Please add a topic in Manage Subjects, then press Play again."
                            } else {
                                StopwatchEngine.start(
                                    subjectId = currentSubject.id,
                                    subjectName = currentSubject.name,
                                    topicId = currentTopic.id,
                                    topicName = currentTopic.name
                                )
                            }
                        } else if (stopwatchSnapshot.isRunning) {
                            StopwatchEngine.pause()
                        } else if (stopwatchSnapshot.isPaused) {
                            StopwatchEngine.resume()
                        } else {
                            Toast.makeText(context, "This action is not available right now.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEndSession = { StopwatchEngine.stop() },
                    onReset = { StopwatchEngine.reset() },
                    onLap = {
                        if (stopwatchSnapshot.isRunning) {
                            StopwatchEngine.recordLap()
                        } else {
                            Toast.makeText(context, "This action is not available right now.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(0.57f)
                        .fillMaxHeight()
                )
            }
            else -> { // BREAKS
                val isBreakRunning = snapshot.state.isBreak
                val timeDisplay = if (isBreakRunning) snapshot.formattedActiveDisplay() else "00:05:00"
                val (hhMm, ss) = formatTimeToHHMMSS(timeDisplay)
                LandscapeRectangleTimerCard(
                    modeLabel = if (isBreakRunning) snapshot.state.name.replace("_", " ") else "BREAK",
                    timeHoursMinutes = hhMm,
                    timeSeconds = ss,
                    hasProgress = true,
                    progressFraction = if (isBreakRunning) snapshot.progressFraction else 0f,
                    isActive = isBreakRunning,
                    isRunning = isBreakRunning && snapshot.state != TimerState.PAUSED,
                    isPaused = isBreakRunning && snapshot.state == TimerState.PAUSED,
                    isQuietMode = snapshot.isQuietMode,
                    showPlayButton = isBreakRunning,
                    showSetTimer = false,
                    endSessionLabel = if (isBreakRunning) "End Break" else "End Session",
                    accentColor = EmeraldAccent,
                    onPlayPause = {
                        if (isBreakRunning) {
                            if (snapshot.state == TimerState.PAUSED) TimerEngine.resume()
                            else if (snapshot.state.isBreak) TimerEngine.pause()
                            else Toast.makeText(context, "This action is not available right now.", Toast.LENGTH_SHORT).show()
                        } else {
                            TimerEngine.startBreak(BreakType.SHORT_BREAK, 5)
                        }
                    },
                    onEndSession = { TimerEngine.stop() },
                    onReset = { TimerEngine.reset() },
                    onQuietModeToggle = {
                        if (snapshot.isQuietMode) TimerEngine.exitQuietMode() else TimerEngine.enterQuietMode()
                    },
                    modifier = Modifier
                        .weight(0.57f)
                        .fillMaxHeight()
                )
            }
        }

        // RIGHT COLUMN (about 43%): Tabs, description, compact subject, strip, content
        Column(
            modifier = Modifier
                .weight(0.43f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val activeTheme = LocalActiveTheme.current
            val tokens = LocalAppThemeTokens.current
            val tabContainer = if (activeTheme == "Classic") CharcoalCard else if (activeTheme == "Ultra Premium") Color.Transparent else tokens.cardFill
            val tabAccent = if (activeTheme == "Classic") AmberAccent else tokens.accent

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = tabContainer,
                contentColor = tabAccent,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = tabAccent,
                            height = 2.dp
                        )
                    }
                },
                divider = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onSelectTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) tabAccent else (if (activeTheme == "Classic") TextSecondary else tokens.secondaryText),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        },
                        modifier = Modifier.testTag("landscape_timer_tab_$index")
                    )
                }
            }

            // 2. One-line short description
            Text(
                text = descriptionText,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // 3. Compact Subject/Task button (tabs 0..3)
            if (selectedTab != 4) {
                val subName = currentSubject?.name
                val topName = currentTopic?.name
                val buttonText = if (!subName.isNullOrBlank() && !topName.isNullOrBlank()) {
                    "$subName • $topName ▾"
                } else if (!subName.isNullOrBlank()) {
                    "$subName ▾"
                } else {
                    "Select Subject/Task ▾"
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CharcoalCard,
                    border = BorderStroke(1.dp, CharcoalBorder),
                    onClick = onOpenSubjectSheet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("landscape_compact_subject_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buttonText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Target card directly below Subject dropdown with 8dp gap for Session, Pomodoro, Countdown (tabs 0..2)
                if (selectedTab in 0..2) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CharcoalCard,
                        border = BorderStroke(1.dp, CharcoalBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .testTag("landscape_target_card")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Target $sessionDurationMinutes min"
                                    1 -> "Target ${pomodoroStudyMinutes}m study / ${pomodoroBreakMinutes}m rest • $pomodoroCycles cycles"
                                    else -> if (totalCdSecs > 0) "Target ${totalCdSecs / 60}m ${totalCdSecs % 60}s" else "Target 30 min"
                                },
                                fontSize = 11.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 4. Slim strip for OTHER running timer (T1, T8: shown when another timer or stopwatch runs)
            val isCurrentTabTimerActive = when (selectedTab) {
                0 -> snapshot.state.isActive && snapshot.timerType == TimerType.STUDY_SESSION
                1 -> snapshot.state.isActive && snapshot.timerType == TimerType.POMODORO
                2 -> snapshot.state.isActive && snapshot.timerType == TimerType.COUNTDOWN
                else -> false
            }
            val showOtherStopwatch = (stopwatchSnapshot.isRunning || stopwatchSnapshot.isPaused) && selectedTab != 3
            val showOtherTimer = snapshot.state.isActive && !isCurrentTabTimerActive

            if (showOtherStopwatch) {
                val swSecs = stopwatchSnapshot.elapsedMillis / 1000
                val swFormatted = if (swSecs >= 3600) {
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", swSecs / 3600, (swSecs % 3600) / 60, swSecs % 60)
                } else {
                    String.format(Locale.getDefault(), "%02d:%02d", (swSecs % 3600) / 60, swSecs % 60)
                }
                OtherTimerRunningStrip(
                    title = "Stopwatch $swFormatted ${if (stopwatchSnapshot.isRunning) "running" else "paused"}",
                    isRunning = stopwatchSnapshot.isRunning,
                    accentColor = CyanAccent,
                    onTapText = { onSelectTab(3) },
                    onPauseResume = {
                        if (stopwatchSnapshot.isRunning) StopwatchEngine.pause() else StopwatchEngine.resume()
                    },
                    onStop = { StopwatchEngine.stop() }
                )
            }
            if (showOtherTimer) {
                val timerSecs = (snapshot.currentSegmentRemainingMillis / 1000).coerceAtLeast(0)
                val timerFormatted = if (timerSecs >= 3600) {
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", timerSecs / 3600, (timerSecs % 3600) / 60, timerSecs % 60)
                } else {
                    String.format(Locale.getDefault(), "%02d:%02d", (timerSecs % 3600) / 60, timerSecs % 60)
                }
                OtherTimerRunningStrip(
                    title = "${snapshot.timerType.displayName} $timerFormatted ${if (snapshot.state == TimerState.STUDYING) "running" else "paused"}",
                    isRunning = (snapshot.state == TimerState.STUDYING),
                    accentColor = AmberAccent,
                    onTapText = {
                        onSelectTab(
                            when (snapshot.timerType) {
                                TimerType.POMODORO -> 1
                                TimerType.COUNTDOWN -> 2
                                else -> 0
                            }
                        )
                    },
                    onPauseResume = {
                        if (snapshot.state == TimerState.STUDYING) TimerEngine.pause() else TimerEngine.resume()
                    },
                    onStop = { TimerEngine.stop() }
                )
            }

            // 5. Tab specific content area
            when (selectedTab) {
                3 -> {
                    // Stopwatch Lap List (scrollable)
                    if (stopwatchSnapshot.laps.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            items(stopwatchSnapshot.laps.asReversed()) { lap ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CharcoalCard)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Lap ${lap.lapNumber}", color = TextSecondary, fontSize = 11.sp)
                                    Text(
                                        formatMillisToLapDisplay(lap.lapDurationMillis),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No laps recorded. Tap Lap while running.", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
                4 -> {
                    // Break choices: Short 5 min, Long 15 min, Lunch 30 min (scrollable list with 16dp bottom padding)
                    val breakPresets = listOf(
                        Triple("Short", 5, BreakType.SHORT_BREAK),
                        Triple("Long", 15, BreakType.LONG_BREAK),
                        Triple("Lunch", 30, BreakType.LONG_BREAK)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(breakPresets) { (name, mins, type) ->
                            val icon = when (name) {
                                "Short" -> Icons.Default.Coffee
                                "Long" -> Icons.Default.SelfImprovement
                                else -> Icons.Default.Restaurant
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, CharcoalBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x2200D675)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = "$name Break",
                                                tint = EmeraldAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val activeTheme = LocalActiveTheme.current
                                        val tokens = LocalAppThemeTokens.current
                                        val isClassic = activeTheme == "Classic"

                                        Column {
                                            Text("$name Break", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isClassic) TextPrimary else tokens.text)
                                            Text("$mins min", fontSize = 10.sp, color = if (isClassic) TextSecondary else tokens.secondaryText)
                                        }
                                    }
                                    val activeTheme = LocalActiveTheme.current
                                    if (activeTheme == "Classic") {
                                        Button(
                                            onClick = { TimerEngine.startBreak(type, mins) },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("START", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        ThemedActionPill(
                                            text = "START",
                                            onClick = { TimerEngine.startBreak(type, mins) },
                                            isFilled = true,
                                            testTag = "landscape_start_break_${name.lowercase()}"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Empty space below target card for Session, Pomodoro, Countdown
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
    if (noticeMessage != null) {
        AppNoticeDialog(
            message = noticeMessage!!,
            onDismiss = { noticeMessage = null }
        )
    }
}
