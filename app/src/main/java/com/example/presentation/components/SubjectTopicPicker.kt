package com.example.presentation.components

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

    val filteredTopics = remember(topics, selectedSubject) {
        if (selectedSubject != null && selectedSubject.id > 0) {
            topics.filter { it.subjectId == selectedSubject.id }
        } else {
            emptyList()
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CharcoalBorder)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
                    color = AmberAccent
                )
                TextButton(
                    onClick = onOpenManageSubjects,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Manage Subjects/Tasks",
                        tint = AmberAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Manage",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent
                    )
                }
            }

            // Subject Selector Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    onClick = { subjectExpanded = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF1E1E1E)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CharcoalBorder)),
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
                            Text("Subject/Task", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = selectedSubject?.name ?: if (subjectOptional) "None (Optional)" else "Select Subject/Task",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                    }
                }

                DropdownMenu(
                    expanded = subjectExpanded,
                    onDismissRequest = { subjectExpanded = false },
                    modifier = Modifier
                        .background(CharcoalCard)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                ) {
                    if (subjectOptional) {
                        DropdownMenuItem(
                            text = { Text("None (No Subject/Task)", color = TextSecondary) },
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
                                    color = if (subject.id == selectedSubject?.id) AmberAccent else TextPrimary,
                                    fontWeight = if (subject.id == selectedSubject?.id) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSubjectSelected(subject)
                                subjectExpanded = false
                            }
                        )
                    }
                    HorizontalDivider(color = CharcoalBorder)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Add new subject/task...", color = AmberAccent, fontWeight = FontWeight.Bold)
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
                    colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF1E1E1E)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CharcoalBorder)),
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
                            Text("Topic/Sub task", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = selectedTopicName.ifBlank { "Select or Add Topic/Sub task" },
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                    }
                }

                DropdownMenu(
                    expanded = topicExpanded,
                    onDismissRequest = { topicExpanded = false },
                    modifier = Modifier
                        .background(CharcoalCard)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                ) {
                    if (filteredTopics.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No topics/sub tasks under this subject/task yet", color = TextMuted, fontSize = 12.sp) },
                            onClick = {}
                        )
                    } else {
                        filteredTopics.forEach { topic ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = topic.name,
                                        color = if (topic.name == selectedTopicName) AmberAccent else TextPrimary,
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
                    HorizontalDivider(color = CharcoalBorder)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Add new topic/sub task...", color = AmberAccent, fontWeight = FontWeight.Bold)
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

    // Quick Add Subject Dialog
    if (showNewSubjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewSubjectDialog = false },
            title = { Text("New Subject/Task", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSubjectName,
                    onValueChange = { newSubjectName = it },
                    label = { Text("Subject/Task Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
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
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
                ) {
                    Text("ADD")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSubjectDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = CharcoalCard
        )
    }

    // Quick Add Topic Dialog
    if (showNewTopicDialog) {
        AlertDialog(
            onDismissRequest = { showNewTopicDialog = false },
            title = { Text("New Topic/Sub task", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Adding to: ${selectedSubject?.name ?: "Current Subject/Task"}",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newTopicName,
                        onValueChange = { newTopicName = it },
                        label = { Text("Topic/Sub task Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
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
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
                ) {
                    Text("ADD")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewTopicDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = CharcoalCard
        )
    }
}
