package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {

    @Query("SELECT * FROM workflows ORDER BY createdAt DESC")
    fun getAllWorkflows(): Flow<List<Workflow>>

    @Query("SELECT * FROM workflows WHERE isActive = 1")
    suspend fun getActiveWorkflowsSync(): List<Workflow>

    @Query("SELECT * FROM workflows WHERE id = :workflowId LIMIT 1")
    suspend fun getWorkflowById(workflowId: Int): Workflow?

    @Query("SELECT * FROM workflow_nodes WHERE workflowId = :workflowId")
    fun getNodesForWorkflow(workflowId: Int): Flow<List<WorkflowNode>>

    @Query("SELECT * FROM workflow_nodes WHERE workflowId = :workflowId")
    suspend fun getNodesForWorkflowSync(workflowId: Int): List<WorkflowNode>

    @Query("SELECT * FROM workflow_connections WHERE workflowId = :workflowId")
    fun getConnectionsForWorkflow(workflowId: Int): Flow<List<WorkflowConnection>>

    @Query("SELECT * FROM workflow_connections WHERE workflowId = :workflowId")
    suspend fun getConnectionsForWorkflowSync(workflowId: Int): List<WorkflowConnection>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: Workflow): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: WorkflowNode): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: WorkflowConnection): Long

    @Update
    suspend fun updateWorkflow(workflow: Workflow)

    @Query("UPDATE workflow_nodes SET x = :x, y = :y WHERE id = :nodeId")
    suspend fun updateNodePosition(nodeId: Int, x: Float, y: Float)

    @Delete
    suspend fun deleteWorkflow(workflow: Workflow)

    @Query("DELETE FROM workflow_nodes WHERE workflowId = :workflowId")
    suspend fun deleteNodesForWorkflow(workflowId: Int)

    @Query("DELETE FROM workflow_connections WHERE workflowId = :workflowId")
    suspend fun deleteConnectionsForWorkflow(workflowId: Int)

    @Query("DELETE FROM workflow_connections WHERE fromNodeId = :nodeId OR toNodeId = :nodeId")
    suspend fun deleteConnectionsForNode(nodeId: Int)

    @Query("DELETE FROM workflow_nodes WHERE id = :nodeId")
    suspend fun deleteNodeById(nodeId: Int)
}
