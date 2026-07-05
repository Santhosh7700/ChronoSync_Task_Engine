package com.example.taskflow.utils

import com.example.taskflow.model.Task
import com.example.taskflow.model.User
import org.json.JSONArray
import org.json.JSONObject

object BackupHelper {

    fun backupToJson(user: User, tasks: List<Task>): String {
        val root = JSONObject()
        
        val userJson = JSONObject().apply {
            put("fullName", user.fullName)
            put("username", user.username)
            put("securityPin", user.securityPin)
        }
        root.put("user", userJson)
        
        val tasksArray = JSONArray()
        for (task in tasks) {
            val taskJson = JSONObject().apply {
                put("title", task.title)
                put("description", task.description)
                put("category", task.category)
                put("dueDate", task.dueDate)
                put("isCompleted", task.isCompleted)
                put("createdAt", task.createdAt)  // preserve original creation timestamp
            }
            tasksArray.put(taskJson)
        }
        root.put("tasks", tasksArray)
        
        return root.toString(4) // Pretty print with 4 spaces
    }

    fun restoreFromJson(jsonString: String): Pair<User, List<Task>>? {
        return try {
            val root = JSONObject(jsonString)
            val userJson = root.getJSONObject("user")
            
            val user = User(
                fullName = userJson.getString("fullName"),
                username = userJson.getString("username"),
                securityPin = userJson.getString("securityPin")
            )
            
            val tasksArray = root.getJSONArray("tasks")
            val tasks = mutableListOf<Task>()
            
            for (i in 0 until tasksArray.length()) {
                val taskJson = tasksArray.getJSONObject(i)
                tasks.add(
                    Task(
                        title       = taskJson.getString("title"),
                        description = taskJson.getString("description"),
                        category    = taskJson.getString("category"),
                        dueDate     = taskJson.getLong("dueDate"),
                        isCompleted = taskJson.getBoolean("isCompleted"),
                        // Use the exact stored timestamp; fall back to now() only for
                        // legacy backup files that pre-date this fix and lack the field.
                        createdAt   = taskJson.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            Pair(user, tasks)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
