package com.example.data.repository

import com.example.core.logging.AppLogger
import com.example.core.logging.LogFeature
import com.example.core.timer.BreakType
import com.example.core.timer.TimerSnapshot
import com.example.data.db.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StudyWatchRepository(private val dao: StudyWatchDao) {

    val allSubjects: Flow<List<SubjectEntity>> = dao.getAllSubjects()
    val allTopics: Flow<List<TopicEntity>> = dao.getAllTopics()
    val allSessions: Flow<List<StudySessionEntity>> = dao.getAllStudySessions()
    val allPlans: Flow<List<StudyPlanEntity>> = dao.getAllStudyPlans()
    val allExams: Flow<List<ExamEntity>> = dao.getAllExams()
    val allAssignments: Flow<List<AssignmentEntity>> = dao.getAllAssignments()
    val allRevisionItems: Flow<List<RevisionItemEntity>> = dao.getAllRevisionItems()

    fun getTopicsForSubject(subjectId: Long): Flow<List<TopicEntity>> =
        dao.getTopicsForSubject(subjectId)

    fun observeDailyGoalForToday(): Flow<DailyGoalEntity?> {
        val todayStr = getTodayDateString()
        return dao.observeDailyGoalForDate(todayStr)
    }

    suspend fun getOrCreateTodayGoal(defaultTargetMinutes: Int = 360): DailyGoalEntity {
        val todayStr = getTodayDateString()
        val existing = dao.getDailyGoalForDate(todayStr)
        if (existing != null) return existing

        val newGoal = DailyGoalEntity(
            dateString = todayStr,
            targetMinutes = defaultTargetMinutes,
            achievedMinutes = 0,
            completed = false
        )
        val id = dao.insertOrUpdateDailyGoal(newGoal)
        return newGoal.copy(id = id)
    }

    suspend fun recordCompletedSession(snapshot: TimerSnapshot): Long {
        val plannedSecs = snapshot.plannedDurationMillis / 1000
        val productiveSecs = snapshot.elapsedProductiveMillis / 1000
        val eyeSecs = snapshot.elapsedEyeRestMillis / 1000
        val waterSecs = snapshot.elapsedWaterBreakMillis / 1000
        val breakSecs = snapshot.elapsedOtherBreakMillis / 1000
        val endTimestamp = System.currentTimeMillis()
        val startTimestamp = if (snapshot.startTimestampMillis > 0) snapshot.startTimestampMillis else (endTimestamp - snapshot.totalSessionElapsedMillis)

        val sessionEntity = StudySessionEntity(
            sessionUid = snapshot.sessionUid,
            subjectId = snapshot.subjectId,
            topicId = snapshot.topicId,
            subjectName = snapshot.subjectName,
            topicName = snapshot.topicName,
            timerType = snapshot.timerType.name,
            plannedSeconds = plannedSecs,
            productiveStudySeconds = productiveSecs,
            breakSeconds = breakSecs,
            eyeRestSeconds = eyeSecs,
            waterBreakSeconds = waterSecs,
            interruptionCount = snapshot.interruptionCount,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            status = snapshot.state.name,
            notes = snapshot.notes
        )

        val id = dao.insertStudySession(sessionEntity)

        // Increment subject and topic study totals
        if (snapshot.subjectId > 0 && productiveSecs > 0) {
            dao.addSubjectStudyTime(snapshot.subjectId, productiveSecs)
        }
        if (snapshot.topicId > 0 && productiveSecs > 0) {
            dao.addTopicStudyTime(snapshot.topicId, productiveSecs)
        }

        // Update Today's Daily Goal
        if (productiveSecs > 0) {
            val todayStr = getTodayDateString()
            val currentGoal = dao.getDailyGoalForDate(todayStr) ?: DailyGoalEntity(dateString = todayStr)
            val updatedAchieved = currentGoal.achievedMinutes + (productiveSecs / 60).toInt()
            val isCompleted = updatedAchieved >= currentGoal.targetMinutes
            dao.insertOrUpdateDailyGoal(
                currentGoal.copy(
                    achievedMinutes = updatedAchieved,
                    completed = isCompleted
                )
            )
            AppLogger.i(
                LogFeature.Database,
                "Updated daily goal for $todayStr",
                "Achieved: ${updatedAchieved}m / ${currentGoal.targetMinutes}m (Completed: $isCompleted)"
            )
        }

        // Record laps if any
        snapshot.laps.forEach { lap ->
            dao.insertLapRecord(
                LapRecordEntity(
                    sessionUid = snapshot.sessionUid,
                    lapNumber = lap.lapNumber,
                    lapDurationMillis = lap.lapDurationMillis,
                    totalDurationMillis = lap.totalDurationMillis
                )
            )
        }

        AppLogger.i(
            LogFeature.Database,
            "Study session saved to database (ID #$id)",
            "Productive: ${productiveSecs}s, Breaks: ${breakSecs + eyeSecs + waterSecs}s",
            snapshot.state.name,
            snapshot.sessionUid,
            snapshot.timerId,
            snapshot.subjectName,
            snapshot.topicName
        )
        return id
    }

    suspend fun recordEyeRestEvent(sessionUid: String, durationSecs: Long, completed: Boolean) {
        val now = System.currentTimeMillis()
        dao.insertEyeRestEvent(
            EyeRestEventEntity(
                sessionUid = sessionUid,
                startTimestamp = now - (durationSecs * 1000),
                endTimestamp = now,
                durationSeconds = durationSecs,
                completed = completed
            )
        )
        AppLogger.i(LogFeature.Database, "Eye rest event recorded in database ($durationSecs s)")
    }

    suspend fun recordWaterEvent(sessionUid: String, actionTaken: String, durationSecs: Long) {
        dao.insertWaterEvent(
            WaterEventEntity(
                sessionUid = sessionUid,
                timestamp = System.currentTimeMillis(),
                actionTaken = actionTaken,
                durationSeconds = durationSecs
            )
        )
        AppLogger.i(LogFeature.Database, "Hydration event recorded in database: $actionTaken")
    }

    suspend fun recordBreakSession(sessionUid: String, breakType: BreakType, durationSecs: Long) {
        val now = System.currentTimeMillis()
        dao.insertBreakSession(
            BreakSessionEntity(
                sessionUid = sessionUid,
                breakType = breakType.name,
                startTimestamp = now - (durationSecs * 1000),
                endTimestamp = now,
                durationSeconds = durationSecs
            )
        )
    }

    // CRUD for Subjects & Topics
    suspend fun insertSubject(name: String, iconName: String, colorHex: String, dailyGoalMinutes: Int): Long {
        return dao.insertSubject(SubjectEntity(name = name, iconName = iconName, colorHex = colorHex, dailyGoalMinutes = dailyGoalMinutes))
    }

    suspend fun updateSubject(subject: SubjectEntity) {
        dao.updateSubject(subject)
        dao.updateSessionsSubjectName(subject.id, subject.name)
    }

    suspend fun deleteSubject(id: Long) {
        dao.deleteSubject(id)
        dao.deleteTopicsForSubject(id)
        // Historical study sessions are safely preserved!
    }

    suspend fun insertTopic(subjectId: Long, name: String): Long {
        return dao.insertTopic(TopicEntity(subjectId = subjectId, name = name))
    }

    suspend fun updateTopic(topic: TopicEntity) {
        dao.updateTopic(topic)
        dao.updateSessionsTopicName(topic.id, topic.name)
    }

    suspend fun deleteTopic(id: Long) {
        dao.deleteTopic(id)
        // Historical study sessions are safely preserved!
    }

    // CRUD for Plans, Exams, Assignments, Revision
    suspend fun insertStudyPlan(plan: StudyPlanEntity) = dao.insertStudyPlan(plan)
    suspend fun setStudyPlanCompleted(id: Long, completed: Boolean) = dao.setStudyPlanCompleted(id, completed)
    suspend fun deleteStudyPlan(id: Long) = dao.deleteStudyPlan(id)

    suspend fun insertExam(exam: ExamEntity) = dao.insertExam(exam)
    suspend fun updateExam(exam: ExamEntity) = dao.updateExam(exam)
    suspend fun deleteExam(id: Long) = dao.deleteExam(id)

    suspend fun insertAssignment(assignment: AssignmentEntity) = dao.insertAssignment(assignment)
    suspend fun updateAssignment(assignment: AssignmentEntity) = dao.updateAssignment(assignment)
    suspend fun deleteAssignment(id: Long) = dao.deleteAssignment(id)

    suspend fun insertRevisionItem(item: RevisionItemEntity) = dao.insertRevisionItem(item)
    suspend fun updateRevisionItem(item: RevisionItemEntity) = dao.updateRevisionItem(item)
    suspend fun deleteRevisionItem(id: Long) = dao.deleteRevisionItem(id)

    suspend fun deleteStudySession(id: Long) = dao.deleteSession(id)

    suspend fun updateDailyGoalTarget(targetMinutes: Int) {
        val todayStr = getTodayDateString()
        val existing = dao.getDailyGoalForDate(todayStr) ?: DailyGoalEntity(dateString = todayStr)
        dao.insertOrUpdateDailyGoal(existing.copy(targetMinutes = targetMinutes, completed = existing.achievedMinutes >= targetMinutes))
    }

    // EXPORT TO JSON
    suspend fun exportAllDataToJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportTimestamp", System.currentTimeMillis())
        root.put("appName", "StudyPulse")

        val subjectsArray = JSONArray()
        dao.getAllSubjectsList().forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("iconName", s.iconName)
            obj.put("colorHex", s.colorHex)
            obj.put("dailyGoalMinutes", s.dailyGoalMinutes)
            obj.put("totalStudySeconds", s.totalStudySeconds)
            subjectsArray.put(obj)
        }
        root.put("subjects", subjectsArray)

        val topicsArray = JSONArray()
        dao.getAllTopicsList().forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("subjectId", t.subjectId)
            obj.put("name", t.name)
            obj.put("totalStudySeconds", t.totalStudySeconds)
            topicsArray.put(obj)
        }
        root.put("topics", topicsArray)

        val sessionsArray = JSONArray()
        dao.getAllSessionsList().forEach { s ->
            val obj = JSONObject()
            obj.put("sessionUid", s.sessionUid)
            obj.put("subjectName", s.subjectName)
            obj.put("topicName", s.topicName)
            obj.put("timerType", s.timerType)
            obj.put("plannedSeconds", s.plannedSeconds)
            obj.put("productiveStudySeconds", s.productiveStudySeconds)
            obj.put("breakSeconds", s.breakSeconds)
            obj.put("eyeRestSeconds", s.eyeRestSeconds)
            obj.put("waterBreakSeconds", s.waterBreakSeconds)
            obj.put("interruptionCount", s.interruptionCount)
            obj.put("startTimestamp", s.startTimestamp)
            obj.put("endTimestamp", s.endTimestamp)
            obj.put("status", s.status)
            obj.put("notes", s.notes)
            sessionsArray.put(obj)
        }
        root.put("studySessions", sessionsArray)

        val goalsArray = JSONArray()
        dao.getAllDailyGoalsList().forEach { g ->
            val obj = JSONObject()
            obj.put("dateString", g.dateString)
            obj.put("targetMinutes", g.targetMinutes)
            obj.put("achievedMinutes", g.achievedMinutes)
            obj.put("completed", g.completed)
            goalsArray.put(obj)
        }
        root.put("dailyGoals", goalsArray)

        return root.toString(2)
    }

    // EXPORT TO CSV
    suspend fun exportSessionsToCsv(): String {
        val sb = StringBuilder()
        sb.append("SessionUID,Subject,Topic,Type,PlannedMinutes,ProductiveMinutes,EyeRestSeconds,WaterBreakSeconds,Interruptions,Date,Status\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        dao.getAllSessionsList().forEach { s ->
            val dateFormatted = sdf.format(Date(s.startTimestamp))
            sb.append("\"${s.sessionUid}\",")
            sb.append("\"${s.subjectName}\",")
            sb.append("\"${s.topicName}\",")
            sb.append("\"${s.timerType}\",")
            sb.append("${s.plannedSeconds / 60},")
            sb.append("${s.productiveStudySeconds / 60},")
            sb.append("${s.eyeRestSeconds},")
            sb.append("${s.waterBreakSeconds},")
            sb.append("${s.interruptionCount},")
            sb.append("\"$dateFormatted\",")
            sb.append("\"${s.status}\"\n")
        }
        return sb.toString()
    }

    // EXPORT TO TXT
    suspend fun exportDataToTxt(): String {
        val sb = StringBuilder()
        sb.append("====================================================\n")
        sb.append("           STUDYPULSE STUDY REPORT & EXPORT        \n")
        sb.append("====================================================\n\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sb.append("Generated at: ${sdf.format(Date())}\n\n")

        sb.append("--- SUBJECTS & ACCUMULATED STUDY TIME ---\n")
        dao.getAllSubjectsList().forEach { s ->
            val hours = s.totalStudySeconds / 3600
            val minutes = (s.totalStudySeconds % 3600) / 60
            sb.append("• ${s.name}: ${hours}h ${minutes}m (Daily Goal: ${s.dailyGoalMinutes}m)\n")
        }

        sb.append("\n--- COMPLETED STUDY SESSIONS ---\n")
        dao.getAllSessionsList().forEach { s ->
            val dateFormatted = sdf.format(Date(s.startTimestamp))
            val productiveMins = s.productiveStudySeconds / 60
            sb.append("• [$dateFormatted] ${s.subjectName} - ${s.topicName}: ${productiveMins} mins (Eye: ${s.eyeRestSeconds}s, Water: ${s.waterBreakSeconds}s, Status: ${s.status})\n")
        }

        return sb.toString()
    }

    // RESTORE FROM JSON
    suspend fun restoreDataFromJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            if (!root.has("subjects") && !root.has("studySessions")) {
                AppLogger.e(LogFeature.BackupRestore, "Restore validation failed: JSON missing required fields")
                return false
            }

            if (root.has("subjects")) {
                val subArray = root.getJSONArray("subjects")
                for (i in 0 until subArray.length()) {
                    val obj = subArray.getJSONObject(i)
                    val s = SubjectEntity(
                        name = obj.optString("name", "Unknown"),
                        iconName = obj.optString("iconName", "book"),
                        colorHex = obj.optString("colorHex", "#E5A93C"),
                        dailyGoalMinutes = obj.optInt("dailyGoalMinutes", 60),
                        totalStudySeconds = obj.optLong("totalStudySeconds", 0L)
                    )
                    dao.insertSubject(s)
                }
            }

            if (root.has("studySessions")) {
                val sessArray = root.getJSONArray("studySessions")
                for (i in 0 until sessArray.length()) {
                    val obj = sessArray.getJSONObject(i)
                    val s = StudySessionEntity(
                        sessionUid = obj.optString("sessionUid", "RESTORED-${System.currentTimeMillis()}"),
                        subjectId = 0,
                        topicId = 0,
                        subjectName = obj.optString("subjectName", "Subject"),
                        topicName = obj.optString("topicName", "Topic"),
                        timerType = obj.optString("timerType", "STUDY_SESSION"),
                        plannedSeconds = obj.optLong("plannedSeconds", 3000L),
                        productiveStudySeconds = obj.optLong("productiveStudySeconds", 0L),
                        breakSeconds = obj.optLong("breakSeconds", 0L),
                        eyeRestSeconds = obj.optLong("eyeRestSeconds", 0L),
                        waterBreakSeconds = obj.optLong("waterBreakSeconds", 0L),
                        interruptionCount = obj.optInt("interruptionCount", 0),
                        startTimestamp = obj.optLong("startTimestamp", System.currentTimeMillis()),
                        endTimestamp = obj.optLong("endTimestamp", System.currentTimeMillis()),
                        status = obj.optString("status", "COMPLETED"),
                        notes = obj.optString("notes", "")
                    )
                    dao.insertStudySession(s)
                }
            }

            AppLogger.i(LogFeature.BackupRestore, "Restore completed successfully!")
            true
        } catch (t: Throwable) {
            AppLogger.e(LogFeature.BackupRestore, "Restore failed with error", null, null, null, null, t)
            false
        }
    }

    companion object {
        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}
