package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import com.example.data.db.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class TodayItem {
    data class StudyBlock(val plan: StudyPlanEntity) : TodayItem()
    data class Revision(val item: RevisionItemEntity) : TodayItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    studyPlans: List<StudyPlanEntity>,
    exams: List<ExamEntity>,
    assignments: List<AssignmentEntity>,
    revisionItems: List<RevisionItemEntity>,
    subjects: List<SubjectEntity> = emptyList(),
    topics: List<TopicEntity> = emptyList(),
    onAddStudyPlan: (StudyPlanEntity) -> Unit,
    onUpdateStudyPlan: (StudyPlanEntity) -> Unit,
    onTogglePlanCompleted: (Long, Boolean) -> Unit,
    onDeleteStudyPlan: (Long) -> Unit,
    onAddExam: (ExamEntity) -> Unit,
    onUpdateExam: (ExamEntity) -> Unit,
    onDeleteExam: (Long) -> Unit,
    onAddAssignment: (AssignmentEntity) -> Unit,
    onUpdateAssignment: (AssignmentEntity) -> Unit,
    onDeleteAssignment: (Long) -> Unit,
    onAddRevisionItem: (RevisionItemEntity) -> Unit,
    onUpdateRevisionItem: (RevisionItemEntity) -> Unit,
    onDeleteRevisionItem: (Long) -> Unit,
    onOpenManageSubjects: () -> Unit = {},
    onQuickAddSubject: (String) -> Unit = {},
    onQuickAddTopic: (Long, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("TODAY", "TASKS", "EXAMS")

    var showAddPlanDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }
    var showAddAssignmentDialog by remember { mutableStateOf(false) }
    var showAddRevisionDialog by remember { mutableStateOf(false) }
    var showAddChooser by remember { mutableStateOf(false) }

    val todayEnd = remember(revisionItems) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    val todayRevisions = remember(revisionItems, todayEnd) {
        revisionItems.filter { it.nextRevisionTimestamp <= todayEnd && !it.isCompleted }
    }

    val todayItems = remember(studyPlans, todayRevisions) {
        val list = mutableListOf<TodayItem>()
        studyPlans.forEach { list.add(TodayItem.StudyBlock(it)) }
        todayRevisions.forEach { list.add(TodayItem.Revision(it)) }

        list.sortedBy { item ->
            when (item) {
                is TodayItem.StudyBlock -> item.plan.startTime
                is TodayItem.Revision -> "00:00" // Revision items listed first
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STUDY PLANNER",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = AmberAccent
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenManageSubjects,
                    modifier = Modifier.testTag("planner_manage_subjects_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Manage Subjects",
                        tint = AmberAccent
                    )
                }

                Box {
                    IconButton(
                        onClick = { showAddChooser = true },
                        modifier = Modifier.testTag("planner_add_button")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Item", tint = AmberAccent)
                    }

                    DropdownMenu(
                        expanded = showAddChooser,
                        onDismissRequest = { showAddChooser = false },
                        modifier = Modifier
                            .background(CharcoalCard)
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Study Block", color = TextPrimary, fontWeight = FontWeight.Medium) },
                            onClick = {
                                showAddChooser = false
                                showAddPlanDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Task", color = TextPrimary, fontWeight = FontWeight.Medium) },
                            onClick = {
                                showAddChooser = false
                                showAddAssignmentDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Exam", color = TextPrimary, fontWeight = FontWeight.Medium) },
                            onClick = {
                                showAddChooser = false
                                showAddExamDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Revision", color = TextPrimary, fontWeight = FontWeight.Medium) },
                            onClick = {
                                showAddChooser = false
                                showAddRevisionDialog = true
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CharcoalCard,
            contentColor = AmberAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AmberAccent
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) AmberAccent else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> TodayTab(
                items = todayItems,
                onToggleComplete = onTogglePlanCompleted,
                onDeleteStudyPlan = onDeleteStudyPlan,
                onUpdateStudyPlan = onUpdateStudyPlan,
                onDeleteRevision = onDeleteRevisionItem,
                onUpdateRevision = onUpdateRevisionItem,
                onAddClick = { showAddChooser = true }
            )
            1 -> AssignmentsTab(
                assignments = assignments,
                onDelete = onDeleteAssignment,
                onUpdate = onUpdateAssignment,
                onAddClick = { showAddAssignmentDialog = true }
            )
            2 -> ExamsTab(
                exams = exams,
                onDelete = onDeleteExam,
                onUpdate = onUpdateExam,
                onAddClick = { showAddExamDialog = true }
            )
        }
    }

    if (showAddPlanDialog) {
        AddStudyPlanDialog(
            subjects = subjects,
            onQuickAddSubject = onQuickAddSubject,
            onDismiss = { showAddPlanDialog = false },
            onAdd = {
                onAddStudyPlan(it)
                showAddPlanDialog = false
            }
        )
    }

    if (showAddExamDialog) {
        AddExamDialog(
            subjects = subjects,
            onQuickAddSubject = onQuickAddSubject,
            onDismiss = { showAddExamDialog = false },
            onAdd = {
                onAddExam(it)
                showAddExamDialog = false
            }
        )
    }

    if (showAddAssignmentDialog) {
        AddAssignmentDialog(
            subjects = subjects,
            onQuickAddSubject = onQuickAddSubject,
            onDismiss = { showAddAssignmentDialog = false },
            onAdd = {
                onAddAssignment(it)
                showAddAssignmentDialog = false
            }
        )
    }

    if (showAddRevisionDialog) {
        AddRevisionDialog(
            subjects = subjects,
            onQuickAddSubject = onQuickAddSubject,
            onDismiss = { showAddRevisionDialog = false },
            onAdd = {
                onAddRevisionItem(it)
                showAddRevisionDialog = false
            }
        )
    }
}

@Composable
fun TodayTab(
    items: List<TodayItem>,
    onToggleComplete: (Long, Boolean) -> Unit,
    onDeleteStudyPlan: (Long) -> Unit,
    onUpdateStudyPlan: (StudyPlanEntity) -> Unit,
    onDeleteRevision: (Long) -> Unit,
    onUpdateRevision: (RevisionItemEntity) -> Unit,
    onAddClick: () -> Unit
) {
    var planToEdit by remember { mutableStateOf<StudyPlanEntity?>(null) }
    var planToDelete by remember { mutableStateOf<StudyPlanEntity?>(null) }
    var revisionToEdit by remember { mutableStateOf<RevisionItemEntity?>(null) }
    var revisionToDelete by remember { mutableStateOf<RevisionItemEntity?>(null) }

    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No study blocks or revisions today", color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
                ) {
                    Text("+ Add Item")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                when (item) {
                    is TodayItem.StudyBlock -> {
                        val plan = item.plan
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
                                Checkbox(
                                    checked = plan.isCompleted,
                                    onCheckedChange = { onToggleComplete(plan.id, it) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = EmeraldAccent,
                                        uncheckedColor = TextMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${plan.startTime} - ${plan.endTime}",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = AmberAccent,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = plan.subjectName,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${plan.topicName}${if (plan.notes.isNotEmpty()) " • ${plan.notes}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(onClick = { planToEdit = plan }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { planToDelete = plan }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    is TodayItem.Revision -> {
                        val rev = item.item
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
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF222222))
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Loop,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CyanSoft)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "REVISION",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = CyanAccent
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = rev.subjectName,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = rev.topicName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(onClick = { revisionToEdit = rev }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { revisionToDelete = rev }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (planToEdit != null) {
        EditStudyPlanDialog(
            plan = planToEdit!!,
            onDismiss = { planToEdit = null },
            onSave = {
                onUpdateStudyPlan(it)
                planToEdit = null
            }
        )
    }

    if (planToDelete != null) {
        DeleteConfirmationDialog(
            title = "Delete Study Block?",
            message = "Are you sure you want to delete this study block for \"${planToDelete!!.subjectName}\"?",
            onConfirm = {
                onDeleteStudyPlan(planToDelete!!.id)
                planToDelete = null
            },
            onDismiss = { planToDelete = null }
        )
    }

    if (revisionToEdit != null) {
        EditRevisionDialog(
            item = revisionToEdit!!,
            onDismiss = { revisionToEdit = null },
            onSave = {
                onUpdateRevision(it)
                revisionToEdit = null
            }
        )
    }

    if (revisionToDelete != null) {
        DeleteConfirmationDialog(
            title = "Delete Spaced Review?",
            message = "Are you sure you want to delete this spaced review item for \"${revisionToDelete!!.subjectName}\"?",
            onConfirm = {
                onDeleteRevision(revisionToDelete!!.id)
                revisionToDelete = null
            },
            onDismiss = { revisionToDelete = null }
        )
    }
}

@Composable
fun ExamsTab(
    exams: List<ExamEntity>,
    onDelete: (Long) -> Unit,
    onUpdate: (ExamEntity) -> Unit,
    onAddClick: () -> Unit
) {
    var examToEdit by remember { mutableStateOf<ExamEntity?>(null) }
    var examToDelete by remember { mutableStateOf<ExamEntity?>(null) }

    if (exams.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = onAddClick, colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)) {
                Text("+ Add Target Exam")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(exams) { exam ->
                val now = System.currentTimeMillis()
                val diffMillis = exam.examTimestamp - now
                val daysLeft = (diffMillis / (1000 * 60 * 60 * 24)).coerceAtLeast(0)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CharcoalBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exam.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AmberSoft)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$daysLeft DAYS LEFT",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = AmberAccent
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { examToEdit = exam }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { examToDelete = exam }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Subjects: ${exam.subjectNames}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Preparation Progress", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text("${exam.preparationPercent}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AmberAccent)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (exam.preparationPercent / 100f).coerceIn(0f, 1f) },
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

    if (examToEdit != null) {
        EditExamDialog(
            exam = examToEdit!!,
            onDismiss = { examToEdit = null },
            onSave = {
                onUpdate(it)
                examToEdit = null
            }
        )
    }

    if (examToDelete != null) {
        DeleteConfirmationDialog(
            title = "Delete Target Exam?",
            message = "Are you sure you want to delete the exam \"${examToDelete!!.title}\"?",
            onConfirm = {
                onDelete(examToDelete!!.id)
                examToDelete = null
            },
            onDismiss = { examToDelete = null }
        )
    }
}

@Composable
fun AssignmentsTab(
    assignments: List<AssignmentEntity>,
    onDelete: (Long) -> Unit,
    onUpdate: (AssignmentEntity) -> Unit,
    onAddClick: () -> Unit
) {
    var taskToEdit by remember { mutableStateOf<AssignmentEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<AssignmentEntity?>(null) }

    if (assignments.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No active assignments or tasks", color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
                ) {
                    Text("+ Add Task")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(assignments) { task ->
                val diffMillis = task.dueTimestamp - System.currentTimeMillis()
                val dueDays = (diffMillis / (1000L * 3600 * 24)).toInt()

                val dueText: String
                val dueColor: Color
                if (dueDays < 0) {
                    dueText = "Overdue by ${-dueDays} days"
                    dueColor = RoseAccent
                } else if (dueDays == 0) {
                    dueText = "Due today"
                    dueColor = AmberAccent
                } else {
                    dueText = "Due in $dueDays days"
                    dueColor = TextSecondary
                }

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
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (task.priority == "High") RoseSoft else CyanSoft)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    val priorityText = task.priority.uppercase()
                                    Text(
                                        text = priorityText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (task.priority == "High") RoseAccent else CyanAccent
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(task.title, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${task.subjectName} • ",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = dueText,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = dueColor
                            )
                            Text(
                                text = " • Est: ${task.estimatedMinutes}m • ${task.progressPercent}% Done",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { taskToEdit = task }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { taskToDelete = task }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (taskToEdit != null) {
        EditAssignmentDialog(
            assignment = taskToEdit!!,
            onDismiss = { taskToEdit = null },
            onSave = {
                onUpdate(it)
                taskToEdit = null
            }
        )
    }

    if (taskToDelete != null) {
        DeleteConfirmationDialog(
            title = "Delete Assignment / Task?",
            message = "Are you sure you want to delete \"${taskToDelete!!.title}\"?",
            onConfirm = {
                onDelete(taskToDelete!!.id)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }
}

@Composable
fun SubjectPickerDropdown(
    subjects: List<SubjectEntity>,
    selectedSubjectName: String,
    onSubjectNameSelected: (String) -> Unit,
    onQuickAddSubject: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showNewSubjectDialog by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedSubjectName,
            onValueChange = onSubjectNameSelected,
            label = { Text("Subject") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Subject")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(CharcoalCard)
                .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
        ) {
            subjects.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.name, color = TextPrimary) },
                    onClick = {
                        onSubjectNameSelected(s.name)
                        expanded = false
                    }
                )
            }
            HorizontalDivider(color = CharcoalBorder)
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add new subject...", color = AmberAccent, fontWeight = FontWeight.Bold)
                    }
                },
                onClick = {
                    expanded = false
                    newSubjectName = ""
                    showNewSubjectDialog = true
                }
            )
        }
    }

    if (showNewSubjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewSubjectDialog = false },
            title = { Text("New Subject", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSubjectName,
                    onValueChange = { newSubjectName = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubjectName.isNotBlank()) {
                            onQuickAddSubject(newSubjectName.trim())
                            onSubjectNameSelected(newSubjectName.trim())
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
}

@Composable
fun AddStudyPlanDialog(
    subjects: List<SubjectEntity> = emptyList(),
    onQuickAddSubject: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onAdd: (StudyPlanEntity) -> Unit
) {
    var subject by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "English Literature") }
    var topic by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("14:00") }
    var end by remember { mutableStateOf("14:50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Add Study Block", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SubjectPickerDropdown(
                    subjects = subjects,
                    selectedSubjectName = subject,
                    onSubjectNameSelected = { subject = it },
                    onQuickAddSubject = onQuickAddSubject
                )
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic / Goal") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        StudyPlanEntity(
                            dayOfWeek = "TODAY",
                            startTime = start,
                            endTime = end,
                            subjectName = subject,
                            topicName = topic.ifBlank { "Study Session" }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
            ) {
                Text("ADD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
        },
        containerColor = CharcoalCard
    )
}

@Composable
fun AddExamDialog(
    subjects: List<SubjectEntity> = emptyList(),
    onQuickAddSubject: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onAdd: (ExamEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subjectsName by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "General") }
    var daysAhead by remember { mutableIntStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Add Target Exam", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Exam Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                SubjectPickerDropdown(
                    subjects = subjects,
                    selectedSubjectName = subjectsName,
                    onSubjectNameSelected = { subjectsName = it },
                    onQuickAddSubject = onQuickAddSubject
                )
                OutlinedTextField(
                    value = daysAhead.toString(),
                    onValueChange = { daysAhead = it.toIntOrNull() ?: 30 },
                    label = { Text("Days until exam") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        ExamEntity(
                            title = title.ifBlank { "Target Exam" },
                            subjectNames = subjectsName.ifBlank { "General" },
                            examTimestamp = System.currentTimeMillis() + (daysAhead * 24L * 3600 * 1000L),
                            preparationPercent = 20
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
            ) {
                Text("SAVE EXAM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
        },
        containerColor = CharcoalCard
    )
}

@Composable
fun AddAssignmentDialog(
    subjects: List<SubjectEntity> = emptyList(),
    onQuickAddSubject: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onAdd: (AssignmentEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "English Literature") }
    var daysUntil by remember { mutableIntStateOf(7) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Add Assignment / Task", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                SubjectPickerDropdown(
                    subjects = subjects,
                    selectedSubjectName = subject,
                    onSubjectNameSelected = { subject = it },
                    onQuickAddSubject = onQuickAddSubject
                )
                OutlinedTextField(
                    value = daysUntil.toString(),
                    onValueChange = { daysUntil = it.toIntOrNull() ?: 7 },
                    label = { Text("Due in (days)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        AssignmentEntity(
                            title = title.ifBlank { "Assignment" },
                            subjectName = subject,
                            topicName = "General",
                            dueTimestamp = System.currentTimeMillis() + (daysUntil * 24L * 3600 * 1000L),
                            priority = "High"
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
            ) {
                Text("ADD TASK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
        },
        containerColor = CharcoalCard
    )
}

@Composable
fun AddRevisionDialog(
    subjects: List<SubjectEntity> = emptyList(),
    onQuickAddSubject: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onAdd: (RevisionItemEntity) -> Unit
) {
    var subject by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "English Literature") }
    var topic by remember { mutableStateOf("") }
    var daysUntil by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Add Spaced Revision", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SubjectPickerDropdown(
                    subjects = subjects,
                    selectedSubjectName = subject,
                    onSubjectNameSelected = { subject = it },
                    onQuickAddSubject = onQuickAddSubject
                )
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic / Goal") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = daysUntil.toString(),
                    onValueChange = { daysUntil = it.toIntOrNull() ?: 1 },
                    label = { Text("Days until next review") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        RevisionItemEntity(
                            subjectName = subject,
                            topicName = topic.ifBlank { "Spaced Revision" },
                            lastStudiedTimestamp = System.currentTimeMillis(),
                            nextRevisionTimestamp = System.currentTimeMillis() + (daysUntil * 24L * 3600 * 1000L),
                            revisionCount = 0,
                            isCompleted = false
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
            ) {
                Text("ADD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
        },
        containerColor = CharcoalCard
    )
}

@Composable
fun EditStudyPlanDialog(
    plan: StudyPlanEntity,
    onDismiss: () -> Unit,
    onSave: (StudyPlanEntity) -> Unit
) {
    var subject by remember { mutableStateOf(plan.subjectName) }
    var topic by remember { mutableStateOf(plan.topicName) }
    var start by remember { mutableStateOf(plan.startTime) }
    var end by remember { mutableStateOf(plan.endTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Edit Study Block", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic / Goal") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        plan.copy(
                            startTime = start,
                            endTime = end,
                            subjectName = subject,
                            topicName = topic.ifBlank { "Study Session" }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
        },
        containerColor = CharcoalCard
    )
}

@Composable
fun EditExamDialog(
    exam: ExamEntity,
    onDismiss: () -> Unit,
    onSave: (ExamEntity) -> Unit
) {
    var title by remember { mutableStateOf(exam.title) }
    var subjects by remember { mutableStateOf(exam.subjectNames) }
    val initialDays = remember(exam.examTimestamp) {
        val diff = exam.examTimestamp - System.currentTimeMillis()
        (diff / (24L * 3600 * 1000L)).toInt().coerceAtLeast(0)
    }
    var daysAhead by remember { mutableIntStateOf(initialDays) }
    var prepPercent by remember { mutableIntStateOf(exam.preparationPercent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Edit Target Exam", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Exam Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = subjects,
                    onValueChange = { subjects = it },
                    label = { Text("Included Subjects") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = daysAhead.toString(),
                    onValueChange = { daysAhead = it.toIntOrNull() ?: 0 },
                    label = { Text("Days until exam") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                Spacer(Modifier.height(4.dp))
                Text("Preparation: $prepPercent%", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = prepPercent.toFloat(),
                    onValueChange = { prepPercent = it.toInt() },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = AmberAccent,
                        activeTrackColor = AmberAccent,
                        inactiveTrackColor = CharcoalElevated
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        exam.copy(
                            title = title.ifBlank { "Target Exam" },
                            subjectNames = subjects.ifBlank { "General" },
                            examTimestamp = System.currentTimeMillis() + (daysAhead * 24L * 3600 * 1000L),
                            preparationPercent = prepPercent
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
        },
        containerColor = CharcoalCard
    )
}

@Composable
fun EditAssignmentDialog(
    assignment: AssignmentEntity,
    onDismiss: () -> Unit,
    onSave: (AssignmentEntity) -> Unit
) {
    var title by remember { mutableStateOf(assignment.title) }
    var subject by remember { mutableStateOf(assignment.subjectName) }
    val initialDays = remember(assignment.dueTimestamp) {
        val diff = assignment.dueTimestamp - System.currentTimeMillis()
        (diff / (24L * 3600 * 1000L)).toInt().coerceAtLeast(0)
    }
    var daysUntil by remember { mutableIntStateOf(initialDays) }
    var priority by remember { mutableStateOf(assignment.priority) }
    var progressPercent by remember { mutableIntStateOf(assignment.progressPercent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Edit Assignment / Task", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = daysUntil.toString(),
                    onValueChange = { daysUntil = it.toIntOrNull() ?: 0 },
                    label = { Text("Due in (days)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                Text("Priority", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Medium", "High").forEach { p ->
                        val isSelected = priority == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AmberSoft else CharcoalElevated)
                                .clickable { priority = p }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p,
                                color = if (isSelected) AmberAccent else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Progress: $progressPercent%", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(2.dp))
                Slider(
                    value = progressPercent.toFloat(),
                    onValueChange = { progressPercent = it.toInt() },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = AmberAccent,
                        activeTrackColor = AmberAccent,
                        inactiveTrackColor = CharcoalElevated
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        assignment.copy(
                            title = title.ifBlank { "Assignment" },
                            subjectName = subject,
                            dueTimestamp = System.currentTimeMillis() + (daysUntil * 24L * 3600 * 1000L),
                            priority = priority,
                            progressPercent = progressPercent
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
        },
        containerColor = CharcoalCard
    )
}

@Composable
fun EditRevisionDialog(
    item: RevisionItemEntity,
    onDismiss: () -> Unit,
    onSave: (RevisionItemEntity) -> Unit
) {
    val initialDays = remember(item.nextRevisionTimestamp) {
        val diff = item.nextRevisionTimestamp - System.currentTimeMillis()
        (diff / (24L * 3600 * 1000L)).toInt().coerceAtLeast(0)
    }
    var daysUntil by remember { mutableStateOf(initialDays) }
    var isCompleted by remember { mutableStateOf(item.isCompleted) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Edit Spaced Review", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Subject: ${item.subjectName}", color = TextPrimary, fontWeight = FontWeight.Medium)
                Text("Topic: ${item.topicName}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = daysUntil.toString(),
                    onValueChange = { daysUntil = it.toIntOrNull() ?: 0 },
                    label = { Text("Days until next review") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCompleted = !isCompleted }
                ) {
                    Checkbox(
                        checked = isCompleted,
                        onCheckedChange = { isCompleted = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = EmeraldAccent,
                            uncheckedColor = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as completed", color = TextPrimary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        item.copy(
                            nextRevisionTimestamp = System.currentTimeMillis() + (daysUntil * 24L * 3600 * 1000L),
                            isCompleted = isCompleted
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color.Black)
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
        },
        containerColor = CharcoalCard
    )
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = TextSecondary) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
            ) {
                Text("DELETE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        },
        containerColor = CharcoalCard
    )
}
