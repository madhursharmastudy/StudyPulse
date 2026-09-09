package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SubjectEntity::class,
        TopicEntity::class,
        StudySessionEntity::class,
        BreakSessionEntity::class,
        EyeRestEventEntity::class,
        WaterEventEntity::class,
        DailyGoalEntity::class,
        StudyPlanEntity::class,
        ExamEntity::class,
        AssignmentEntity::class,
        RevisionItemEntity::class,
        AppLogEntity::class,
        LapRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studyWatchDao(): StudyWatchDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_watch_database.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default sample subjects and topics
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    seedDefaultData(database.studyWatchDao())
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDefaultData(dao: StudyWatchDao) {
            val litId = dao.insertSubject(
                SubjectEntity(
                    name = "English Literature",
                    iconName = "menu_book",
                    colorHex = "#E5A93C",
                    dailyGoalMinutes = 60
                )
            )
            dao.insertTopic(TopicEntity(subjectId = litId, name = "Romantic Poetry"))
            dao.insertTopic(TopicEntity(subjectId = litId, name = "Victorian Literature"))
            dao.insertTopic(TopicEntity(subjectId = litId, name = "Shakespeare"))
            dao.insertTopic(TopicEntity(subjectId = litId, name = "Literary Criticism"))

            val chemId = dao.insertSubject(
                SubjectEntity(
                    name = "Chemistry",
                    iconName = "science",
                    colorHex = "#38BDF8",
                    dailyGoalMinutes = 60
                )
            )
            dao.insertTopic(TopicEntity(subjectId = chemId, name = "Organic Synthesis"))
            dao.insertTopic(TopicEntity(subjectId = chemId, name = "Thermodynamics"))
            dao.insertTopic(TopicEntity(subjectId = chemId, name = "Chemical Bonding"))

            val mathId = dao.insertSubject(
                SubjectEntity(
                    name = "Mathematics",
                    iconName = "calculate",
                    colorHex = "#4ADE80",
                    dailyGoalMinutes = 90
                )
            )
            dao.insertTopic(TopicEntity(subjectId = mathId, name = "Calculus & Integrals"))
            dao.insertTopic(TopicEntity(subjectId = mathId, name = "Linear Algebra"))

            val csId = dao.insertSubject(
                SubjectEntity(
                    name = "Computer Science",
                    iconName = "code",
                    colorHex = "#A855F7",
                    dailyGoalMinutes = 90
                )
            )
            dao.insertTopic(TopicEntity(subjectId = csId, name = "Algorithms & Data Structures"))
            dao.insertTopic(TopicEntity(subjectId = csId, name = "Database Systems"))

            // Sample study plans
            dao.insertStudyPlan(
                StudyPlanEntity(
                    dayOfWeek = "TODAY",
                    startTime = "10:00",
                    endTime = "10:50",
                    subjectName = "English Literature",
                    topicName = "Romantic Poetry",
                    notes = "Read Shelley & Keats"
                )
            )
            dao.insertStudyPlan(
                StudyPlanEntity(
                    dayOfWeek = "TODAY",
                    startTime = "11:00",
                    endTime = "11:50",
                    subjectName = "Chemistry",
                    topicName = "Organic Synthesis",
                    notes = "Mechanisms review"
                )
            )

            // Sample Exam
            dao.insertExam(
                ExamEntity(
                    title = "National Eligibility Test (NET)",
                    examTimestamp = System.currentTimeMillis() + (47L * 24 * 3600 * 1000L),
                    subjectNames = "English Literature, Chemistry",
                    preparationPercent = 78,
                    notes = "Paper 1 & Paper 2 Comprehensive"
                )
            )

            // Sample Assignment
            dao.insertAssignment(
                AssignmentEntity(
                    title = "Critical Analysis of Hamlet",
                    subjectName = "English Literature",
                    topicName = "Shakespeare",
                    dueTimestamp = System.currentTimeMillis() + (5L * 24 * 3600 * 1000L),
                    priority = "High",
                    estimatedMinutes = 180,
                    progressPercent = 40,
                    notes = "Focus on soliloquies"
                )
            )

            // Sample Revision Item
            dao.insertRevisionItem(
                RevisionItemEntity(
                    subjectName = "Chemistry",
                    topicName = "Thermodynamics",
                    lastStudiedTimestamp = System.currentTimeMillis() - (3L * 24 * 3600 * 1000L),
                    nextRevisionTimestamp = System.currentTimeMillis() + (2L * 24 * 3600 * 1000L),
                    revisionCount = 1,
                    notes = "Spaced repetition review 2"
                )
            )
        }
    }
}
