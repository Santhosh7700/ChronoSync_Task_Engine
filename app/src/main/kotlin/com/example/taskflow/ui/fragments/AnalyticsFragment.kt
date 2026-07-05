package com.example.taskflow.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.taskflow.TaskFlowApplication
import com.example.taskflow.ui.theme.TaskFlowTheme
import com.example.taskflow.viewmodel.AnalyticsViewModel
import com.example.taskflow.viewmodel.ViewModelFactory

class AnalyticsFragment : Fragment() {

    private val viewModel: AnalyticsViewModel by viewModels {
        val app = requireContext().applicationContext as TaskFlowApplication
        ViewModelFactory(app.repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { TaskFlowTheme { AnalyticsScreen(viewModel) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val state by viewModel.analyticsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Productivity Metrics",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Top Summary Grid ──────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Tasks",
                        value = state.totalTasks.toString(),
                        containerColor = Color(0xFFF3F4F6), // Light gray
                        contentColor = Color(0xFF1F2937)
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Completed",
                        value = state.completedTasks.toString(),
                        containerColor = Color(0xFFD1FAE5), // Light green
                        contentColor = Color(0xFF065F46)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Pending",
                        value = state.pendingTasks.toString(),
                        containerColor = Color(0xFFDBEAFE), // Light blue
                        contentColor = Color(0xFF1E40AF)
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Out of Date",
                        value = state.outOfDateTasks.toString(),
                        containerColor = Color(0xFFFEF2F2), // Light red
                        contentColor = Color(0xFF991B1B)
                    )
                }
            }

            // ── Custom Donut Chart Summary ────────────────────────────────
            item {
                DonutChartSummary(
                    completed = state.completedTasks,
                    pending = state.pendingTasks,
                    outOfDate = state.outOfDateTasks,
                    total = state.totalTasks,
                    rate = state.completionRate
                )
            }

            // ── Category Breakdown ────────────────────────────────────────
            item {
                Text(
                    text = "Tasks by Category",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                if (state.tasksByCategory.isEmpty()) {
                    Text(
                        text = "No tasks available to categorize.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            state.tasksByCategory.entries.forEachIndexed { index, entry ->
                                CategoryBarGraph(
                                    category = entry.key,
                                    count = entry.value,
                                    total = state.totalTasks
                                )
                                if (index < state.tasksByCategory.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

@Composable
fun DonutChartSummary(
    completed: Int,
    pending: Int,
    outOfDate: Int,
    total: Int,
    rate: Float
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Task Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // If there are no tasks, draw a neutral ring
                if (total == 0) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 30f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }
                } else {
                    val completedSweep = (completed.toFloat() / total) * 360f
                    val outOfDateSweep = (outOfDate.toFloat() / total) * 360f
                    val pendingSweep = (pending.toFloat() / total) * 360f
                    
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 36f, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
                        
                        var currentStart = -90f // Start from top
                        
                        // Green for Completed
                        if (completedSweep > 0) {
                            drawArc(
                                color = Color(0xFF10B981), // Emerald green
                                startAngle = currentStart,
                                sweepAngle = completedSweep,
                                useCenter = false,
                                style = stroke
                            )
                            currentStart += completedSweep
                        }
                        
                        // Orange/Yellow for Pending
                        if (pendingSweep > 0) {
                            drawArc(
                                color = Color(0xFFF59E0B), // Amber
                                startAngle = currentStart,
                                sweepAngle = pendingSweep,
                                useCenter = false,
                                style = stroke
                            )
                            currentStart += pendingSweep
                        }
                        
                        // Red for Out of Date
                        if (outOfDateSweep > 0) {
                            drawArc(
                                color = Color(0xFFEF4444), // Red
                                startAngle = currentStart,
                                sweepAngle = outOfDateSweep,
                                useCenter = false,
                                style = stroke
                            )
                            currentStart += outOfDateSweep
                        }
                    }
                }
                
                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${rate.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color(0xFF10B981), label = "Completed")
                LegendItem(color = Color(0xFFF59E0B), label = "Pending")
                LegendItem(color = Color(0xFFEF4444), label = "Overdue")
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryBarGraph(category: String, count: Int, total: Int) {
    val fraction = if (total > 0) count.toFloat() / total.toFloat() else 0f
    val color = getCategoryColor(category)
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$count / $total Tasks",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.LightGray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}
