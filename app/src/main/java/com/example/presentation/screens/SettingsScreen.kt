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
    onUpdateSound: (Boolean) -> Unit,
    onUpdateVibration: (Boolean) -> Unit,
    onUpdateKeepScreenOn: (Boolean) -> Unit,
    onToggleDebugMode: (Boolean) -> Unit,
    onExportJsonClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onExportTxtClick: () -> Unit = {},
    onOpenLogBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS & HUB",
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
            // Section: Secondary Navigation Hub
            item {
                SectionHeader("STUDYPULSE PRODUCTIVITY HUB")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Access secondary study and productivity tools:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateToTimer,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StudyAccent)
                            ) {
                                Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Timers", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onNavigateToPlanner,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EyeCareAccent)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Planner", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onNavigateToStats,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WaterAccent)
                            ) {
                                Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Stats", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Section: Study Session Defaults
            item {
                SectionHeader("DEFAULT STUDY SESSION DURATION")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Default session duration when started from Desk Watch:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
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
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Clock & Display
            item {
                SectionHeader("CLOCK & DESK DISPLAY")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingSwitchRow(
                            "24-Hour Format",
                            "Display time in 24h format instead of 12h AM/PM",
                            settings.use24HourFormat,
                            onUpdate24Hour
                        )
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))
                        SettingSwitchRow(
                            "Show Seconds",
                            "Display ticking seconds beside the desk clock",
                            settings.showSeconds,
                            onUpdateShowSeconds
                        )
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))
                        SettingSwitchRow(
                            "Keep Screen Awake",
                            "Prevent display sleep in desk watch mode",
                            settings.keepScreenOn,
                            onUpdateKeepScreenOn
                        )
                    }
                }
            }

            // Section: 20-20-20 Eye Care
            item {
                SectionHeader("20-20-20 EYE CARE SYSTEM")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingSwitchRow(
                            "Eye Care Reminder",
                            "Pause study timer and rest eyes periodically",
                            settings.eyeCareEnabled,
                            onUpdateEyeCareEnabled
                        )
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))
                        SettingSwitchRow(
                            "Auto-Resume After Rest",
                            "Automatically resume study when eye rest ends",
                            settings.eyeAutoResume,
                            onUpdateEyeAutoResume
                        )
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Eye Reminder Interval", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Rule: every 20 minutes (20s in debug)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Text("${settings.eyeStudyIntervalMinutes}m", fontWeight = FontWeight.Bold, color = EyeCareAccent)
                        }
                    }
                }
            }

            // Section: Hydration Reminder
            item {
                SectionHeader("HYDRATION REMINDER")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingSwitchRow(
                            "Water Intake Reminders",
                            "Periodic reminders to drink water during study blocks",
                            settings.waterReminderEnabled,
                            onUpdateWaterEnabled
                        )
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Water Interval", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Default 45 minutes", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Text("${settings.waterIntervalMinutes}m", fontWeight = FontWeight.Bold, color = WaterAccent)
                        }
                    }
                }
            }

            // Section: Sound & Haptics
            item {
                SectionHeader("SOUND & HAPTICS")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingSwitchRow("Sound Effects", "Play alert chime on eye rest and breaks", settings.soundEnabled, onUpdateSound)
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))
                        SettingSwitchRow("Vibration & Haptics", "Haptic feedback for timer events", settings.vibrationEnabled, onUpdateVibration)
                    }
                }
            }

            // Section: Data & Backup
            item {
                SectionHeader("DATA EXPORT & BACKUP")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportJsonClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_json_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export All Data (JSON)")
                        }

                        OutlinedButton(
                            onClick = onExportCsvClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_csv_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Study Sessions (CSV)")
                        }

                        OutlinedButton(
                            onClick = onExportTxtClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_txt_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Study Report (TXT)")
                        }
                    }
                }
            }

            // Section: Developer & Diagnostics
            item {
                SectionHeader("DEVELOPER & DIAGNOSTICS")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(RoseAccent)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingSwitchRow(
                            title = "Debug Test Mode",
                            subtitle = "Accelerates intervals: 20s Eye Care, 30s Water Break",
                            checked = settings.debugModeActive,
                            onCheckedChange = onToggleDebugMode
                        )

                        HorizontalDivider(color = DarkBorder)

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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { TimerEngine.triggerEyeRestAlert() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Test Eye Alert", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { TimerEngine.triggerWaterReminder() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Test Water Alert", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Section: About StudyPulse
            item {
                SectionHeader("ABOUT STUDYPULSE")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = StudyAccent,
        modifier = Modifier.padding(bottom = 6.dp)
    )
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
