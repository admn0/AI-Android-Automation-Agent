package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val actionName: String,
    val status: String, // SUCCESS, FAILED, INFO
    val message: String,
    val modelUsed: String? = null
)
