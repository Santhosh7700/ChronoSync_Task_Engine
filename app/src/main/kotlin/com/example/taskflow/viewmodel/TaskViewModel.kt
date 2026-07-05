package com.example.taskflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.model.Task
import com.example.taskflow.model.TaskFlowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskFlowRepository) : ViewModel() {

    // ── Filter State Management ───────────────────────────────────────────────
    
    val currentSearchQuery = MutableStateFlow("")
    val activePrimaryFilter = MutableStateFlow<String?>(null)
    val activeSubFilter = MutableStateFlow<String?>(null)

    // ── Dynamic Filtering Logic & Automated Time Check ────────────────────────
    
    val filteredTasks: StateFlow<List<Task>> = combine(
        repository.allTasks,
        currentSearchQuery,
        activePrimaryFilter,
        activeSubFilter
    ) { tasks, query, primaryFilter, subFilter ->
        tasks.filter { task ->
            // 1. Search Query
            val matchesSearch = task.title.contains(query, ignoreCase = true)

            // 2. Automated Time Check & Status/Created/Category
            val matchesSubFilter = when {
                subFilter == null || subFilter == "All" -> true
                
                primaryFilter == "Status" -> when (subFilter) {
                    "Completed" -> task.isCompleted
                    "Pending" -> !task.isCompleted
                    "Out of Date" -> task.dueDate < System.currentTimeMillis() && !task.isCompleted
                    else -> true
                }
                
                primaryFilter == "Created Date" -> {
                    val calendar = java.util.Calendar.getInstance()
                    
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    val startOfTodayMidnight = calendar.timeInMillis
                    
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    calendar.set(java.util.Calendar.MINUTE, 59)
                    calendar.set(java.util.Calendar.SECOND, 59)
                    calendar.set(java.util.Calendar.MILLISECOND, 999)
                    val endOfTodayMidnight = calendar.timeInMillis

                    val now = System.currentTimeMillis()
                    val sevenDaysMillis = 7 * 86400000L
                    
                    when (subFilter) {
                        "Today" -> task.createdAt in startOfTodayMidnight..endOfTodayMidnight
                        "This Week" -> (now - task.createdAt) < sevenDaysMillis
                        "Older" -> (now - task.createdAt) >= sevenDaysMillis
                        else -> true
                    }
                }
                
                primaryFilter == "Category" -> task.category == subFilter
                
                else -> true
            }

            matchesSearch && matchesSubFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // ── Database Operations ──────────────────────────────────────────

    fun insertTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertTask(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateTask(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteTask(task)
    }

    // Helper for checkbox toggles
    fun toggleCompletion(id: Int, completed: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        repository.toggleCompletion(id, completed)
    }
}
