package com.example.taskflow

import android.app.Application
import android.content.Context
import com.example.taskflow.model.TaskDatabase
import com.example.taskflow.model.TaskFlowRepository

class TaskFlowApplication : Application() {

    // Global repository instance
    lateinit var repository: TaskFlowRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize the Database singleton
        val database = TaskDatabase.getInstance(this)
        
        // Initialize SharedPreferences
        val sharedPreferences = getSharedPreferences("taskflow_prefs", Context.MODE_PRIVATE)
        
        // Initialize the Repository with DAOs and SharedPreferences
        repository = TaskFlowRepository(
            userDao = database.userDao(),
            taskDao = database.taskDao(),
            sharedPreferences = sharedPreferences
        )
    }
}
