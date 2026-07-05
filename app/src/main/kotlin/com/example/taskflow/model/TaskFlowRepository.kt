package com.example.taskflow.model

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

class TaskFlowRepository(
    private val userDao: UserDao,
    private val taskDao: TaskDao,
    private val sharedPreferences: SharedPreferences
) {
    // ── Task Operations ────────────────────────────────────────────────────────

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun getTaskCount(): Int = taskDao.getTaskCount()

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun insertAll(tasks: List<Task>) = taskDao.insertAll(tasks)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun toggleCompletion(id: Int, completed: Boolean) =
        taskDao.setCompleted(id, completed)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun clearAllTasks() = taskDao.clearAllTasks()

    // ── User Operations ────────────────────────────────────────────────────────
    
    suspend fun insertOrUpdateUser(user: User) = userDao.insertOrUpdateUser(user)
    
    suspend fun getUserProfile(): User? = userDao.getUserProfile()
    
    suspend fun queryUserByUsername(username: String): User? = userDao.queryUserByUsername(username)
    
    suspend fun clearUserProfile() = userDao.clearUserProfile()

    // ── Settings Operations ────────────────────────────────────────────────────

    fun isDarkMode(): Boolean {
        return sharedPreferences.getBoolean("dark_mode", false)
    }

    fun setDarkMode(isDark: Boolean) {
        sharedPreferences.edit().putBoolean("dark_mode", isDark).apply()
    }

    fun logoutSession() {
        sharedPreferences.edit().putBoolean("is_logged_in", false).apply()
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean("is_logged_in", isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean("is_logged_in", false)

    fun setLockEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("is_lock_enabled", enabled).apply()
    }

    fun isLockEnabled(): Boolean = sharedPreferences.getBoolean("is_lock_enabled", false)

    suspend fun clearAllUserData() {
        taskDao.clearAllTasks()
        userDao.clearUserProfile()
        logoutSession()
    }
}
