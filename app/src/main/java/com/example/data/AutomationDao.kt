package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationRuleDao {
    @Query("SELECT * FROM automation_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<AutomationRule>>

    @Query("SELECT * FROM automation_rules WHERE isActive = 1")
    suspend fun getActiveRules(): List<AutomationRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutomationRule)

    @Update
    suspend fun updateRule(rule: AutomationRule)

    @Delete
    suspend fun deleteRule(rule: AutomationRule)

    @Query("DELETE FROM automation_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Int)
}

@Dao
interface LogEntryDao {
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntry)

    @Query("DELETE FROM log_entries")
    suspend fun clearLogs()
}
