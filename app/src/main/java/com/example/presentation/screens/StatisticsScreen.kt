package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.BreakSessionEntity
import com.example.data.db.StudySessionEntity
import com.example.data.db.SubjectEntity
import com.example.core.stretch.StretchBreakManager
import com.example.presentation.components.AppBackground
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticsScreen(
    sessions: List<StudySessionEntity>,
    subjects: List<SubjectEntity>,
    breakSessions: List<BreakSessionEntity> = emptyList(),
    onDeleteSession: (Long) -> Unit,
    dailyTargetHours: Int = 6,
    modifier: Modifier = Modifier
) {
    var selectedFilterTab by remember { mutableStateOf(0) }
    val filterTabs = listOf("TODAY", "WEEK", "MONTH", "ALL TIME")

    val now = System.currentTimeMillis()
    val filteredSessions = remember(sessions, selectedFilterTab) {
        val calendar = Calendar.getInstance()
        when (selectedFilterTab) {
            0 -> {
                // Today
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                sessions.filter { it.startTimestamp >= startOfDay }
            }
            1 -> {
                // This Week
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val startOfWeek = calendar.timeInMillis
                sessions.filter { it.startTimestamp >= startOfWeek }
            }
            2 -> {
                // This Month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val startOfMonth = calendar.timeInMillis
                sessions.filter { it.startTimestamp >= startOfMonth }
            }
            else -> sessions
        }
    }

    val filteredBreakSessions = remember(breakSessions, selectedFilterTab) {
        val calendar = Calendar.getInstance()
        when (selectedFilterTab) {
            0 -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                breakSessions.filter { it.startTimestamp >= startOfDay }
            }
            1 -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val startOfWeek = calendar.timeInMillis
                breakSessions.filter { it.startTimestamp >= startOfWeek }
            }
            2 -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val startOfMonth = calendar.timeInMillis
                breakSessions.filter { it.startTimestamp >= startOfMonth }
            }
            else -> breakSessions
        }
    }

    val totalProductiveSeconds = filteredSessions.sumOf { it.productiveStudySeconds }
    val totalSessionsCount = filteredSessions.size
    val completedSessionsCount = filteredSessions.count { it.status == "COMPLETED" }
    val totalEyeRestSeconds = filteredSessions.sumOf { it.eyeRestSeconds }
    val totalWaterBreaks = filteredSessions.count { it.waterBreakSeconds > 0 }
    val totalStretchBreaks = filteredBreakSessions.count { it.breakType == "STRETCH_BREAK" }
    val totalInterruptions = filteredSessions.sumOf { it.interruptionCount }

    val longestSeconds = filteredSessions.maxOfOrNull { it.productiveStudySeconds } ?: 0L
    val avgSeconds = if (totalSessionsCount > 0) totalProductiveSeconds / totalSessionsCount else 0L

    // Study Score (0-100) based on completed sessions & low interruption ratio
    val studyScore = if (totalSessionsCount == 0) 85 else {
        val completionRatio = completedSessionsCount.toFloat() / totalSessionsCount.toFloat()
        val interruptionPenalty = (totalInterruptions * 2).coerceAtMost(30)
        ((completionRatio * 70) + 30 - interruptionPenalty).toInt().coerceIn(30, 100)
    }

    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isExisting = isExistingTheme(activeTheme)
    val accent = if (isExisting) (if (activeTheme == "Classic") AmberAccent else tokens.accent) else tokens.accent
    val textColor = if (isExisting) TextPrimary else tokens.text
    val subTextColor = if (isExisting) TextSecondary else tokens.secondaryText
    val tabContainer = if (isExisting) CharcoalCard else Color.Transparent
    val progressColor = if (isExisting) AmberAccent else tokens.accent
    val trackColor = if (isExisting) CharcoalElevated else if (activeTheme == "Ultra Premium") Color.White.copy(alpha = 0.10f) else tokens.chipFill

    AppBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            Text(
                text = "STUDY ANALYTICS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = if (isExisting) accent else tokens.text
            )
        }

        // Timeframe selector tabs
        item {
            TabRow(
                selectedTabIndex = selectedFilterTab,
                containerColor = tabContainer,
                contentColor = accent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFilterTab]),
                        color = accent
                    )
                },
                divider = {
                    HorizontalDivider(color = if (isExisting) CharcoalBorder else tokens.divider)
                }
            ) {
                filterTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedFilterTab == index,
                        onClick = { selectedFilterTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedFilterTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedFilterTab == index) accent else subTextColor,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }

        if (selectedFilterTab == 0) {
            item {
                val productiveMinutes = (totalProductiveSeconds / 60).toInt()
                val productiveDisplay = if (productiveMinutes >= 60) {
                    val h = productiveMinutes / 60
                    val m = productiveMinutes % 60
                    if (m > 0) "${h}h ${m}m" else "${h}h"
                } else {
                    "${productiveMinutes}m"
                }
                val targetDisplay = "${dailyTargetHours}h"
                val fraction = (totalProductiveSeconds.toFloat() / (dailyTargetHours * 3600f)).coerceAtMost(1f)
                val percent = (fraction * 100).toInt()

                ThemedCard(
                    cornerRadius = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Today's Target",
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "$productiveDisplay / $targetDisplay",
                            fontWeight = FontWeight.Black,
                            color = progressColor,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = trackColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$percent% of daily goal completed",
                        fontSize = 11.sp,
                        color = subTextColor
                    )
                }
            }
        }

        // Hero KPI Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "PRODUCTIVE TIME",
                    value = formatSecsToHoursMins(totalProductiveSeconds),
                    subtitle = "$totalSessionsCount total sessions",
                    accentColor = AmberAccent,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "FOCUS SCORE",
                    value = "$studyScore / 100",
                    subtitle = "High discipline",
                    accentColor = EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Breaks taken today (water + stretch count)
        item {
            val breaksToday = StretchBreakManager.getTodayBreakCount()
            ThemedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isExisting) Color(0x2200B4FF) else tokens.chipFill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Breaks",
                                tint = if (isExisting) WaterAccent else tokens.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Breaks taken today: $breaksToday",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                            Text(
                                text = "Water + stretch health routine",
                                style = MaterialTheme.typography.labelSmall,
                                color = subTextColor
                            )
                        }
                    }
                    Text(
                        text = "$breaksToday",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = if (isExisting) WaterAccent else tokens.accent
                    )
                }
            }
        }

        // Secondary Metrics Grid
        item {
            ThemedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SmallMetricItem("Completed", "$completedSessionsCount / $totalSessionsCount")
                    SmallMetricItem("Longest", formatSecsToHoursMins(longestSeconds))
                    SmallMetricItem("Average", formatSecsToHoursMins(avgSeconds))
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = if (isExisting) CharcoalBorder else tokens.divider)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SmallMetricItem("Eye Rests", "${totalEyeRestSeconds / 20} times")
                    SmallMetricItem("Hydration", "$totalWaterBreaks logged")
                    SmallMetricItem("Stretch breaks", "$totalStretchBreaks completed")
                }
            }
        }

        // Subject Breakdown Progress
        item {
            Text(
                "Subject/Task Distribution",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isExisting) TextPrimary else tokens.text
            )
        }

        item {
            ThemedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (subjects.isEmpty() || totalProductiveSeconds == 0L) {
                        Text(
                            "No recorded study data yet for this timeframe.",
                            color = if (isExisting) TextMuted else tokens.secondaryText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        subjects.forEach { subject ->
                            val subjectSessions = filteredSessions.filter { it.subjectName == subject.name }
                            val subSecs = subjectSessions.sumOf { it.productiveStudySeconds }
                            val fraction = if (totalProductiveSeconds > 0) subSecs.toFloat() / totalProductiveSeconds.toFloat() else 0f
                            val percent = (fraction * 100).toInt()

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        subject.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isExisting) TextPrimary else tokens.text,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "${formatSecsToHoursMins(subSecs)} ($percent%)",
                                        color = AmberAccent,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AmberAccent,
                                    trackColor = if (isExisting) CharcoalElevated else tokens.chipFill
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Sessions Log
        item {
            Text(
                "Recent Sessions (${filteredSessions.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isExisting) TextPrimary else tokens.text
            )
        }

        items(filteredSessions) { session ->
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            val dateStr = sdf.format(Date(session.startTimestamp))

            ThemedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                session.subjectName,
                                fontWeight = FontWeight.Bold,
                                color = if (isExisting) TextPrimary else tokens.text
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "• ${session.topicName}",
                                color = if (isExisting) TextSecondary else tokens.secondaryText,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$dateStr • Productive: ${formatSecsToHoursMins(session.productiveStudySeconds)} • Breaks: ${session.breakSeconds + session.eyeRestSeconds + session.waterBreakSeconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isExisting) TextMuted else tokens.secondaryText
                        )
                    }

                    IconButton(onClick = { onDeleteSession(session.id) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Session",
                            tint = if (isExisting) TextMuted else tokens.secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isExisting = isExistingTheme(activeTheme)
    val subColor = if (isExisting) TextSecondary else tokens.secondaryText

    ThemedCard(
        cornerRadius = 14.dp,
        modifier = modifier
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = subColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = if (isExisting) accentColor else tokens.accent
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subColor)
    }
}

@Composable
fun SmallMetricItem(label: String, value: String) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isExisting = isExistingTheme(activeTheme)
    val subColor = if (isExisting) TextMuted else tokens.secondaryText
    val valColor = if (isExisting) TextPrimary else tokens.text

    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = subColor)
        Text(value, fontWeight = FontWeight.Bold, color = valColor, fontSize = 14.sp)
    }
}

fun formatSecsToHoursMins(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
