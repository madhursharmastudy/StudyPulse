package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AppLogEntity): Long

    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC LIMIT 5000")
    fun getRecentLogs(): Flow<List<AppLogEntity>>

    @Query("SELECT * FROM app_logs WHERE level = :level ORDER BY timestamp DESC LIMIT 5000")
    fun getLogsByLevel(level: String): Flow<List<AppLogEntity>>

    @Query("SELECT * FROM app_logs WHERE feature = :feature ORDER BY timestamp DESC LIMIT 5000")
    fun getLogsByFeature(feature: String): Flow<List<AppLogEntity>>

    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC LIMIT 5000")
    suspend fun getAllLogsList(): List<AppLogEntity>

    @Query("DELETE FROM app_logs")
    suspend fun clearLogs()

    @Query("SELECT COUNT(*) FROM app_logs")
    suspend fun getLogCount(): Int
}
