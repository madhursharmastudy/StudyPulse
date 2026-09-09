package com.example.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.logging.*
import com.example.ui.theme.*

@Composable
fun LogBookScreen(
    logs: List<LogEntry>,
    onClearLogs: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf<LogLevel?>(null) }
    var selectedFeature by remember { mutableStateOf<LogFeature?>(null) }

    val filteredLogs = remember(logs, searchQuery, selectedLevel, selectedFeature) {
        logs.filter { entry ->
            val matchesLevel = selectedLevel == null || entry.level == selectedLevel
            val matchesFeature = selectedFeature == null || entry.feature == selectedFeature
            val matchesSearch = if (searchQuery.isBlank()) true else {
                entry.event.contains(searchQuery, ignoreCase = true) ||
                        (entry.details?.contains(searchQuery, ignoreCase = true) == true) ||
                        (entry.sessionUid?.contains(searchQuery, ignoreCase = true) == true) ||
                        (entry.timerId?.contains(searchQuery, ignoreCase = true) == true) ||
                        (entry.subjectName?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesLevel && matchesFeature && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "DIAGNOSTIC LOG BOOK",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = AmberAccent
                    )
                    Text(
                        text = "${filteredLogs.size} of ${logs.size} records",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Row {
                IconButton(
                    onClick = {
                        val txt = AppLogger.exportToTxt()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("StudyWatchLogs", txt))
                        Toast.makeText(context, "Copied ${logs.size} logs to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("copy_logs_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary)
                }

                IconButton(
                    onClick = {
                        val txt = AppLogger.exportToTxt()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, txt)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share StudyPulse Logs"))
                    },
                    modifier = Modifier.testTag("share_logs_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary)
                }

                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = RoseAccent)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search event, session, timer, error...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("log_search_input"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AmberAccent,
                unfocusedBorderColor = CharcoalBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Level Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedLevel == null,
                onClick = { selectedLevel = null },
                label = { Text("ALL") }
            )
            listOf(LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR, LogLevel.CRITICAL).forEach { lvl ->
                FilterChip(
                    selected = selectedLevel == lvl,
                    onClick = { selectedLevel = if (selectedLevel == lvl) null else lvl },
                    label = { Text(lvl.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (lvl) {
                            LogLevel.ERROR, LogLevel.CRITICAL -> RoseSoft
                            LogLevel.WARNING -> AmberSoft
                            else -> CyanSoft
                        },
                        selectedLabelColor = when (lvl) {
                            LogLevel.ERROR, LogLevel.CRITICAL -> RoseAccent
                            LogLevel.WARNING -> AmberAccent
                            else -> CyanAccent
                        }
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logs List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredLogs) { entry ->
                LogEntryCard(entry = entry)
            }
        }
    }
}

@Composable
fun LogEntryCard(entry: LogEntry) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CharcoalBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (entry.level) {
                                    LogLevel.DEBUG -> CharcoalElevated
                                    LogLevel.INFO -> CyanSoft
                                    LogLevel.WARNING -> AmberSoft
                                    LogLevel.ERROR, LogLevel.CRITICAL -> RoseSoft
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.level.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = when (entry.level) {
                                LogLevel.DEBUG -> TextMuted
                                LogLevel.INFO -> CyanAccent
                                LogLevel.WARNING -> AmberAccent
                                LogLevel.ERROR, LogLevel.CRITICAL -> RoseAccent
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = entry.feature.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = AmberAccent
                    )
                }

                Text(
                    text = entry.formattedTime(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.event,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary
            )

            if (!entry.details.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.details,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Correlation IDs
            if (!entry.sessionUid.isNullOrEmpty() || !entry.timerId.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    entry.sessionUid?.let {
                        Text("Session: $it", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                    }
                    entry.timerId?.let {
                        Text("Timer: $it", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                    }
                }
            }

            if (!entry.exceptionMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Exception: ${entry.exceptionMessage}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = RoseAccent
                )
            }
        }
    }
}
