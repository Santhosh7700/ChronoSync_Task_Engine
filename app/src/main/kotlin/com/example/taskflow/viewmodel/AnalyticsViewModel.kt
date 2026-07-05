package com.example.taskflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.model.TaskFlowRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AnalyticsState(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val outOfDateTasks: Int = 0,
    val completionRate: Float = 0f,
    val tasksByCategory: Map<String, Int> = emptyMap()
)

class AnalyticsViewModel(
    private val repository: TaskFlowRepository
) : ViewModel() {

    /**
     * Real-time metrics calculation running completely offline based on Room Flow.
     */
    val analyticsState: StateFlow<AnalyticsState> = repository.allTasks
        .map { tasks ->
            val total = tasks.size
            val completed = tasks.count { it.isCompleted }
            val pending = total - completed
            
            val now = System.currentTimeMillis()
            val outOfDate = tasks.count { !it.isCompleted && it.dueDate < now }
            
            val completionRate = if (total > 0) {
                (completed.toFloat() / total.toFloat()) * 100f
            } else {
                0f
            }
            
            val byCategory = tasks.groupBy { it.category }
                .mapValues { it.value.size }
            
            AnalyticsState(
                totalTasks = total,
                completedTasks = completed,
                pendingTasks = pending,
                outOfDateTasks = outOfDate,
                completionRate = completionRate,
                tasksByCategory = byCategory
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AnalyticsState()
        )
}
