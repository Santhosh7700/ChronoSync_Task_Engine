package com.example.taskflow.ui.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.taskflow.TaskFlowApplication
import com.example.taskflow.model.Task
import com.example.taskflow.ui.theme.TaskFlowTheme
import com.example.taskflow.viewmodel.TaskViewModel
import com.example.taskflow.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────
// Helpers & constants
// ─────────────────────────────────────────────

enum class TaskCategory(val label: String, val colorHex: Long) {
    PERSONAL("Personal", 0xFF7C4DFF),
    WORK("Work", 0xFF0091EA),
    STUDY("Study", 0xFF00BFA5),
    SHOPPING("Shopping", 0xFFFF6D00),
    FINANCE("Finance", 0xFF2E7D32)
}

fun TaskCategory.toComposeColor(): Color = Color(colorHex)

fun getCategoryColor(categoryLabel: String): Color {
    val cat = TaskCategory.values().find { it.label == categoryLabel }
    return cat?.toComposeColor() ?: Color.Gray
}

/** "Due: Jun 25, 2026 · 10:30 AM" */
fun formatDueDate(epochMillis: Long): String {
    val fmt = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
    return "Due: ${fmt.format(Date(epochMillis))}"
}

/** "Cre: Jun 25, 10:00 AM" */
fun formatCreatedAt(epochMillis: Long): String {
    val fmt = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return "Cre: ${fmt.format(Date(epochMillis))}"
}

enum class PrimaryFilter(val label: String) {
    STATUS("Status"),
    CREATED_DATE("Created Date"),
    CATEGORY("Category")
}

val subFilters: Map<PrimaryFilter, List<String>> = mapOf(
    PrimaryFilter.STATUS to listOf("All", "Completed", "Pending", "Out of Date"),
    PrimaryFilter.CREATED_DATE to listOf("Today", "This Week", "Older"),
    PrimaryFilter.CATEGORY to listOf("Personal", "Work", "Study", "Shopping", "Finance")
)

val categoryLabels = listOf("Personal", "Work", "Study", "Shopping", "Finance")

// ─────────────────────────────────────────────
// Fragment
// ─────────────────────────────────────────────

class MainScreenFragment : Fragment() {

    private val viewModel: TaskViewModel by viewModels {
        val app = requireContext().applicationContext as TaskFlowApplication
        ViewModelFactory(app.repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TaskFlowTheme {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TaskViewModel) {

    val filteredTasks   by viewModel.filteredTasks.collectAsState()
    val searchQuery     by viewModel.currentSearchQuery.collectAsState()
    val activePrimary   by viewModel.activePrimaryFilter.collectAsState()
    val activeSubFilter by viewModel.activeSubFilter.collectAsState()

    var showSheet by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TaskFlow",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingTask = null; showSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Task")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TaskSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.currentSearchQuery.value = it }
            )

            Spacer(modifier = Modifier.height(4.dp))

            QuickFilterBar(
                activePrimaryStr = activePrimary,
                activeSubFilter  = activeSubFilter,
                onPrimaryClick   = { clicked ->
                    val lbl = clicked.label
                    viewModel.activePrimaryFilter.value = if (activePrimary == lbl) null else lbl
                    viewModel.activeSubFilter.value = null
                },
                onSubFilterClick = { sub ->
                    viewModel.activeSubFilter.value = if (activeSubFilter == sub) null else sub
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks yet. Tap ＋ to add one!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = filteredTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onCompletedChange = { checked ->
                                viewModel.updateTask(task.copy(isCompleted = checked))
                            },
                            onEditClick = {
                                editingTask = task
                                showSheet = true
                            },
                            onDeleteClick = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }

        // ── Add Task Bottom Sheet ────────────────────────────────────────────
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                AddTaskSheetContent(
                    initialTask = editingTask,
                    onSave = { task ->
                        if (task.id == 0) viewModel.insertTask(task)
                        else viewModel.updateTask(task)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showSheet = false
                            editingTask = null
                        }
                    },
                    onCancel = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showSheet = false
                            editingTask = null
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Add Task Bottom Sheet Content
// ─────────────────────────────────────────────

@Composable
fun AddTaskSheetContent(
    initialTask: Task? = null,
    onSave: (Task) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var title            by remember { mutableStateOf(initialTask?.title ?: "") }
    var description      by remember { mutableStateOf(initialTask?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(initialTask?.category ?: categoryLabels.first()) }
    var pickedDueMillis  by remember { mutableLongStateOf(initialTask?.dueDate ?: -1L) }

    // Reusable picker launcher: opens DatePickerDialog, then TimePickerDialog
    fun openDateTimePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        cal.set(year, month, day, hour, minute, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        if (cal.timeInMillis < System.currentTimeMillis()) {
                            android.widget.Toast.makeText(context, "Cannot select a past time", android.widget.Toast.LENGTH_SHORT).show()
                            pickedDueMillis = -1L
                        } else {
                            pickedDueMillis = cal.timeInMillis
                        }
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false  // 12-hour format with AM/PM
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Prevent picking dates in the past
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    val isTitleValid  = title.isNotBlank()
    val isDueSelected = pickedDueMillis > 0L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── Header ──────────────────────────────────────────────────────────
        Text(
            text = if (initialTask != null) "Edit Task" else "New Task",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Title ───────────────────────────────────────────────────────────
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title *") },
            placeholder = { Text("e.g. Finish project report") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Description ─────────────────────────────────────────────────────
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            placeholder = { Text("Optional details…") },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Category chips ───────────────────────────────────────────────────
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categoryLabels.forEach { cat ->
                val isSelected = selectedCategory == cat
                val catColor   = getCategoryColor(cat)
                InputChip(
                    selected = isSelected,
                    onClick  = { selectedCategory = cat },
                    label = {
                        Text(
                            text  = cat,
                            fontSize = 12.sp,
                            color = if (isSelected) catColor
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = androidx.compose.material3.InputChipDefaults.inputChipColors(
                        selectedContainerColor = catColor.copy(alpha = 0.18f)
                    )
                )
            }
        }

        // ── Due Date / Time picker ───────────────────────────────────────────
        Text(
            text = "Due Date & Time *",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            color = if (isDueSelected)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openDateTimePicker() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "📅", fontSize = 18.sp)
                Text(
                    text = if (isDueSelected) formatDueDate(pickedDueMillis)
                           else "Tap to pick date & time",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isDueSelected) FontWeight.Medium else FontWeight.Normal
                    ),
                    color = if (isDueSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!isDueSelected) {
            Text(
                text = "⚠ Due date is required",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Action buttons ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor   = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (isTitleValid && isDueSelected) {
                        val updatedTask = initialTask?.copy(
                            title       = title.trim(),
                            description = description.trim(),
                            category    = selectedCategory,
                            dueDate     = pickedDueMillis
                        ) ?: Task(
                            title       = title.trim(),
                            description = description.trim(),
                            category    = selectedCategory,
                            dueDate     = pickedDueMillis,
                            isCompleted = false,
                            createdAt   = System.currentTimeMillis()
                        )
                        onSave(updatedTask)
                    }
                },
                enabled = isTitleValid && isDueSelected,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Task")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────
// Search bar
// ─────────────────────────────────────────────

@Composable
fun TaskSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text  = "Search tasks…",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ─────────────────────────────────────────────
// Filter bar
// ─────────────────────────────────────────────

@Composable
fun QuickFilterBar(
    activePrimaryStr: String?,
    activeSubFilter: String?,
    onPrimaryClick: (PrimaryFilter) -> Unit,
    onSubFilterClick: (String) -> Unit
) {
    val activePrimary = PrimaryFilter.values().find { it.label == activePrimaryStr }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimaryFilter.entries.forEach { filter ->
                FilterChip(
                    selected = activePrimary == filter,
                    onClick  = { onPrimaryClick(filter) },
                    label    = { Text(filter.label) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        AnimatedVisibility(
            visible = activePrimary != null,
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            val subs = activePrimary?.let { subFilters[it] } ?: emptyList()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subs.forEach { sub ->
                    FilterChip(
                        selected = activeSubFilter == sub,
                        onClick  = { onSubFilterClick(sub) },
                        label    = { Text(sub, fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor     = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Task Card  ← now shows createdAt
// ─────────────────────────────────────────────

@Composable
fun TaskCard(
    task: Task,
    onCompletedChange: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isCompleted = task.isCompleted

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isCompleted) 0.55f else 1f),
        shape = RoundedCornerShape(20.dp),
        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = onCompletedChange,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                // ── Title row + delete icon ──────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = task.title,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize      = 18.sp,
                            fontWeight    = FontWeight.Bold,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough
                                            else TextDecoration.None,
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick  = onEditClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Task",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(
                            onClick  = onDeleteClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Task",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── Description ──────────────────────────────────────────────
                if (task.description.isNotBlank()) {
                    Text(
                        text  = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── Due date ─────────────────────────────────────────────────
                Text(
                    text  = formatDueDate(task.dueDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.dueDate < System.currentTimeMillis() && !task.isCompleted)
                                MaterialTheme.colorScheme.error          // overdue → red
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // ── Category badge + Created date row ────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CategoryBadge(categoryLabel = task.category)

                    Text(
                        text  = formatCreatedAt(task.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Category badge
// ─────────────────────────────────────────────

@Composable
fun CategoryBadge(categoryLabel: String) {
    val color = getCategoryColor(categoryLabel)
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(color = color, shape = RoundedCornerShape(50))
            )
            Text(
                text          = categoryLabel,
                color         = color,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 0.4.sp
            )
        }
    }
}
