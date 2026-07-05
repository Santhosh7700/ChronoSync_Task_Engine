package com.example.taskflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.model.TaskFlowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

// ── Data models ────────────────────────────────────────────────────────────────

enum class UrgencyLevel {
    /** 6-hour window  – informational yellow/amber tint  */
    LOW,

    /** 3-hour window  – warning orange tint              */
    MEDIUM,

    /** 1.5-hour window  – critical red tint                */
    HIGH
}

data class NotificationItem(
    val taskId: Int,
    val urgency: UrgencyLevel,
    val message: String
)

// ── ViewModel ──────────────────────────────────────────────────────────────────

class NotificationViewModel(
    private val repository: TaskFlowRepository
) : ViewModel() {

    companion object {
        private const val HOUR_MS         = 3_600_000L   // 1 hour in ms
        private const val REFRESH_INTERVAL = 60_000L     // re-evaluate every 60 s
    }

    /**
     * A cold flow that emits [Unit] every [REFRESH_INTERVAL] ms so the alert
     * windows are recalculated even if the task list hasn't changed.
     * Runs entirely on [Dispatchers.Default] – off the main thread.
     */
    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(REFRESH_INTERVAL)
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Live stream of time-sensitive alert items for all PENDING tasks.
     *
     * Alert windows:
     *  • [HIGH]   <= 1.5h remaining  → 🚨 1.5-hour urgent
     *  • [MEDIUM] <= 3.0h remaining  → ❗ 3-hour warning
     *  • [LOW]    <= 6.0h remaining  → ⏳ 6-hour heads-up
     *
     * Sorted so the most urgent items always appear first.
     */
    val notificationsList: StateFlow<List<NotificationItem>> = combine(
        repository.allTasks,
        ticker
    ) { tasks, _ ->
        val now = System.currentTimeMillis()

        tasks
            .filter { task -> !task.isCompleted }           // pending only
            .mapNotNull { task ->
                val rawTimeLeft = task.dueDate - now

                when {
                    // ── 🚨 1.5-hour window (<= 1.5 h) ──────────────────
                    rawTimeLeft in 0..(1.5 * HOUR_MS).toLong() ->
                        NotificationItem(
                            taskId  = task.id,
                            urgency = UrgencyLevel.HIGH,
                            message = "🚨 URGENT: \"${task.title}\" is due in less than 1.5 hours!"
                        )

                    // ── ❗ 3-hour window (<= 3.0 h) ──────────────────
                    rawTimeLeft in ((1.5 * HOUR_MS).toLong() + 1)..(3 * HOUR_MS).toLong() ->
                        NotificationItem(
                            taskId  = task.id,
                            urgency = UrgencyLevel.MEDIUM,
                            message = "❗ Only 3 hours left for \"${task.title}\"!"
                        )

                    // ── ⏳ 6-hour window (<= 6.0 h) ─────────────────
                    rawTimeLeft in ((3 * HOUR_MS).toLong() + 1)..(6 * HOUR_MS).toLong() ->
                        NotificationItem(
                            taskId  = task.id,
                            urgency = UrgencyLevel.LOW,
                            message = "⏳ \"${task.title}\" is due in 6 hours! Don't let it expire."
                        )

                    else -> null   // outside all alert windows
                }
            }
            // Most urgent first → HIGH before MEDIUM before LOW
            .sortedBy { it.urgency.ordinal }
    }
    .flowOn(Dispatchers.Default)  // all filtering runs off the main thread
    .stateIn(
        scope          = viewModelScope,
        started        = SharingStarted.WhileSubscribed(5_000),
        initialValue   = emptyList()
    )
}
