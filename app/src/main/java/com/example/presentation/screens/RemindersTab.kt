package com.example.presentation.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.core.reminder.Reminder
import com.example.core.reminder.ReminderPlanner
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val reminderTimeFormat = SimpleDateFormat("EEE, d MMM  h:mm a", Locale.getDefault())
private val reminderDateFormat = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
private val reminderClockFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

private fun repeatSummary(r: Reminder): String {
    val repeat = when {
        r.untilDone -> "Every ${r.repeatEveryMinutes} min until Done"
        r.repeatTimes <= 1 -> "Alerts once"
        else -> "Every ${r.repeatEveryMinutes} min, ${r.repeatTimes} times"
    }
    return if (r.repeatDaily) "Daily • $repeat" else repeat
}

private fun statusSummary(r: Reminder): String = when {
    r.completed -> "Completed"
    !r.enabled -> "Off"
    r.firedCount > 0 -> "Reminding… (${r.firedCount} sent)"
    else -> "Next: ${reminderTimeFormat.format(Date(r.nextAlertAtMillis))}"
}

@Composable
fun RemindersTab(
    reminders: List<Reminder>,
    defaultMinutes: Int,
    defaultTimes: Int,
    onAddClick: () -> Unit,
    onEdit: (Reminder) -> Unit,
    onToggle: (Reminder, Boolean) -> Unit,
    onDelete: (Reminder) -> Unit,
    onEditDefaults: () -> Unit
) {
    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isExisting = isExistingTheme(activeTheme)
    val textColor = if (isExisting) TextPrimary else tokens.text
    val subTextColor = if (isExisting) TextSecondary else tokens.secondaryText
    val accent = if (isExisting) AmberAccent else tokens.accent

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Default repeat: every $defaultMinutes min, $defaultTimes times",
                style = MaterialTheme.typography.bodySmall,
                color = subTextColor,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onEditDefaults) {
                Text("CHANGE", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No reminders yet", color = subTextColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onAddClick,
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                    ) {
                        Text("+ Add Reminder")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    ThemedCard(
                        cornerRadius = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.text,
                                    fontWeight = FontWeight.Bold,
                                    color = if (reminder.enabled && !reminder.completed) textColor else subTextColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = statusSummary(reminder),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accent
                                )
                                Text(
                                    text = repeatSummary(reminder),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subTextColor
                                )
                            }
                            Switch(
                                checked = reminder.enabled && !reminder.completed,
                                enabled = !reminder.completed,
                                onCheckedChange = { onToggle(reminder, it) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { onEdit(reminder) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit reminder", tint = subTextColor)
                            }
                            IconButton(onClick = { onDelete(reminder) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete reminder", tint = subTextColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Add or edit a reminder. Empty repeat fields fall back to the saved defaults.
 */
@Composable
fun ReminderDialog(
    initial: Reminder?,
    defaultMinutes: Int,
    defaultTimes: Int,
    onDismiss: () -> Unit,
    onSave: (text: String, triggerAtMillis: Long, daily: Boolean, minutes: Int, times: Int, untilDone: Boolean) -> Unit
) {
    val context = LocalContext.current

    var text by remember { mutableStateOf(initial?.text ?: "") }
    var triggerAt by remember {
        mutableLongStateOf(initial?.triggerAtMillis ?: (System.currentTimeMillis() + 10 * 60_000L))
    }
    var daily by remember { mutableStateOf(initial?.repeatDaily ?: false) }
    var untilDone by remember { mutableStateOf(initial?.untilDone ?: false) }
    var minutesText by remember { mutableStateOf(initial?.repeatEveryMinutes?.toString() ?: "") }
    var timesText by remember { mutableStateOf(initial?.repeatTimes?.toString() ?: "") }

    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isExisting = isExistingTheme(activeTheme)
    val dialogBg = if (isExisting) CharcoalCard else if (activeTheme == "Ultra Premium") Color(0xFF171D26) else tokens.cardFill
    val textColor = if (isExisting) TextPrimary else tokens.text
    val subTextColor = if (isExisting) TextSecondary else tokens.secondaryText
    val accent = if (isExisting) AmberAccent else tokens.accent
    val borderColor = if (isExisting) CharcoalBorder else tokens.divider

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = {
            Text(
                if (initial == null) "Add Reminder" else "Edit Reminder",
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("What should I remind you about?") },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = borderColor
                    )
                )

                Text("When", color = subTextColor, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = triggerAt }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val updated = Calendar.getInstance().apply {
                                        timeInMillis = triggerAt
                                        set(year, month, day)
                                    }
                                    triggerAt = updated.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(reminderDateFormat.format(Date(triggerAt)), color = textColor, fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = triggerAt }
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val updated = Calendar.getInstance().apply {
                                        timeInMillis = triggerAt
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    triggerAt = updated.timeInMillis
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                false
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(reminderClockFormat.format(Date(triggerAt)), color = textColor, fontSize = 13.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Repeat every day", color = textColor, modifier = Modifier.weight(1f))
                    Switch(checked = daily, onCheckedChange = { daily = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Keep reminding until I press Done", color = textColor)
                        Text("Repeats every N minutes with no limit", color = subTextColor, fontSize = 11.sp)
                    }
                    Switch(checked = untilDone, onCheckedChange = { untilDone = it })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Every (min)") },
                        placeholder = { Text("$defaultMinutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = borderColor
                        )
                    )
                    OutlinedTextField(
                        value = timesText,
                        onValueChange = { timesText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Times") },
                        placeholder = { Text("$defaultTimes") },
                        singleLine = true,
                        enabled = !untilDone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = borderColor
                        )
                    )
                }
                Text(
                    "Leave Every and Times empty to use $defaultMinutes min, $defaultTimes times.",
                    color = subTextColor,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutes = minutesText.toIntOrNull()
                        ?.coerceIn(ReminderPlanner.MIN_REPEAT_MINUTES, ReminderPlanner.MAX_REPEAT_MINUTES)
                        ?: defaultMinutes
                    val times = timesText.toIntOrNull()
                        ?.coerceIn(ReminderPlanner.MIN_REPEAT_TIMES, ReminderPlanner.MAX_REPEAT_TIMES)
                        ?: defaultTimes
                    when {
                        text.isBlank() ->
                            Toast.makeText(context, "Write what to remind you about", Toast.LENGTH_SHORT).show()
                        !daily && triggerAt <= System.currentTimeMillis() ->
                            Toast.makeText(context, "Choose a time in the future", Toast.LENGTH_SHORT).show()
                        else -> onSave(text.trim(), triggerAt, daily, minutes, times, untilDone)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = if (isExisting) Color.Black else tokens.textOnAccent
                )
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = subTextColor) }
        },
        containerColor = dialogBg
    )
}

/** Change the default repeat values used when a reminder leaves them empty. */
@Composable
fun ReminderDefaultsDialog(
    defaultMinutes: Int,
    defaultTimes: Int,
    onDismiss: () -> Unit,
    onSave: (minutes: Int, times: Int) -> Unit
) {
    var minutesText by remember { mutableStateOf(defaultMinutes.toString()) }
    var timesText by remember { mutableStateOf(defaultTimes.toString()) }

    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isExisting = isExistingTheme(activeTheme)
    val dialogBg = if (isExisting) CharcoalCard else if (activeTheme == "Ultra Premium") Color(0xFF171D26) else tokens.cardFill
    val textColor = if (isExisting) TextPrimary else tokens.text
    val subTextColor = if (isExisting) TextSecondary else tokens.secondaryText
    val accent = if (isExisting) AmberAccent else tokens.accent
    val borderColor = if (isExisting) CharcoalBorder else tokens.divider

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default repeat", color = textColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Used when you leave Every and Times empty in a new reminder.",
                    color = subTextColor,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Every (minutes)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = borderColor
                    )
                )
                OutlinedTextField(
                    value = timesText,
                    onValueChange = { timesText = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Times (total alerts)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = borderColor
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        minutesText.toIntOrNull() ?: defaultMinutes,
                        timesText.toIntOrNull() ?: defaultTimes
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = if (isExisting) Color.Black else tokens.textOnAccent
                )
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = subTextColor) }
        },
        containerColor = dialogBg
    )
}
