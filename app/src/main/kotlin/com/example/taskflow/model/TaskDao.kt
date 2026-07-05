package com.example.taskflow.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM task_table ORDER BY created_at DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("DELETE FROM task_table")
    suspend fun clearAllTasks()

    // Internal helper for demo seeding
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)

    // Internal helper for demo seeding check
    @Query("SELECT COUNT(*) FROM task_table")
    suspend fun getTaskCount(): Int

    // Quick toggle helper
    @Query("UPDATE task_table SET is_completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: Int, completed: Boolean)
}
