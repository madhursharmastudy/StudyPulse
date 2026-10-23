package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import com.example.core.timer.StopwatchEngine
import com.example.core.timer.TimerEngine
import com.example.core.timer.TimerState
import com.example.data.repository.AppSettings
import com.example.presentation.components.AppBackground
import com.example.presentation.components.CombinedBreakOverlay
import com.example.presentation.components.FullscreenEyeAlert
import com.example.presentation.components.FullscreenWaterAlert
import com.example.core.stretch.StretchBreakManager
import com.example.presentation.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            StudyWatchRootApp()
        }
    }
}

enum class NavigationTab(val label: String, val icon: ImageVector) {
    WATCH("Clock", Icons.Default.Schedule),
    TIMER("Timers", Icons.Default.Timer),
    PLANNER("Planner", Icons.Default.CalendarMonth),
    STATS("Stats", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun StudyWatchRootApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val app = context.applicationContext as StudyWatchApplication

    // State flows
    val snapshot by TimerEngine.snapshot.collectAsStateWithLifecycle()
    val stopwatchSnapshot by StopwatchEngine.snapshot.collectAsStateWithLifecycle()
    val settings by app.settingsRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())
    val subjects by app.repository.allSubjects.collectAsStateWithLifecycle(initialValue = emptyList())
    val topics by app.repository.allTopics.collectAsStateWithLifecycle(initialValue = emptyList())
    val sessions by app.repository.allSessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val todayGoal by app.repository.observeDailyGoalForToday().collectAsStateWithLifecycle(initialValue = null)
    val plans by app.repository.allPlans.collectAsStateWithLifecycle(initialValue = emptyList())
    val exams by app.repository.allExams.collectAsStateWithLifecycle(initialValue = emptyList())
    val assignments by app.repository.allAssignments.collectAsStateWithLifecycle(initialValue = emptyList())
    val revisionItems by app.repository.allRevisionItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val logs by app.logRepository.liveLogs.collectAsStateWithLifecycle(initialValue = emptyList())

    var currentTab by remember { mutableStateOf(NavigationTab.WATCH) }
    var isViewingLogBook by remember { mutableStateOf(false) }
    var isManagingSubjects by remember { mutableStateOf(false) }

    // Request POST_NOTIFICATIONS permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        AppLogger.i(LogFeature.NotificationManager, "Notification permission result: $isGranted")
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // Initialize today's goal if not present
        app.repository.getOrCreateTodayGoal()
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val activeTheme = settings.theme

    StudyWatchTheme(
        activeTheme = activeTheme
    ) {
        val themeTokens = LocalAppThemeTokens.current
        val classicAccent = themeTokens.accent
        AppBackground {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.systemBars,
                bottomBar = {
                    // Secondary navigation bar only in portrait mode when user is navigating secondary feature tabs
                    if (!isLandscape && !isViewingLogBook && currentTab != NavigationTab.WATCH && snapshot.state != TimerState.EYE_REST && snapshot.state != TimerState.WATER_BREAK) {
                        if (isExistingTheme(activeTheme)) {
                            // Existing themes (M3): keep current shape and selected box. Selected color follows accent role.
                            NavigationBar(
                                containerColor = DarkCard,
                                contentColor = TextPrimary,
                                modifier = Modifier.testTag("main_bottom_nav")
                            ) {
                                NavigationTab.entries.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label, fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = themeTokens.accent,
                                            selectedTextColor = themeTokens.accent,
                                            indicatorColor = themeTokens.accent.copy(alpha = 0.2f),
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        } else if (activeTheme == "Ultra Premium") {
                            // Part F: Ultra Premium floating rounded plate (radius 28dp, fill = UltraPremiumGlass, windowInsets outside, 0.dp NavigationBar)
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = UltraPremiumGlass,
                                modifier = Modifier
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                                    .fillMaxWidth()
                            ) {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White,
                                    windowInsets = WindowInsets(0.dp),
                                    modifier = Modifier.testTag("main_bottom_nav")
                                ) {
                                    NavigationTab.entries.forEach { tab ->
                                        val selected = currentTab == tab
                                        val glowAlpha by androidx.compose.animation.core.animateFloatAsState(
                                            targetValue = if (selected) 1f else 0f,
                                            animationSpec = androidx.compose.animation.core.tween(200),
                                            label = "nav_glow_${tab.name}"
                                        )
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { currentTab = tab },
                                            icon = {
                                                Box(
                                                    modifier = Modifier.drawBehind {
                                                        if (glowAlpha > 0f) {
                                                            drawCircle(
                                                                brush = Brush.radialGradient(
                                                                    colors = listOf(
                                                                        Color(0x70FF8A24).copy(alpha = 0.50f * glowAlpha),
                                                                        Color(0x40FF8A24).copy(alpha = 0.25f * glowAlpha),
                                                                        Color.Transparent
                                                                    ),
                                                                    center = this.center,
                                                                    radius = 36.dp.toPx()
                                                                ),
                                                                radius = 36.dp.toPx(),
                                                                center = this.center
                                                            )
                                                        }
                                                    },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(tab.icon, contentDescription = tab.label)
                                                }
                                            },
                                            label = { Text(tab.label, fontSize = 11.sp) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color(0xFFFFB52E),
                                                selectedTextColor = Color(0xFFFFB52E),
                                                indicatorColor = Color.Transparent,
                                                unselectedIconColor = Color(0xFFAAB8D6),
                                                unselectedTextColor = Color(0xFFAAB8D6)
                                            ),
                                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        } else {
                            // Dark & Light (M1, M2): Floating rounded pill, radius 28dp, inset 12dp. Fill = card role. No border, no shadow, no indicator box.
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = themeTokens.cardFill,
                                modifier = Modifier
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                                    .fillMaxWidth()
                            ) {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    contentColor = themeTokens.text,
                                    windowInsets = WindowInsets(0.dp),
                                    modifier = Modifier.testTag("main_bottom_nav")
                                ) {
                                    NavigationTab.entries.forEach { tab ->
                                        NavigationBarItem(
                                            selected = currentTab == tab,
                                            onClick = { currentTab = tab },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label, fontSize = 11.sp) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = themeTokens.accent,
                                                selectedTextColor = themeTokens.accent,
                                                indicatorColor = Color.Transparent,
                                                unselectedIconColor = themeTokens.secondaryText,
                                                unselectedTextColor = themeTokens.secondaryText
                                            ),
                                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Left Navigation Rail in Landscape mode (64-72dp wide)
                        if (isLandscape && !isViewingLogBook && !isManagingSubjects && snapshot.state != TimerState.EYE_REST && snapshot.state != TimerState.WATER_BREAK) {
                            if (isExistingTheme(activeTheme)) {
                                NavigationRail(
                                    containerColor = DarkCard,
                                    contentColor = TextPrimary,
                                    modifier = Modifier
                                        .width(68.dp)
                                        .fillMaxHeight()
                                        .testTag("main_landscape_nav_rail")
                                ) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    NavigationTab.entries.forEach { tab ->
                                        NavigationRailItem(
                                            selected = currentTab == tab,
                                            onClick = { currentTab = tab },
                                            icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(20.dp)) },
                                            label = { Text(tab.label, fontSize = 10.sp, maxLines = 1) },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = themeTokens.accent,
                                                selectedTextColor = themeTokens.accent,
                                                indicatorColor = themeTokens.accent.copy(alpha = 0.2f),
                                                unselectedIconColor = TextMuted,
                                                unselectedTextColor = TextMuted
                                            ),
                                            modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                                        )
                                    }
                                }
                            } else if (activeTheme == "Ultra Premium") {
                                // Ultra Premium landscape rail: one single plate with glow!
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = UltraPremiumGlass,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .width(68.dp)
                                        .fillMaxHeight()
                                ) {
                                    NavigationRail(
                                        containerColor = Color.Transparent,
                                        contentColor = Color.White,
                                        windowInsets = WindowInsets(0.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("main_landscape_nav_rail")
                                    ) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        NavigationTab.entries.forEach { tab ->
                                            val selected = currentTab == tab
                                            val glowAlpha by androidx.compose.animation.core.animateFloatAsState(
                                                targetValue = if (selected) 1f else 0f,
                                                animationSpec = androidx.compose.animation.core.tween(200),
                                                label = "rail_glow_${tab.name}"
                                            )
                                            NavigationRailItem(
                                                selected = selected,
                                                onClick = { currentTab = tab },
                                                icon = {
                                                    Box(
                                                        modifier = Modifier.drawBehind {
                                                            if (glowAlpha > 0f) {
                                                                drawCircle(
                                                                    brush = Brush.radialGradient(
                                                                        colors = listOf(
                                                                            Color(0x70FF8A24).copy(alpha = 0.50f * glowAlpha),
                                                                            Color(0x40FF8A24).copy(alpha = 0.25f * glowAlpha),
                                                                            Color.Transparent
                                                                        ),
                                                                        center = this.center,
                                                                        radius = 56.dp.toPx()
                                                                    ),
                                                                    radius = 56.dp.toPx(),
                                                                    center = this.center
                                                                )
                                                            }
                                                        },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(20.dp))
                                                    }
                                                },
                                                label = { Text(tab.label, fontSize = 10.sp, maxLines = 1) },
                                                colors = NavigationRailItemDefaults.colors(
                                                    selectedIconColor = Color(0xFFFFB52E),
                                                    selectedTextColor = Color(0xFFFFB52E),
                                                    indicatorColor = Color.Transparent,
                                                    unselectedIconColor = Color(0xFFAAB8D6),
                                                    unselectedTextColor = Color(0xFFAAB8D6)
                                                ),
                                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Dark & Light: landscape rail as one single plate
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = themeTokens.cardFill,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .width(68.dp)
                                        .fillMaxHeight()
                                ) {
                                    NavigationRail(
                                        containerColor = Color.Transparent,
                                        contentColor = themeTokens.text,
                                        windowInsets = WindowInsets(0.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("main_landscape_nav_rail")
                                    ) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        NavigationTab.entries.forEach { tab ->
                                            NavigationRailItem(
                                                selected = currentTab == tab,
                                                onClick = { currentTab = tab },
                                                icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(20.dp)) },
                                                label = { Text(tab.label, fontSize = 10.sp, maxLines = 1) },
                                                colors = NavigationRailItemDefaults.colors(
                                                    selectedIconColor = themeTokens.accent,
                                                    selectedTextColor = themeTokens.accent,
                                                    indicatorColor = Color.Transparent,
                                                    unselectedIconColor = themeTokens.secondaryText,
                                                    unselectedTextColor = themeTokens.secondaryText
                                                ),
                                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                // Main Screen Navigation Router
                if (isViewingLogBook) {
                    LogBookScreen(
                        logs = logs,
                        onClearLogs = { coroutineScope.launch { app.logRepository.clearLogs() } },
                        onBack = { isViewingLogBook = false }
                    )
                } else if (isManagingSubjects) {
                    ManageSubjectsScreen(
                        subjects = subjects,
                        topics = topics,
                        onBack = { isManagingSubjects = false },
                        onAddSubject = { name, icon, colorHex, goalMins ->
                            coroutineScope.launch { app.repository.insertSubject(name, icon, colorHex, goalMins) }
                        },
                        onUpdateSubject = {
                            coroutineScope.launch { app.repository.updateSubject(it) }
                        },
                        onDeleteSubject = {
                            coroutineScope.launch { app.repository.deleteSubject(it) }
                        },
                        onAddTopic = { subjectId, name ->
                            coroutineScope.launch { app.repository.insertTopic(subjectId, name) }
                        },
                        onUpdateTopic = {
                            coroutineScope.launch { app.repository.updateTopic(it) }
                        },
                        onDeleteTopic = {
                            coroutineScope.launch { app.repository.deleteTopic(it) }
                        }
                    )
                } else {
                    when (currentTab) {
                        NavigationTab.WATCH -> {
                            WatchScreen(
                                snapshot = snapshot,
                                settings = settings,
                                subjects = subjects,
                                topics = topics,
                                todayGoal = todayGoal,
                                sessions = sessions,
                                onStartSessionClick = { currentTab = NavigationTab.TIMER },
                                onOpenSettings = { currentTab = NavigationTab.SETTINGS },
                                onSelectSubject = { sub ->
                                    coroutineScope.launch { app.settingsRepository.updateSelectedSubject(sub.id, sub.name) }
                                },
                                onSelectTopic = { top ->
                                    coroutineScope.launch { app.settingsRepository.updateSelectedTopic(top.id, top.name) }
                                },
                                onAddNewSubject = { name, icon, colorHex, goalMins ->
                                    coroutineScope.launch {
                                        app.repository.insertSubject(name, icon, colorHex, goalMins)
                                    }
                                },
                                onAddNewTopic = { subId, name ->
                                    coroutineScope.launch {
                                        app.repository.insertTopic(subId, name)
                                    }
                                },
                                onUpdateSubject = { sub ->
                                    coroutineScope.launch {
                                        app.repository.updateSubject(sub)
                                    }
                                },
                                onDeleteSubject = { id ->
                                    coroutineScope.launch {
                                        app.repository.deleteSubject(id)
                                    }
                                },
                                onUpdateTopic = { top ->
                                    coroutineScope.launch {
                                        app.repository.updateTopic(top)
                                    }
                                },
                                onDeleteTopic = { id ->
                                    coroutineScope.launch {
                                        app.repository.deleteTopic(id)
                                    }
                                },
                                onUpdateDefaultStudyDuration = { dur ->
                                    coroutineScope.launch { app.settingsRepository.updateDefaultStudyDuration(dur) }
                                },
                                onUpdateEyeStudyInterval = { mins ->
                                    coroutineScope.launch { app.settingsRepository.updateEyeStudyInterval(mins) }
                                },
                                onUpdateEyeRestDuration = { secs ->
                                    coroutineScope.launch { app.settingsRepository.updateEyeRestDuration(secs) }
                                },
                                onUpdateWaterInterval = { mins ->
                                    coroutineScope.launch { app.settingsRepository.updateWaterInterval(mins) }
                                },
                                onUpdateWaterBreakDuration = { secs ->
                                    coroutineScope.launch { app.settingsRepository.updateWaterBreakDuration(secs) }
                                }
                            )
                        }
                        NavigationTab.TIMER -> {
                            TimerScreen(
                                snapshot = snapshot,
                                stopwatchSnapshot = stopwatchSnapshot,
                                subjects = subjects,
                                topics = topics,
                                selectedSubjectId = settings.selectedSubjectId,
                                selectedTopicId = settings.selectedTopicId,
                                onBackToWatch = { currentTab = NavigationTab.WATCH },
                                onOpenManageSubjects = { isManagingSubjects = true },
                                onSelectSubject = { sub ->
                                    coroutineScope.launch { app.settingsRepository.updateSelectedSubject(sub.id, sub.name) }
                                },
                                onSelectTopic = { top ->
                                    coroutineScope.launch { app.settingsRepository.updateSelectedTopic(top.id, top.name) }
                                },
                                onQuickAddSubject = { name ->
                                    coroutineScope.launch { app.repository.insertSubject(name, "Book", "#FFA640", 60) }
                                },
                                onQuickAddTopic = { subjectId, name ->
                                    coroutineScope.launch { app.repository.insertTopic(subjectId, name) }
                                },
                                onUpdateSubject = { sub ->
                                    coroutineScope.launch { app.repository.updateSubject(sub) }
                                },
                                onDeleteSubject = { id ->
                                    coroutineScope.launch { app.repository.deleteSubject(id) }
                                },
                                onUpdateTopic = { top ->
                                    coroutineScope.launch { app.repository.updateTopic(top) }
                                },
                                onDeleteTopic = { id ->
                                    coroutineScope.launch { app.repository.deleteTopic(id) }
                                },
                                initialSessionMinutes = settings.lastSessionMinutes,
                                initialPomodoroStudyMinutes = settings.lastPomodoroStudyMinutes,
                                initialPomodoroBreakMinutes = settings.lastPomodoroBreakMinutes,
                                initialPomodoroCycles = settings.lastPomodoroCycles,
                                initialCountdownMinutes = settings.lastCountdownMinutes,
                                onSaveSessionMinutes = { mins ->
                                    coroutineScope.launch { app.settingsRepository.updateLastSessionMinutes(mins) }
                                },
                                onSavePomodoroSettings = { s, b, c ->
                                    coroutineScope.launch { app.settingsRepository.updateLastPomodoroSettings(s, b, c) }
                                },
                                onSaveCountdownMinutes = { mins ->
                                    coroutineScope.launch { app.settingsRepository.updateLastCountdownMinutes(mins) }
                                }
                            )
                        }
                        NavigationTab.PLANNER -> {
                            PlannerScreen(
                                studyPlans = plans,
                                exams = exams,
                                assignments = assignments,
                                revisionItems = revisionItems,
                                subjects = subjects,
                                topics = topics,
                                onAddStudyPlan = { coroutineScope.launch { app.repository.insertStudyPlan(it) } },
                                onUpdateStudyPlan = { coroutineScope.launch { app.repository.updateStudyPlan(it) } },
                                onTogglePlanCompleted = { id, comp -> coroutineScope.launch { app.repository.setStudyPlanCompleted(id, comp) } },
                                onDeleteStudyPlan = { id -> coroutineScope.launch { app.repository.deleteStudyPlan(id) } },
                                onAddExam = { coroutineScope.launch { app.repository.insertExam(it) } },
                                onUpdateExam = { coroutineScope.launch { app.repository.updateExam(it) } },
                                onDeleteExam = { id -> coroutineScope.launch { app.repository.deleteExam(id) } },
                                onAddAssignment = { coroutineScope.launch { app.repository.insertAssignment(it) } },
                                onUpdateAssignment = { coroutineScope.launch { app.repository.updateAssignment(it) } },
                                onDeleteAssignment = { id -> coroutineScope.launch { app.repository.deleteAssignment(id) } },
                                onAddRevisionItem = { coroutineScope.launch { app.repository.insertRevisionItem(it) } },
                                onUpdateRevisionItem = { coroutineScope.launch { app.repository.updateRevisionItem(it) } },
                                onDeleteRevisionItem = { id -> coroutineScope.launch { app.repository.deleteRevisionItem(id) } },
                                onOpenManageSubjects = { isManagingSubjects = true },
                                onQuickAddSubject = { name ->
                                    coroutineScope.launch { app.repository.insertSubject(name, "Book", "#FFA640", 60) }
                                },
                                onQuickAddTopic = { subjectId, name ->
                                    coroutineScope.launch { app.repository.insertTopic(subjectId, name) }
                                }
                            )
                        }
                        NavigationTab.STATS -> {
                            StatisticsScreen(
                                sessions = sessions,
                                subjects = subjects,
                                onDeleteSession = { id -> coroutineScope.launch { app.repository.deleteStudySession(id) } },
                                dailyTargetHours = settings.dailyTargetHours
                            )
                        }
                        NavigationTab.SETTINGS -> {
                            SettingsScreen(
                                settings = settings,
                                onBackToWatch = { currentTab = NavigationTab.WATCH },
                                onNavigateToTimer = { currentTab = NavigationTab.TIMER },
                                onNavigateToPlanner = { currentTab = NavigationTab.PLANNER },
                                onNavigateToStats = { currentTab = NavigationTab.STATS },
                                onUpdate24Hour = { coroutineScope.launch { app.settingsRepository.update24Hour(it) } },
                                onUpdateShowSeconds = { coroutineScope.launch { app.settingsRepository.updateShowSeconds(it) } },
                                onUpdateClockStyle = { coroutineScope.launch { app.settingsRepository.updateClockStyle(it) } },
                                onUpdateTheme = { coroutineScope.launch { app.settingsRepository.updateTheme(it) } },
                                onUpdateBackgroundStyle = { coroutineScope.launch { app.settingsRepository.updateBackgroundStyle(it) } },
                                onUpdateAppTheme = { coroutineScope.launch { app.settingsRepository.updateAppTheme(it) } },
                                onUpdateDefaultStudyDuration = { coroutineScope.launch { app.settingsRepository.updateDefaultStudyDuration(it) } },
                                onUpdateEyeCareEnabled = { coroutineScope.launch { app.settingsRepository.updateEyeCareEnabled(it) } },
                                onUpdateEyeStudyInterval = { coroutineScope.launch { app.settingsRepository.updateEyeStudyInterval(it) } },
                                onUpdateEyeRestDuration = { coroutineScope.launch { app.settingsRepository.updateEyeRestDuration(it) } },
                                onUpdateEyeAutoResume = { coroutineScope.launch { app.settingsRepository.updateEyeAutoResume(it) } },
                                onUpdateWaterEnabled = { coroutineScope.launch { app.settingsRepository.updateWaterReminderEnabled(it) } },
                                onUpdateWaterInterval = { coroutineScope.launch { app.settingsRepository.updateWaterInterval(it) } },
                                onUpdateWaterAutoResume = { coroutineScope.launch { app.settingsRepository.updateWaterAutoResume(it) } },
                                onUpdateStretchBreakEnabled = { coroutineScope.launch { app.settingsRepository.updateStretchBreakEnabled(it) } },
                                onUpdateBreakInterval = { coroutineScope.launch { app.settingsRepository.updateBreakIntervalMinutes(it) } },
                                onUpdateShortModeEnabled = { coroutineScope.launch { app.settingsRepository.updateShortModeEnabled(it) } },
                                onUpdateIncludeStandingExercises = { coroutineScope.launch { app.settingsRepository.updateIncludeStandingExercises(it) } },
                                onUpdateSound = { coroutineScope.launch { app.settingsRepository.updateSoundEnabled(it) } },
                                onUpdateVibration = { coroutineScope.launch { app.settingsRepository.updateVibrationEnabled(it) } },
                                onUpdateNotificationsEnabled = { coroutineScope.launch { app.settingsRepository.updateNotificationsEnabled(it) } },
                                onUpdateKeepScreenOn = { coroutineScope.launch { app.settingsRepository.updateKeepScreenOn(it) } },
                                onToggleDebugMode = { coroutineScope.launch { app.settingsRepository.toggleDebugMode(it) } },
                                onExportJsonClick = {
                                    coroutineScope.launch {
                                        val json = app.repository.exportAllDataToJson()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("DeskPulseBackup", json))
                                        Toast.makeText(context, "Exported JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onExportCsvClick = {
                                    coroutineScope.launch {
                                        val csv = app.repository.exportSessionsToCsv()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("DeskPulseCSV", csv))
                                        Toast.makeText(context, "Exported CSV copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onExportTxtClick = {
                                    coroutineScope.launch {
                                        val txt = app.repository.exportDataToTxt()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("DeskPulseReport", txt))
                                        Toast.makeText(context, "Exported Study Report copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onOpenLogBook = { isViewingLogBook = true },
                                onUpdateDailyTarget = { coroutineScope.launch { app.settingsRepository.updateDailyTargetHours(it) } },
                                onResetTodayStats = { coroutineScope.launch { app.repository.resetTodayStats() } },
                                onClearAllData = { coroutineScope.launch { app.repository.clearAllData() } }
                            )
                        }
                    }
                }
            }
        }

                // Fullscreen Overlay for 20-20-20 Eye Care
                AnimatedVisibility(
                    visible = snapshot.state == TimerState.EYE_REST,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FullscreenEyeAlert(snapshot = snapshot)
                }

                // Fullscreen Overlay for Hydration & Stretch Health Break
                AnimatedVisibility(
                    visible = snapshot.state == TimerState.WATER_BREAK,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    CombinedBreakOverlay(
                        snapshot = snapshot,
                        shortMode = settings.shortModeEnabled,
                        includeStanding = settings.includeStandingExercises,
                        onCompleteBreak = { completedStretches ->
                            coroutineScope.launch {
                                val streak = StretchBreakManager.recordBreakCompleted()
                                TimerEngine.completeWaterBreak(autoResumed = true)
                                val logDesc = when {
                                    snapshot.waterReminderEnabled && snapshot.stretchBreakEnabled ->
                                        "Break completed: Water + Stretch ($streak today)"
                                    snapshot.stretchBreakEnabled ->
                                        "Break completed: Stretch break ($streak today)"
                                    else ->
                                        "Break completed: Water break ($streak today)"
                                }
                                AppLogger.i(
                                    feature = LogFeature.Hydration,
                                    event = logDesc,
                                    details = "Completed health break cycle",
                                    timerState = snapshot.state.name
                                )
                            }
                        },
                        onSkipAll = {
                            TimerEngine.skipWaterReminder()
                        }
                    )
                }
            }
        }
    }
}
}
