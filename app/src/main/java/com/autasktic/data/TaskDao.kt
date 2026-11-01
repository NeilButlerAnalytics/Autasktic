package com.autasktic.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :date AND isCompleted = 0 ORDER BY time IS NULL, time, createdAt")
    fun observeActiveTasks(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date = :date AND isCompleted = 1 ORDER BY completedAt DESC")
    fun observeCompletedTasks(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getById(taskId: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND time IS NOT NULL")
    suspend fun getActiveTimedTasks(): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query(
        "UPDATE tasks SET isCompleted = 1, completedAt = :completedAt WHERE id = :taskId",
    )
    suspend fun complete(taskId: Long, completedAt: String)

    @Query("UPDATE tasks SET isCompleted = 0, completedAt = NULL WHERE id = :taskId")
    suspend fun reopen(taskId: Long)

    @Query("UPDATE tasks SET date = :date WHERE id = :taskId AND isCompleted = 0")
    suspend fun moveForward(taskId: Long, date: String)
}
