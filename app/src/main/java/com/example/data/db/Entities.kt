package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String = "book",
    val colorHex: String = "#E5A93C",
    val dailyGoalMinutes: Int = 60,
    val totalStudySeconds: Long = 0L
)

@Entity(
    tableName = "topics",
    indices = [Index(value = ["subjectId"])]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val name: String,
    val totalStudySeconds: Long = 0L
)

@Entity(
    tableName = "study_sessions",
    indices = [Index(value = ["sessionUid"]), Index(value = ["startTimestamp"])]
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionUid: String,
    val subjectId: Long,
    val topicId: Long,
    val subjectName: String,
    val topicName: String,
    val timerType: String,
    val plannedSeconds: Long,
    val productiveStudySeconds: Long,
    val breakSeconds: Long,
    val eyeRestSeconds: Long,
    val waterBreakSeconds: Long,
    val interruptionCount: Int,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val status: String,
    val notes: String = ""
)

@Entity(tableName = "break_sessions")
data class BreakSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionUid: String,
    val breakType: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationSeconds: Long
)

@Entity(tableName = "eye_rest_events")
data class EyeRestEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionUid: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationSeconds: Long,
    val completed: Boolean = true
)

@Entity(tableName = "water_events")
data class WaterEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionUid: String,
    val timestamp: Long,
    val actionTaken: String, // DRANK_WATER, REMIND_LATER, SKIPPED
    val durationSeconds: Long
)

@Entity(
    tableName = "daily_goals",
    indices = [Index(value = ["dateString"], unique = true)]
)
data class DailyGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // "yyyy-MM-dd"
    val targetMinutes: Int = 360, // 6 hours default
    val achievedMinutes: Int = 0,
    val completed: Boolean = false
)

@Entity(tableName = "study_plans")
data class StudyPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: String, // "MONDAY", "TUESDAY", etc. or "TODAY"
    val startTime: String, // "10:00"
    val endTime: String,   // "10:50"
    val subjectName: String,
    val topicName: String,
    val notes: String = "",
    val isCompleted: Boolean = false
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val examTimestamp: Long,
    val subjectNames: String,
    val preparationPercent: Int = 0,
    val notes: String = ""
)

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectName: String,
    val topicName: String,
    val dueTimestamp: Long,
    val priority: String = "Medium", // High, Medium, Low
    val estimatedMinutes: Int = 60,
    val progressPercent: Int = 0,
    val notes: String = ""
)

@Entity(tableName = "revision_items")
data class RevisionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectName: String,
    val topicName: String,
    val lastStudiedTimestamp: Long,
    val nextRevisionTimestamp: Long,
    val revisionCount: Int = 0,
    val isCompleted: Boolean = false,
    val notes: String = ""
)

@Entity(
    tableName = "app_logs",
    indices = [Index(value = ["timestamp"]), Index(value = ["level"]), Index(value = ["feature"])]
)
data class AppLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val level: String,
    val feature: String,
    val event: String,
    val timerState: String? = null,
    val sessionUid: String? = null,
    val subjectName: String? = null,
    val topicName: String? = null,
    val details: String? = null,
    val exceptionDetails: String? = null
)

@Entity(tableName = "lap_records")
data class LapRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionUid: String,
    val lapNumber: Int,
    val lapDurationMillis: Long,
    val totalDurationMillis: Long
)
