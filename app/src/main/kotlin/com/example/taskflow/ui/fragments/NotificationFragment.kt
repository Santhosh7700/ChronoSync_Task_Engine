package com.example.taskflow.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.taskflow.viewmodel.NotificationItem
import com.example.taskflow.viewmodel.NotificationViewModel
import com.example.taskflow.viewmodel.UrgencyLevel
import com.example.taskflow.viewmodel.ViewModelFactory

// ─────────────────────────────────────────────
// Fragment
// ─────────────────────────────────────────────

class NotificationFragment : Fragment() {

    private val viewModel: NotificationViewModel by viewModels {
        val app = requireContext().applicationContext as TaskFlowApplication
        ViewModelFactory(app.repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            TaskFlowTheme {
                NotificationScreen(viewModel = viewModel)
            }
        }
    }
}

// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(viewModel: NotificationViewModel) {

    val alerts by viewModel.notificationsList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Notifications",
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

        if (alerts.isEmpty()) {
            EmptyNotificationsView(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier          = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding    = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section header
                item {
                    Text(
                        text  = "${alerts.size} active alert${if (alerts.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(items = alerts, key = { "${it.taskId}_${it.urgency}" }) { item ->
                    NotificationCard(item = item)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Alert card
// ─────────────────────────────────────────────

/** Color palette for each urgency tier */
private data class UrgencyColors(
    val containerColor : Color,
    val borderColor    : Color,
    val iconBg         : Color,
    val label          : String
)

@Composable
private fun urgencyColorsFor(level: UrgencyLevel): UrgencyColors = when (level) {
    UrgencyLevel.HIGH -> UrgencyColors(
        containerColor = Color(0xFFFEF2F2),     // modern very light red
        borderColor    = Color(0xFFFCA5A5),
        iconBg         = Color(0xFFEF4444),
        label          = "URGENT · < 1.5 hr"
    )
    UrgencyLevel.MEDIUM -> UrgencyColors(
        containerColor = Color(0xFFFFF7ED),     // modern very light orange
        borderColor    = Color(0xFFFDBA74),
        iconBg         = Color(0xFFF97316),
        label          = "WARNING · < 3 hr"
    )
    UrgencyLevel.LOW -> UrgencyColors(
        containerColor = Color(0xFFFEFCE8),     // modern very light yellow
        borderColor    = Color(0xFFFDE047),
        iconBg         = Color(0xFFEAB308),
        label          = "HEADS-UP · < 6 hr"
    )
}

@Composable
fun NotificationCard(item: NotificationItem) {
    val colors = urgencyColorsFor(item.urgency)

    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Urgency icon circle ────────────────────────────────────────
            Surface(
                shape = CircleShape,
                color = colors.iconBg,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ── Text content ───────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                // Urgency badge
                Surface(
                    color  = colors.iconBg.copy(alpha = 0.18f),
                    shape  = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text      = colors.label,
                        modifier  = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize  = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color     = colors.iconBg,
                        letterSpacing = 0.6.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Alert message
                Text(
                    text  = item.message,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = if (item.urgency == UrgencyLevel.HIGH)
                                         FontWeight.Bold
                                     else
                                         FontWeight.Medium
                    ),
                    color = Color(0xFF1A1A2E)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────

@Composable
private fun EmptyNotificationsView(modifier: Modifier = Modifier) {
    Box(
        modifier          = modifier.fillMaxSize(),
        contentAlignment  = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier           = Modifier.size(38.dp)
                    )
                }
            }

            Text(
                text      = "All clear!",
                style     = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color     = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = "No tasks are approaching their due time.\nCheck back when deadlines are near.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
