package com.example.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.timer.TimerEngine
import com.example.data.repository.AppSettings
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBackToWatch: () -> Unit,
    onNavigateToTimer: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onUpdate24Hour: (Boolean) -> Unit,
    onUpdateShowSeconds: (Boolean) -> Unit,
    onUpdateClockStyle: (String) -> Unit,
    onUpdateDefaultStudyDuration: (Int) -> Unit = {},
    onUpdateEyeCareEnabled: (Boolean) -> Unit,
    onUpdateEyeStudyInterval: (Int) -> Unit,
    onUpdateEyeRestDuration: (Int) -> Unit,
    onUpdateEyeAutoResume: (Boolean) -> Unit,
    onUpdateWaterEnabled: (Boolean) -> Unit,
    onUpdateWaterInterval: (Int) -> Unit,
    onUpdateWaterAutoResume: (Boolean) -> Unit,
    onUpdateStretchBreakEnabled: (Boolean) -> Unit = {},
    onUpdateBreakInterval: (Int) -> Unit = {},
    onUpdateShortModeEnabled: (Boolean) -> Unit = {},
    onUpdateIncludeStandingExercises: (Boolean) -> Unit = {},
    onUpdateSound: (Boolean) -> Unit,
    onUpdateVibration: (Boolean) -> Unit,
    onUpdateKeepScreenOn: (Boolean) -> Unit,
    onToggleDebugMode: (Boolean) -> Unit,
    onExportJsonClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onExportTxtClick: () -> Unit = {},
    onOpenLogBook: () -> Unit,
    onUpdateDailyTarget: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToWatch,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Watch",
                            tint = StudyAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmoledBlack
                )
            )
        },
        containerColor = AmoledBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: STUDY
            item {
                SettingsCard(
                    title = "STUDY",
                    subtitle = "Default study session goals and defaults"
                ) {
                    Text("Default session length", fontWeight = FontWeight.Bold, color = TextPrimary)
                    val durations = listOf(15, 25, 30, 45, 50, 60, 90)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        durations.forEach { dur ->
                            val isSelected = settings.defaultStudyDurationMinutes == dur
                            Surface(
                                onClick = { onUpdateDefaultStudyDuration(dur) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) StudyAccent else Color(0xFF1E1E1E),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${dur}m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))

                    Text("Daily target (hours)", fontWeight = FontWeight.Bold, color = TextPrimary)
                    val targets = listOf(2, 4, 6, 8, 10)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        targets.forEach { target ->
                            val isSelected = settings.dailyTargetHours == target
                            Surface(
                                onClick = { onUpdateDailyTarget(target) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) StudyAccent else Color(0xFF1E1E1E),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${target}h",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2: DISPLAY
            item {
                SettingsCard(
                    title = "DISPLAY",
                    subtitle = "Customize visual options and screen behavior"
                ) {
                    SettingSwitchRow(
                        title = "24-Hour Format",
                        subtitle = "Display time in 24h format instead of 12h AM/PM",
                        checked = settings.use24HourFormat,
                        onCheckedChange = onUpdate24Hour
                    )
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Show Seconds",
                        subtitle = "Display ticking seconds beside the desk clock",
                        checked = settings.showSeconds,
                        onCheckedChange = onUpdateShowSeconds
                    )
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Keep Screen Awake",
                        subtitle = "Prevent display sleep in desk watch mode",
                        checked = settings.keepScreenOn,
                        onCheckedChange = onUpdateKeepScreenOn
                    )
                }
            }

            // SECTION 3: REMINDERS
            item {
                SettingsCard(
                    title = "REMINDERS",
                    subtitle = "Configure healthy study habits and break alerts"
                ) {
                    SettingSwitchRow(
                        title = "Eye Care (20-20-20)",
                        subtitle = "Pause study timer and rest eyes periodically (20 min)",
                        checked = settings.eyeCareEnabled,
                        onCheckedChange = onUpdateEyeCareEnabled
                    )
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Water Reminder",
                        subtitle = "Periodic reminders to drink water and take a short walk",
                        checked = settings.waterReminderEnabled,
                        onCheckedChange = onUpdateWaterEnabled
                    )
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Stretch Break",
                        subtitle = "Gentle ergonomic exercises for neck, shoulders, back & legs",
                        checked = settings.stretchBreakEnabled,
                        onCheckedChange = onUpdateStretchBreakEnabled
                    )
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 2.dp))

                    Text("Break interval", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Shared by Water Reminder and Stretch Break", fontSize = 12.sp, color = TextSecondary)
                    val intervals = listOf(30, 45, 60)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        intervals.forEach { interval ->
                            val isSelected = settings.breakIntervalMinutes == interval
                            Surface(
                                onClick = { onUpdateBreakInterval(interval) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) WaterAccent else Color(0xFF1E1E1E),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$interval min",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Short mode (3 min)",
                        subtitle = "1 min water + 3 exercises instead of 6 exercises",
                        checked = settings.shortModeEnabled,
                        onCheckedChange = onUpdateShortModeEnabled
                    )
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Only sitting exercises",
                        subtitle = "Filters out standing exercises (calf raises, back extension)",
                        checked = !settings.includeStandingExercises,
                        onCheckedChange = { sittingOnly -> onUpdateIncludeStandingExercises(!sittingOnly) }
                    )
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Auto-Resume After Break",
                        subtitle = "Automatically resume study when break or eye rest finishes",
                        checked = settings.eyeAutoResume,
                        onCheckedChange = {
                            onUpdateEyeAutoResume(it)
                            onUpdateWaterAutoResume(it)
                        }
                    )
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Sound",
                        subtitle = "Play alert chime on eye rest and breaks",
                        checked = settings.soundEnabled,
                        onCheckedChange = onUpdateSound
                    )
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Vibration",
                        subtitle = "Haptic feedback for timer events",
                        checked = settings.vibrationEnabled,
                        onCheckedChange = onUpdateVibration
                    )
                }
            }

            // SECTION 4: ABOUT
            item {
                SettingsCard(
                    title = "ABOUT",
                    subtitle = "About StudyPulse and system developer tools"
                ) {
                    var showDevTools by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_studypulse_logo),
                            contentDescription = "StudyPulse Logo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = "StudyPulse",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = CreamWhite
                        )
                        Text(
                            text = "FOCUS. REST. HYDRATE. REPEAT.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = StudyAccent
                        )
                        Text(
                            text = "A minimalist AMOLED desk watch and productivity companion designed for deep focus, healthy 20-20-20 eye care, and regular hydration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showDevTools = !showDevTools }
                                .padding(6.dp)
                        )

                        if (showDevTools) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onOpenLogBook,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_log_book_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222), contentColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Diagnostic Log Book")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = StudyAccent,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = StudyAccent,
                checkedTrackColor = Color(0x66FFA640),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Color(0xFF222222)
            )
        )
    }
}
