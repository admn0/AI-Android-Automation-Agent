package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workflows")
data class Workflow(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workflow_nodes")
data class WorkflowNode(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workflowId: Int,
    val type: String, // "TRIGGER_TIME", "TRIGGER_NOTIFICATION", "AI_AGENT", "DELAY", "HTTP_REQUEST", "ACTION_TTS", "ACTION_OPEN_APP", "ACTION_SMS"
    val label: String,
    val x: Float,
    val y: Float,
    val configValue: String // Configuration string (e.g. time, package name, text, URL)
)

@Entity(tableName = "workflow_connections")
data class WorkflowConnection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workflowId: Int,
    val fromNodeId: Int,
    val toNodeId: Int
)
