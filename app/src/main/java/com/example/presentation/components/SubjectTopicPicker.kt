package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SubjectEntity
import com.example.data.db.TopicEntity
import com.example.ui.theme.*

@Composable
fun SubjectTopicPicker(
    subjects: List<SubjectEntity>,
    selectedSubject: SubjectEntity?,
    onSubjectSelected: (SubjectEntity) -> Unit,
    topics: List<TopicEntity>,
    selectedTopicName: String,
    onTopicSelected: (String) -> Unit,
    onOpenManageSubjects: () -> Unit,
    onQuickAddSubject: (String) -> Unit,
    onQuickAddTopic: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    subjectOptional: Boolean = false
) {
    var subjectExpanded by remember { mutableStateOf(false) }
    var topicExpanded by remember { mutableStateOf(false) }

    var showNewSubjectDialog by remember { mutableStateOf(false) }
    var showNewTopicDialog by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }
    var newTopicName by remember { mutableStateOf("") }

    val activeTheme = LocalActiveTheme.current
    val tokens = LocalAppThemeTokens.current
    val isExisting = isExistingTheme(activeTheme)

    val filteredTopics = remember(topics, selectedSubject) {
        if (selectedSubject != null && selectedSubject.id > 0) {
            topics.filter { it.subjectId == selectedSubject.id }
        } else {
            emptyList()
        }
    }

    ThemedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (subjectOptional) "TARGET SUBJECT/TASK & TOPIC/SUB TASK (OPTIONAL)" else "TARGET SUBJECT/TASK & TOPIC/SUB TASK",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = if (isExisting) AmberAccent else tokens.accent
                )
                TextButton(
                    onClick = onOpenManageSubjects,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Manage Subjects/Tasks",
                        tint = if (isExisting) AmberAccent else tokens.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Manage",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExisting) AmberAccent else tokens.accent
                    )
                }
            }

            val cardInnerBg = if (isExisting) Color(0xFF1E1E1E) else if (activeTheme == "Ultra Premium") Color.White.copy(alpha = 0.05f) else tokens.chipFill
            val dropdownBg = if (isExisting) CharcoalCard else if (activeTheme == "Ultra Premium") Color(0xFF0E2340) else tokens.cardFill

            // Subject Selector Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    onClick = { subjectExpanded = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = cardInnerBg),
                    border = if (isExisting) BorderStroke(1.dp, CharcoalBorder) else BorderStroke(0.dp, Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Subject/Task", style = MaterialTheme.typography.labelSmall, color = if (isExisting) TextMuted else tokens.secondaryText)
                            Text(
                                text = selectedSubject?.name ?: if (subjectOptional) "None (Optional)" else "Select Subject/Task",
                                fontWeight = FontWeight.Bold,
                                color = if (isExisting) TextPrimary else tokens.text,
                                fontSize = 14.sp
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = if (isExisting) TextMuted else tokens.secondaryText)
                    }
                }

                DropdownMenu(
                    expanded = subjectExpanded,
                    onDismissRequest = { subjectExpanded = false },
                    modifier = if (isExisting) {
                        Modifier
                            .background(CharcoalCard)
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                    } else {
                        Modifier.background(dropdownBg)
                    }
                ) {
                    if (subjectOptional) {
                        DropdownMenuItem(
                            text = { Text("None (No Subject/Task)", color = if (isExisting) TextSecondary else tokens.secondaryText) },
                            onClick = {
                                onSubjectSelected(SubjectEntity(name = "General", id = 0L))
                                subjectExpanded = false
                            }
                        )
                    }
                    subjects.forEach { subject ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = subject.name,
                                    color = if (subject.id == selectedSubject?.id) (if (isExisting) AmberAccent else tokens.accent) else (if (isExisting) TextPrimary else tokens.text),
                                    fontWeight = if (subject.id == selectedSubject?.id) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSubjectSelected(subject)
                                subjectExpanded = false
                            }
                        )
                    }
                    HorizontalDivider(color = if (isExisting) CharcoalBorder else tokens.divider)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = if (isExisting) AmberAccent else tokens.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Add new subject/task...", color = if (isExisting) AmberAccent else tokens.accent, fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = {
                            subjectExpanded = false
                            newSubjectName = ""
                            showNewSubjectDialog = true
                        }
                    )
                }
            }

            // Topic Selector Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    onClick = { topicExpanded = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = cardInnerBg),
                    border = if (isExisting) BorderStroke(1.dp, CharcoalBorder) else BorderStroke(0.dp, Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Topic/Sub task", style = MaterialTheme.typography.labelSmall, color = if (isExisting) TextMuted else tokens.secondaryText)
                            Text(
                                text = selectedTopicName.ifBlank { "Select or Add Topic/Sub task" },
                                fontWeight = FontWeight.Bold,
                                color = if (isExisting) TextPrimary else tokens.text,
                                fontSize = 14.sp
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = if (isExisting) TextMuted else tokens.secondaryText)
                    }
                }

                DropdownMenu(
                    expanded = topicExpanded,
                    onDismissRequest = { topicExpanded = false },
                    modifier = if (isExisting) {
                        Modifier
                            .background(CharcoalCard)
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                    } else {
                        Modifier.background(dropdownBg)
                    }
                ) {
                    if (filteredTopics.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No topics/sub tasks under this subject/task yet", color = if (isExisting) TextMuted else tokens.secondaryText, fontSize = 12.sp) },
                            onClick = {}
                        )
                    } else {
                        filteredTopics.forEach { topic ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = topic.name,
                                        color = if (topic.name == selectedTopicName) (if (isExisting) AmberAccent else tokens.accent) else (if (isExisting) TextPrimary else tokens.text),
                                        fontWeight = if (topic.name == selectedTopicName) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onTopicSelected(topic.name)
                                    topicExpanded = false
                                }
                            )
                        }
                    }
                    HorizontalDivider(color = if (isExisting) CharcoalBorder else tokens.divider)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = if (isExisting) AmberAccent else tokens.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Add new topic/sub task...", color = if (isExisting) AmberAccent else tokens.accent, fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = {
                            topicExpanded = false
                            newTopicName = ""
                            showNewTopicDialog = true
                        }
                    )
                }
            }
        }
    }

    val dialogContainer = if (isExisting) CharcoalCard else if (activeTheme == "Ultra Premium") Color(0xFF0E2340) else tokens.cardFill
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = if (isExisting) AmberAccent else tokens.accent,
        unfocusedBorderColor = if (isExisting) CharcoalBorder else tokens.chipFill,
        focusedTextColor = if (isExisting) TextPrimary else tokens.text,
        unfocusedTextColor = if (isExisting) TextPrimary else tokens.text,
        focusedLabelColor = if (isExisting) AmberAccent else tokens.accent,
        unfocusedLabelColor = if (isExisting) TextSecondary else tokens.secondaryText,
        cursorColor = if (isExisting) AmberAccent else tokens.accent,
        focusedContainerColor = if (isExisting) Color.Transparent else if (activeTheme == "Ultra Premium") Color.White.copy(alpha = 0.08f) else Color.Transparent,
        unfocusedContainerColor = if (isExisting) Color.Transparent else if (activeTheme == "Ultra Premium") Color.White.copy(alpha = 0.08f) else Color.Transparent
    )

    // Quick Add Subject Dialog
    if (showNewSubjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewSubjectDialog = false },
            title = { Text("New Subject/Task", color = if (isExisting) TextPrimary else tokens.text, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSubjectName,
                    onValueChange = { newSubjectName = it },
                    label = { Text("Subject/Task Name") },
                    singleLine = true,
                    colors = textFieldColors
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubjectName.isNotBlank()) {
                            onQuickAddSubject(newSubjectName.trim())
                            showNewSubjectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExisting) AmberAccent else tokens.accent,
                        contentColor = if (isExisting) Color.Black else tokens.textOnAccent
                    )
                ) {
                    Text("ADD")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSubjectDialog = false }) {
                    Text("CANCEL", color = if (isExisting) TextSecondary else tokens.secondaryText)
                }
            },
            containerColor = dialogContainer
        )
    }

    // Quick Add Topic Dialog
    if (showNewTopicDialog) {
        AlertDialog(
            onDismissRequest = { showNewTopicDialog = false },
            title = { Text("New Topic/Sub task", color = if (isExisting) TextPrimary else tokens.text, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Adding to: ${selectedSubject?.name ?: "Current Subject/Task"}",
                        color = if (isExisting) TextSecondary else tokens.secondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newTopicName,
                        onValueChange = { newTopicName = it },
                        label = { Text("Topic/Sub task Name") },
                        singleLine = true,
                        colors = textFieldColors
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTopicName.isNotBlank()) {
                            val subjectId = selectedSubject?.id ?: 1L
                            onQuickAddTopic(subjectId, newTopicName.trim())
                            onTopicSelected(newTopicName.trim())
                            showNewTopicDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExisting) AmberAccent else tokens.accent,
                        contentColor = if (isExisting) Color.Black else tokens.textOnAccent
                    )
                ) {
                    Text("ADD")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewTopicDialog = false }) {
                    Text("CANCEL", color = if (isExisting) TextSecondary else tokens.secondaryText)
                }
            },
            containerColor = dialogContainer
        )
    }
}
