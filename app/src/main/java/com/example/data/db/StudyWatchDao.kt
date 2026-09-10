package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyWatchDao {

    // ================= SUBJECTS =================
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: Long)

    @Query("UPDATE study_sessions SET subjectName = :newName WHERE subjectId = :id")
    suspend fun updateSessionsSubjectName(id: Long, newName: String)

    @Query("UPDATE subjects SET totalStudySeconds = totalStudySeconds + :addedSeconds WHERE id = :id")
    suspend fun addSubjectStudyTime(id: Long, addedSeconds: Long)

    // ================= TOPICS =================
    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY name ASC")
    fun getTopicsForSubject(subjectId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics ORDER BY name ASC")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopic(id: Long)

    @Query("DELETE FROM topics WHERE subjectId = :subjectId")
    suspend fun deleteTopicsForSubject(subjectId: Long)

    @Query("UPDATE study_sessions SET topicName = :newName WHERE topicId = :id")
    suspend fun updateSessionsTopicName(id: Long, newName: String)

    @Query("UPDATE topics SET totalStudySeconds = totalStudySeconds + :addedSeconds WHERE id = :id")
    suspend fun addTopicStudyTime(id: Long, addedSeconds: Long)

    // ================= STUDY SESSIONS =================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity): Long

    @Query("SELECT * FROM study_sessions ORDER BY startTimestamp DESC")
    fun getAllStudySessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE startTimestamp >= :fromTimestamp AND startTimestamp <= :toTimestamp ORDER BY startTimestamp DESC")
    fun getStudySessionsBetween(fromTimestamp: Long, toTimestamp: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE subjectName = :subjectName ORDER BY startTimestamp DESC")
    fun getStudySessionsForSubject(subjectName: String): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE sessionUid = :sessionUid LIMIT 1")
    suspend fun getSessionByUid(sessionUid: String): StudySessionEntity?

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    // ================= BREAKS, EYE CARE, HYDRATION =================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreakSession(breakSession: BreakSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEyeRestEvent(event: EyeRestEventEntity): Long

    @Query("SELECT COUNT(*) FROM eye_rest_events WHERE startTimestamp >= :fromTimestamp")
    suspend fun getEyeRestCountSince(fromTimestamp: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterEvent(event: WaterEventEntity): Long

    @Query("SELECT COUNT(*) FROM water_events WHERE timestamp >= :fromTimestamp AND actionTaken = 'DRANK_WATER'")
    suspend fun getWaterIntakeCountSince(fromTimestamp: Long): Int

    // ================= DAILY GOALS =================
    @Query("SELECT * FROM daily_goals WHERE dateString = :dateString LIMIT 1")
    suspend fun getDailyGoalForDate(dateString: String): DailyGoalEntity?

    @Query("SELECT * FROM daily_goals WHERE dateString = :dateString LIMIT 1")
    fun observeDailyGoalForDate(dateString: String): Flow<DailyGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyGoal(dailyGoal: DailyGoalEntity): Long

    @Query("SELECT * FROM daily_goals ORDER BY dateString DESC LIMIT 30")
    fun getRecentDailyGoals(): Flow<List<DailyGoalEntity>>

    // ================= STUDY PLANS =================
    @Query("SELECT * FROM study_plans ORDER BY startTime ASC")
    fun getAllStudyPlans(): Flow<List<StudyPlanEntity>>

    @Query("SELECT * FROM study_plans WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getStudyPlansForDay(dayOfWeek: String): Flow<List<StudyPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyPlan(plan: StudyPlanEntity): Long

    @Query("UPDATE study_plans SET isCompleted = :completed WHERE id = :id")
    suspend fun setStudyPlanCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM study_plans WHERE id = :id")
    suspend fun deleteStudyPlan(id: Long)

    @Update
    suspend fun updateStudyPlan(plan: StudyPlanEntity)

    // ================= EXAMS =================
    @Query("SELECT * FROM exams ORDER BY examTimestamp ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExam(id: Long)

    // ================= ASSIGNMENTS =================
    @Query("SELECT * FROM assignments ORDER BY dueTimestamp ASC")
    fun getAllAssignments(): Flow<List<AssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: AssignmentEntity): Long

    @Update
    suspend fun updateAssignment(assignment: AssignmentEntity)

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteAssignment(id: Long)

    // ================= REVISION ITEMS =================
    @Query("SELECT * FROM revision_items ORDER BY nextRevisionTimestamp ASC")
    fun getAllRevisionItems(): Flow<List<RevisionItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevisionItem(item: RevisionItemEntity): Long

    @Update
    suspend fun updateRevisionItem(item: RevisionItemEntity)

    @Query("DELETE FROM revision_items WHERE id = :id")
    suspend fun deleteRevisionItem(id: Long)

    // ================= LAPS =================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLapRecord(lap: LapRecordEntity): Long

    @Query("SELECT * FROM lap_records WHERE sessionUid = :sessionUid ORDER BY lapNumber ASC")
    suspend fun getLapsForSession(sessionUid: String): List<LapRecordEntity>

    // ================= BACKUP / EXPORT ALL DATA =================
    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjectsList(): List<SubjectEntity>

    @Query("SELECT * FROM topics")
    suspend fun getAllTopicsList(): List<TopicEntity>

    @Query("SELECT * FROM study_sessions")
    suspend fun getAllSessionsList(): List<StudySessionEntity>

    @Query("SELECT * FROM daily_goals")
    suspend fun getAllDailyGoalsList(): List<DailyGoalEntity>

    @Query("SELECT * FROM study_plans")
    suspend fun getAllPlansList(): List<StudyPlanEntity>

    @Query("SELECT * FROM exams")
    suspend fun getAllExamsList(): List<ExamEntity>

    @Query("SELECT * FROM assignments")
    suspend fun getAllAssignmentsList(): List<AssignmentEntity>

    @Query("SELECT * FROM revision_items")
    suspend fun getAllRevisionList(): List<RevisionItemEntity>

    @Query("DELETE FROM study_sessions")
    suspend fun clearAllStudySessions()

    @Query("DELETE FROM subjects")
    suspend fun clearAllSubjects()

    @Query("DELETE FROM topics")
    suspend fun clearAllTopics()
}
