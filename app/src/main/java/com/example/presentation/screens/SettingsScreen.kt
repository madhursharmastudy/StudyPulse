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
import com.example.presentation.components.AppBackground
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
    onUpdateTheme: (String) -> Unit = {},
    onUpdateBackgroundStyle: (String) -> Unit = {},
    onUpdateAppTheme: (String) -> Unit = {},
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
    onUpdateNotificationsEnabled: (Boolean) -> Unit = {},
    onUpdateKeepScreenOn: (Boolean) -> Unit,
    onToggleDebugMode: (Boolean) -> Unit,
    onExportJsonClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onExportTxtClick: () -> Unit = {},
    onOpenLogBook: () -> Unit,
    onUpdateDailyTarget: (Int) -> Unit = {},
    onResetTodayStats: () -> Unit = {},
    onClearAllData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalActiveTheme.current
    val themeTokens = LocalAppThemeTokens.current
    val isExisting = isExistingTheme(activeTheme)
    val textColor = if (isExisting) TextPrimary else themeTokens.text
    val subTextColor = if (isExisting) TextSecondary else themeTokens.secondaryText
    val accentColor = if (isExisting) (if (settings.appTheme.equals("Blue", ignoreCase = true)) WaterAccent else StudyAccent) else themeTokens.accent
    val dialogBg = if (isExisting) DarkCard else if (activeTheme == "Ultra Premium") Color(0xFF171D26) else themeTokens.cardFill
    val dividerColor = if (isExisting) DarkBorder else themeTokens.divider

    AppBackground(modifier = modifier) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "SETTINGS",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = textColor
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
                                tint = accentColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
        var showResetStatsDialog by remember { mutableStateOf(false) }
        var showClearAllDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current

        val isCurrentClassic = isExistingTheme(activeTheme)
        val dialogShape = if (isCurrentClassic) RoundedCornerShape(16.dp) else RoundedCornerShape(24.dp)

        if (showResetStatsDialog) {
            AlertDialog(
                onDismissRequest = { showResetStatsDialog = false },
                title = { Text("Reset today's stats", fontWeight = FontWeight.Bold, color = textColor) },
                text = { Text("Are you sure you want to reset today's study time and break counts? All past days will stay intact.", color = subTextColor) },
                confirmButton = {
                    ThemedActionPill(
                        text = "Confirm",
                        isFilled = true,
                        onClick = {
                            onResetTodayStats()
                            showResetStatsDialog = false
                            Toast.makeText(context, "Today's stats have been reset", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                dismissButton = {
                    ThemedActionPill(
                        text = "Cancel",
                        isFilled = false,
                        onClick = { showResetStatsDialog = false }
                    )
                },
                shape = dialogShape,
                containerColor = dialogBg
            )
        }

        if (showClearAllDialog) {
            val dangerColor = themeTokens.danger
            AlertDialog(
                onDismissRequest = { showClearAllDialog = false },
                title = { Text("Clear all data", fontWeight = FontWeight.Bold, color = textColor) },
                text = { Text("Are you sure you want to delete all saved study sessions and stats history? Your settings will remain unchanged.", color = subTextColor) },
                confirmButton = {
                    ThemedActionPill(
                        text = "Confirm",
                        isFilled = true,
                        textColor = dangerColor,
                        onClick = {
                            onClearAllData()
                            showClearAllDialog = false
                            Toast.makeText(context, "All study data cleared", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                dismissButton = {
                    ThemedActionPill(
                        text = "Cancel",
                        isFilled = false,
                        onClick = { showClearAllDialog = false }
                    )
                },
                shape = dialogShape,
                containerColor = dialogBg
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: STUDY (O1)
            item {
                SettingsCard(
                    title = "STUDY",
                    subtitle = "Your daily study goal"
                ) {
                    Text("Daily target (hours)", fontWeight = FontWeight.Bold, color = textColor)
                    val targets = listOf(2, 4, 6, 8, 10)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        targets.forEach { target ->
                            val isSelected = settings.dailyTargetHours == target
                            ThemedOptionChip(
                                text = "${target}h",
                                isSelected = isSelected,
                                onClick = { onUpdateDailyTarget(target) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // SECTION 2: REMINDERS (O1)
            item {
                val bothRemindersOff = !settings.waterReminderEnabled && !settings.stretchBreakEnabled
                val dependentAlpha = if (bothRemindersOff) 0.4f else 1f

                SettingsCard(
                    title = "REMINDERS",
                    subtitle = "Configure healthy study habits and break alerts"
                ) {
                    SettingSwitchRow(
                        title = "Eye Care (20-20-20)",
                        subtitle = "Pause study timer and rest eyes periodically (20 min)",
                        extraNote = "When OFF: the Eye Rest card is hidden and no eye-rest breaks appear.",
                        checked = settings.eyeCareEnabled,
                        onCheckedChange = onUpdateEyeCareEnabled
                    )
                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Water Reminder",
                        subtitle = "Periodic reminders to drink water and take a short walk",
                        extraNote = "When OFF: no water reminders. If Stretch Break is also OFF, the Water & Stretch card is hidden.",
                        checked = settings.waterReminderEnabled,
                        onCheckedChange = onUpdateWaterEnabled
                    )
                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Stretch Break",
                        subtitle = "Gentle ergonomic exercises for neck, shoulders, back & legs",
                        extraNote = "When OFF: no stretch exercises. If Water Reminder is also OFF, the Water & Stretch card is hidden.",
                        checked = settings.stretchBreakEnabled,
                        onCheckedChange = onUpdateStretchBreakEnabled
                    )
                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))

                    // Break interval (dimmed & disabled if both off)
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            "Break interval",
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = dependentAlpha)
                        )
                        Text(
                            "Shared by Water Reminder and Stretch Break",
                            fontSize = 12.sp,
                            color = subTextColor.copy(alpha = dependentAlpha)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val intervals = listOf(30, 45, 60)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            intervals.forEach { interval ->
                                val isSelected = settings.breakIntervalMinutes == interval
                                ThemedOptionChip(
                                    text = "$interval min",
                                    isSelected = isSelected,
                                    onClick = {
                                        if (!bothRemindersOff) {
                                            onUpdateBreakInterval(interval)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Short mode (3 min)",
                        subtitle = "1 min water + 3 exercises instead of 6 exercises",
                        checked = settings.shortModeEnabled,
                        enabled = !bothRemindersOff,
                        alpha = dependentAlpha,
                        onCheckedChange = onUpdateShortModeEnabled
                    )
                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Only sitting exercises",
                        subtitle = "Filters out standing exercises (calf raises, back extension)",
                        checked = !settings.includeStandingExercises,
                        enabled = !bothRemindersOff,
                        alpha = dependentAlpha,
                        onCheckedChange = { sittingOnly -> onUpdateIncludeStandingExercises(!sittingOnly) }
                    )
                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Auto-Resume After Break",
                        subtitle = "Automatically resume study when break or eye rest finishes",
                        checked = settings.eyeAutoResume,
                        onCheckedChange = {
                            onUpdateEyeAutoResume(it)
                            onUpdateWaterAutoResume(it)
                        }
                    )
                }
            }

            // SECTION 3: ALERTS (O1)
            item {
                SettingsCard(
                    title = "ALERTS",
                    subtitle = "Notifications, tones and vibration"
                ) {
                    SettingSwitchRow(
                        title = "Notifications",
                        subtitle = "Master switch for break alerts, session completions and reminders",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = onUpdateNotificationsEnabled,
                        bottomContent = {
                            TextButton(
                                onClick = {
                                    val intent = android.content.Intent().apply {
                                        action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        val fallback = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                        context.startActivity(fallback)
                                    }
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = "Open phone notification settings",
                                    fontSize = 12.sp,
                                    color = if (isExisting) StudyAccent else themeTokens.accent
                                )
                            }
                        }
                    )
                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Sound",
                        subtitle = "Play tones for session & break start/end",
                        checked = settings.soundEnabled,
                        onCheckedChange = onUpdateSound
                    )
                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Vibration",
                        subtitle = "Haptic feedback for timer alerts and transitions",
                        checked = settings.vibrationEnabled,
                        onCheckedChange = onUpdateVibration
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Phone Silent / Do Not Disturb can still mute tones.",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isExisting) TextMuted else themeTokens.secondaryText,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // SECTION 4: DISPLAY (O1)
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
                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))
                    SettingSwitchRow(
                        title = "Keep Screen Awake",
                        subtitle = "Keep the screen on while a timer is running",
                        checked = settings.keepScreenOn,
                        onCheckedChange = onUpdateKeepScreenOn
                    )
                }
            }

            // SECTION 5: THEME (F10, F11, F12)
            item {
                SettingsCard(
                    title = "THEME",
                    subtitle = "Choose how StudyPulse looks"
                ) {
                    val isCurrentClassic = isExistingTheme(activeTheme)

                    // 1. Dark (Default)
                    ThemeRow(
                        title = "Dark",
                        description = "Calm dark theme for long study",
                        pillText = "Default",
                        checked = settings.theme == "Dark",
                        onCheckedChange = { onUpdateTheme("Dark") },
                        pillAccent = Color(0xFF8F86FF),
                        tokens = themeTokens,
                        isClassicTheme = isCurrentClassic
                    )

                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))

                    // 2. Light
                    ThemeRow(
                        title = "Light",
                        description = "Bright theme for daytime",
                        pillText = "Daylight",
                        checked = settings.theme == "Light",
                        onCheckedChange = { onUpdateTheme("Light") },
                        pillAccent = Color(0xFF5B50E6),
                        tokens = themeTokens,
                        isClassicTheme = isCurrentClassic
                    )

                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))

                    // 3. Ultra Premium
                    ThemeRow(
                        title = "Ultra Premium",
                        description = "Cinematic dark theme with soft glow",
                        pillText = "Premium",
                        checked = settings.theme == "Ultra Premium",
                        onCheckedChange = { onUpdateTheme("Ultra Premium") },
                        pillAccent = Color(0xFFFFB52E),
                        tokens = themeTokens,
                        isClassicTheme = isCurrentClassic
                    )

                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))

                    // 4. Dark Black
                    ThemeRow(
                        title = "Dark Black",
                        description = "Pure black, saves battery on AMOLED",
                        pillText = "AMOLED",
                        checked = settings.theme == "Dark Black" || settings.theme == "Classic",
                        onCheckedChange = { onUpdateTheme("Dark Black") },
                        pillAccent = StudyAccent,
                        tokens = themeTokens,
                        isClassicTheme = isCurrentClassic
                    )

                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))

                    // 5. Glowing Blue
                    ThemeRow(
                        title = "Glowing Blue",
                        description = "Deep navy glow",
                        pillText = "Blue",
                        checked = settings.theme == "Glowing Blue",
                        onCheckedChange = { onUpdateTheme("Glowing Blue") },
                        pillAccent = WaterAccent,
                        tokens = themeTokens,
                        isClassicTheme = isCurrentClassic
                    )

                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))

                    // 6. Glowing Gold
                    ThemeRow(
                        title = "Glowing Gold",
                        description = "Ember gold glow",
                        pillText = "Gold",
                        checked = settings.theme == "Glowing Gold",
                        onCheckedChange = { onUpdateTheme("Glowing Gold") },
                        pillAccent = Color(0xFFFFA640),
                        tokens = themeTokens,
                        isClassicTheme = isCurrentClassic
                    )
                }
            }

            // SECTION 5: DATA (A3)
            item {
                SettingsCard(
                    title = "DATA",
                    subtitle = "Manage your saved study data"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showResetStatsDialog = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reset today's stats", fontWeight = FontWeight.Bold, color = textColor)
                            Text("Resets only today's study time and break counts", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                        }
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset today's stats",
                            tint = if (isExisting) StudyAccent else themeTokens.accent
                        )
                    }

                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 2.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearAllDialog = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clear all data", fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                            Text("Deletes all saved study sessions and stats history", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                        }
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear all data",
                            tint = Color(0xFFFF5252)
                        )
                    }
                }
            }

            // SECTION 6: ABOUT (O1 & F6)
            item {
                SettingsCard(
                    title = "ABOUT",
                    subtitle = "About StudyPulse"
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
                            color = textColor
                        )
                        Text(
                            text = "FOCUS. REST. HYDRATE. REPEAT.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = if (isExisting) StudyAccent else themeTokens.accent
                        )
                        Text(
                            text = "A minimalist AMOLED desk watch and productivity companion designed for deep focus, healthy 20-20-20 eye care, and regular hydration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = subTextColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showDevTools = !showDevTools }
                                .padding(6.dp)
                        )

                        if (showDevTools) {
                            Spacer(modifier = Modifier.height(4.dp))
                            ThemedActionPill(
                                text = "Open Diagnostic Log Book",
                                onClick = onOpenLogBook,
                                isFilled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_log_book_button")
                            )
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
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isClassic = isExistingTheme(activeTheme)
    val titleColor = tokens.accent
    val subtitleColor = if (isClassic) TextSecondary else tokens.secondaryText

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = titleColor,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = subtitleColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (isClassic) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = tokens.cardFill),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(tokens.divider)
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
        } else if (activeTheme == "Ultra Premium") {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = UltraPremiumGlass),
                border = null,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content()
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = tokens.cardFill),
                border = null,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    extraNote: String? = null,
    enabled: Boolean = true,
    alpha: Float = 1f,
    bottomContent: (@Composable () -> Unit)? = null
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isExisting = isExistingTheme(activeTheme)
    val titleColor = if (isExisting) TextPrimary.copy(alpha = alpha) else tokens.text.copy(alpha = alpha)
    val subtitleColor = if (isExisting) TextSecondary.copy(alpha = alpha) else tokens.secondaryText.copy(alpha = alpha)
    val noteColor = if (isExisting) TextMuted.copy(alpha = alpha) else tokens.secondaryText.copy(alpha = alpha * 0.85f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor
            )
            if (!extraNote.isNullOrEmpty()) {
                Text(
                    text = extraNote,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = noteColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (bottomContent != null) {
                Spacer(modifier = Modifier.height(2.dp))
                bottomContent()
            }
        }
        // B5: Switch: for Dark, Light and Ultra Premium set checkedBorderColor and uncheckedBorderColor to Color.Transparent
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.textOnAccent.copy(alpha = alpha),
                checkedTrackColor = tokens.accent.copy(alpha = alpha),
                uncheckedThumbColor = tokens.secondaryText.copy(alpha = alpha),
                uncheckedTrackColor = tokens.chipFill.copy(alpha = alpha),
                checkedBorderColor = if (isExisting) SwitchDefaults.colors().checkedBorderColor else Color.Transparent,
                uncheckedBorderColor = if (isExisting) SwitchDefaults.colors().uncheckedBorderColor else Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeRow(
    title: String,
    description: String,
    pillText: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    pillAccent: Color,
    tokens: AppThemeTokens,
    isClassicTheme: Boolean,
    bottomContent: (@Composable () -> Unit)? = null
) {
    val activeTheme = LocalActiveTheme.current
    val isExisting = isExistingTheme(activeTheme) || isClassicTheme
    val effectivePillAccent = if (isExisting) pillAccent else tokens.accent

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = if (isExisting) TextPrimary else tokens.text
                )
                Spacer(modifier = Modifier.height(2.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isExisting) TextSecondary else tokens.secondaryText
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = effectivePillAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = pillText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = effectivePillAccent,
                            softWrap = false,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = { if (it && !checked) onCheckedChange() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = tokens.textOnAccent,
                    checkedTrackColor = tokens.accent,
                    uncheckedThumbColor = tokens.secondaryText,
                    uncheckedTrackColor = tokens.chipFill,
                    checkedBorderColor = if (isExisting) SwitchDefaults.colors().checkedBorderColor else Color.Transparent,
                    uncheckedBorderColor = if (isExisting) SwitchDefaults.colors().uncheckedBorderColor else Color.Transparent
                )
            )
        }
        if (bottomContent != null) {
            bottomContent()
        }
    }
}
