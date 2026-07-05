package com.example.taskflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.taskflow.model.TaskFlowRepository

/**
 * Single factory that constructs any ViewModel in the app by injecting the
 * shared [TaskFlowRepository]. Add new ViewModel classes here as the app grows.
 */
class ViewModelFactory(
    private val repository: TaskFlowRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(TaskViewModel::class.java) ->
            TaskViewModel(repository) as T

        modelClass.isAssignableFrom(NotificationViewModel::class.java) ->
            NotificationViewModel(repository) as T

        modelClass.isAssignableFrom(AnalyticsViewModel::class.java) ->
            AnalyticsViewModel(repository) as T

        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(repository) as T

        modelClass.isAssignableFrom(AuthViewModel::class.java) ->
            AuthViewModel(repository) as T

        else -> throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
