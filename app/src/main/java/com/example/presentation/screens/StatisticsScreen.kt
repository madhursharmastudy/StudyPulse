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
import com.example.data.db.StudySessionEntity
import com.example.data.db.SubjectEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticsScreen(
    sessions: List<StudySessionEntity>,
    subjects: List<SubjectEntity>,
    onDeleteSession: (Long) -> Unit,
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

    val totalProductiveSeconds = filteredSessions.sumOf { it.productiveStudySeconds }
    val totalSessionsCount = filteredSessions.size
    val completedSessionsCount = filteredSessions.count { it.status == "COMPLETED" }
    val totalEyeRestSeconds = filteredSessions.sumOf { it.eyeRestSeconds }
    val totalWaterBreaks = filteredSessions.count { it.waterBreakSeconds > 0 }
    val totalInterruptions = filteredSessions.sumOf { it.interruptionCount }

    val longestSeconds = filteredSessions.maxOfOrNull { it.productiveStudySeconds } ?: 0L
    val avgSeconds = if (totalSessionsCount > 0) totalProductiveSeconds / totalSessionsCount else 0L

    // Study Score (0-100) based on completed sessions & low interruption ratio
    val studyScore = if (totalSessionsCount == 0) 85 else {
        val completionRatio = completedSessionsCount.toFloat() / totalSessionsCount.toFloat()
        val interruptionPenalty = (totalInterruptions * 2).coerceAtMost(30)
        ((completionRatio * 70) + 30 - interruptionPenalty).toInt().coerceIn(30, 100)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
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
                color = AmberAccent
            )
        }

        // Timeframe selector tabs
        item {
            TabRow(
                selectedTabIndex = selectedFilterTab,
                containerColor = CharcoalCard,
                contentColor = AmberAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFilterTab]),
                        color = AmberAccent
                    )
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
                                color = if (selectedFilterTab == index) AmberAccent else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
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

        // Secondary Metrics Grid
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CharcoalBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SmallMetricItem("Completed", "$completedSessionsCount / $totalSessionsCount")
                        SmallMetricItem("Longest", formatSecsToHoursMins(longestSeconds))
                        SmallMetricItem("Average", formatSecsToHoursMins(avgSeconds))
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = CharcoalBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SmallMetricItem("Eye Rests", "${totalEyeRestSeconds / 20} times")
                        SmallMetricItem("Hydration", "$totalWaterBreaks logged")
                        SmallMetricItem("Interruptions", "$totalInterruptions total")
                    }
                }
            }
        }

        // Subject Breakdown Progress
        item {
            Text("Subject Distribution", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CharcoalBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (subjects.isEmpty() || totalProductiveSeconds == 0L) {
                        Text("No recorded study data yet for this timeframe.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
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
                                    Text(subject.name, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                                    Text("${formatSecsToHoursMins(subSecs)} ($percent%)", color = AmberAccent, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AmberAccent,
                                    trackColor = CharcoalElevated
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Sessions Log
        item {
            Text("Recent Sessions (${filteredSessions.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
        }

        items(filteredSessions) { session ->
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            val dateStr = sdf.format(Date(session.startTimestamp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CharcoalBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(session.subjectName, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("• ${session.topicName}", color = TextSecondary, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$dateStr • Productive: ${formatSecsToHoursMins(session.productiveStudySeconds)} • Breaks: ${session.breakSeconds + session.eyeRestSeconds + session.waterBreakSeconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    IconButton(onClick = { onDeleteSession(session.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Session", tint = TextMuted, modifier = Modifier.size(18.dp))
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
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CharcoalBorder)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
fun SmallMetricItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
    }
}

fun formatSecsToHoursMins(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
