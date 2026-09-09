package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import com.example.core.timer.TimerEngine
import com.example.core.timer.TimerState
import com.example.data.repository.AppSettings
import com.example.presentation.components.FullscreenEyeAlert
import com.example.presentation.components.FullscreenWaterAlert
import com.example.presentation.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StudyWatchRootApp()
        }
    }
}

enum class NavigationTab(val label: String, val icon: ImageVector) {
    WATCH("Watch", Icons.Default.Schedule),
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

    StudyWatchTheme(isAmoled = settings.appTheme != "Light") {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AmoledBlack,
            contentWindowInsets = WindowInsets.systemBars,
            bottomBar = {
                // Secondary navigation bar only when user is navigating secondary feature tabs
                if (!isViewingLogBook && currentTab != NavigationTab.WATCH && snapshot.state != TimerState.EYE_REST && snapshot.state != TimerState.WATER_BREAK) {
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
                                    selectedIconColor = StudyAccent,
                                    selectedTextColor = StudyAccent,
                                    indicatorColor = Color(0x33FFA640),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
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
                // Main Screen Navigation Router
                if (isViewingLogBook) {
                    LogBookScreen(
                        logs = logs,
                        onClearLogs = { coroutineScope.launch { app.logRepository.clearLogs() } },
                        onBack = { isViewingLogBook = false }
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
                                }
                            )
                        }
                        NavigationTab.TIMER -> {
                            TimerScreen(
                                snapshot = snapshot,
                                subjects = subjects,
                                topics = topics,
                                onBackToWatch = { currentTab = NavigationTab.WATCH }
                            )
                        }
                        NavigationTab.PLANNER -> {
                            PlannerScreen(
                                studyPlans = plans,
                                exams = exams,
                                assignments = assignments,
                                revisionItems = revisionItems,
                                onAddStudyPlan = { coroutineScope.launch { app.repository.insertStudyPlan(it) } },
                                onTogglePlanCompleted = { id, comp -> coroutineScope.launch { app.repository.setStudyPlanCompleted(id, comp) } },
                                onDeleteStudyPlan = { id -> coroutineScope.launch { app.repository.deleteStudyPlan(id) } },
                                onAddExam = { coroutineScope.launch { app.repository.insertExam(it) } },
                                onDeleteExam = { id -> coroutineScope.launch { app.repository.deleteExam(id) } },
                                onAddAssignment = { coroutineScope.launch { app.repository.insertAssignment(it) } },
                                onDeleteAssignment = { id -> coroutineScope.launch { app.repository.deleteAssignment(id) } },
                                onAddRevisionItem = { coroutineScope.launch { app.repository.insertRevisionItem(it) } }
                            )
                        }
                        NavigationTab.STATS -> {
                            StatisticsScreen(
                                sessions = sessions,
                                subjects = subjects,
                                onDeleteSession = { id -> coroutineScope.launch { app.repository.deleteStudySession(id) } }
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
                                onUpdateDefaultStudyDuration = { coroutineScope.launch { app.settingsRepository.updateDefaultStudyDuration(it) } },
                                onUpdateEyeCareEnabled = { coroutineScope.launch { app.settingsRepository.updateEyeCareEnabled(it) } },
                                onUpdateEyeStudyInterval = { coroutineScope.launch { app.settingsRepository.updateEyeStudyInterval(it) } },
                                onUpdateEyeRestDuration = { coroutineScope.launch { app.settingsRepository.updateEyeRestDuration(it) } },
                                onUpdateEyeAutoResume = { coroutineScope.launch { app.settingsRepository.updateEyeAutoResume(it) } },
                                onUpdateWaterEnabled = { coroutineScope.launch { app.settingsRepository.updateWaterReminderEnabled(it) } },
                                onUpdateWaterInterval = { coroutineScope.launch { app.settingsRepository.updateWaterInterval(it) } },
                                onUpdateWaterAutoResume = { coroutineScope.launch { app.settingsRepository.updateWaterAutoResume(it) } },
                                onUpdateSound = { coroutineScope.launch { app.settingsRepository.updateSoundEnabled(it) } },
                                onUpdateVibration = { coroutineScope.launch { app.settingsRepository.updateVibrationEnabled(it) } },
                                onUpdateKeepScreenOn = { coroutineScope.launch { app.settingsRepository.updateKeepScreenOn(it) } },
                                onToggleDebugMode = { coroutineScope.launch { app.settingsRepository.toggleDebugMode(it) } },
                                onExportJsonClick = {
                                    coroutineScope.launch {
                                        val json = app.repository.exportAllDataToJson()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("StudyPulseBackup", json))
                                        Toast.makeText(context, "Exported JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onExportCsvClick = {
                                    coroutineScope.launch {
                                        val csv = app.repository.exportSessionsToCsv()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("StudyPulseCSV", csv))
                                        Toast.makeText(context, "Exported CSV copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onExportTxtClick = {
                                    coroutineScope.launch {
                                        val txt = app.repository.exportDataToTxt()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("StudyPulseReport", txt))
                                        Toast.makeText(context, "Exported Study Report copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onOpenLogBook = { isViewingLogBook = true }
                            )
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

                // Fullscreen Overlay for Hydration Water Break
                AnimatedVisibility(
                    visible = snapshot.state == TimerState.WATER_BREAK,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FullscreenWaterAlert(snapshot = snapshot)
                }
            }
        }
    }
}
