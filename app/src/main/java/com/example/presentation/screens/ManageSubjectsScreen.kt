package com.example.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SubjectEntity
import com.example.data.db.TopicEntity
import com.example.presentation.components.AppBackground
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSubjectsScreen(
    subjects: List<SubjectEntity>,
    topics: List<TopicEntity>,
    onBack: () -> Unit,
    onAddSubject: (name: String, icon: String, colorHex: String, goalMins: Int) -> Unit,
    onUpdateSubject: (SubjectEntity) -> Unit,
    onDeleteSubject: (Long) -> Unit,
    onAddTopic: (subjectId: Long, name: String) -> Unit,
    onUpdateTopic: (TopicEntity) -> Unit,
    onDeleteTopic: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }

    var topicSubjectTarget by remember { mutableStateOf<SubjectEntity?>(null) }
    var topicToEdit by remember { mutableStateOf<TopicEntity?>(null) }
    var topicToDelete by remember { mutableStateOf<TopicEntity?>(null) }

    AppBackground(modifier = modifier) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MANAGE SUBJECTS/TASKS",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("manage_subjects_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AmberAccent
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddSubjectDialog = true },
                        modifier = Modifier.testTag("add_subject_button")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Subject/Task", tint = AmberAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Add, edit, or delete subjects/tasks and topics/sub tasks. Changes sync across all timers and study planner.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (subjects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No subjects/tasks found", color = TextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAddSubjectDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
                        ) {
                            Text("+ Add First Subject/Task")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(subjects, key = { it.id }) { subject ->
                        val subjectTopics = topics.filter { it.subjectId == subject.id }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CharcoalBorder)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(AmberAccent)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = subject.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { topicSubjectTarget = subject },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add Topic", tint = AmberAccent, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { subjectToEdit = subject },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Subject", tint = TextMuted, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { subjectToDelete = subject },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Subject", tint = TextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = CharcoalBorder)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Topics under subject
                                Text(
                                    text = "TOPICS (${subjectTopics.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                    color = TextMuted
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                if (subjectTopics.isEmpty()) {
                                    Text(
                                        text = "No topics yet. Tap + to add a topic.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        subjectTopics.forEach { topic ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF1E1E1E))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = topic.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = { topicToEdit = topic },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Topic", tint = TextMuted, modifier = Modifier.size(14.dp))
                                                    }
                                                    IconButton(
                                                        onClick = { topicToDelete = topic },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "Delete Topic", tint = TextMuted, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Subject Dialog
    if (showAddSubjectDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("Add Subject/Task", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject/Task Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddSubject(name.trim(), "book", "#E5A93C", 60)
                            showAddSubjectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
                ) {
                    Text("ADD")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubjectDialog = false }) { Text("CANCEL", color = TextSecondary) }
            },
            containerColor = CharcoalCard
        )
    }

    // Edit Subject Dialog
    if (subjectToEdit != null) {
        var name by remember { mutableStateOf(subjectToEdit!!.name) }
        AlertDialog(
            onDismissRequest = { subjectToEdit = null },
            title = { Text("Rename Subject/Task", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject/Task Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onUpdateSubject(subjectToEdit!!.copy(name = name.trim()))
                            subjectToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToEdit = null }) { Text("CANCEL", color = TextSecondary) }
            },
            containerColor = CharcoalCard
        )
    }

    // Delete Subject Confirmation
    if (subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete Subject/Task?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${subjectToDelete!!.name}\"? All topics/sub tasks under it will also be deleted.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSubject(subjectToDelete!!.id)
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                ) {
                    Text("DELETE")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) { Text("CANCEL", color = TextSecondary) }
            },
            containerColor = CharcoalCard
        )
    }

    // Add Topic Dialog
    if (topicSubjectTarget != null) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { topicSubjectTarget = null },
            title = { Text("Add Topic/Sub task to ${topicSubjectTarget!!.name}", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Topic/Sub task Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddTopic(topicSubjectTarget!!.id, name.trim())
                            topicSubjectTarget = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
                ) {
                    Text("ADD")
                }
            },
            dismissButton = {
                TextButton(onClick = { topicSubjectTarget = null }) { Text("CANCEL", color = TextSecondary) }
            },
            containerColor = CharcoalCard
        )
    }

    // Edit Topic Dialog
    if (topicToEdit != null) {
        var name by remember { mutableStateOf(topicToEdit!!.name) }
        AlertDialog(
            onDismissRequest = { topicToEdit = null },
            title = { Text("Rename Topic/Sub task", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Topic/Sub task Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onUpdateTopic(topicToEdit!!.copy(name = name.trim()))
                            topicToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToEdit = null }) { Text("CANCEL", color = TextSecondary) }
            },
            containerColor = CharcoalCard
        )
    }

    // Delete Topic Confirmation
    if (topicToDelete != null) {
        AlertDialog(
            onDismissRequest = { topicToDelete = null },
            title = { Text("Delete Topic/Sub task?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${topicToDelete!!.name}\"?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTopic(topicToDelete!!.id)
                        topicToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                ) {
                    Text("DELETE")
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToDelete = null }) { Text("CANCEL", color = TextSecondary) }
            },
            containerColor = CharcoalCard
        )
    }
}
}
