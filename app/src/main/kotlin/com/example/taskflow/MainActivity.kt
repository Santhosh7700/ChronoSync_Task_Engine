package com.example.taskflow

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.taskflow.ui.fragments.AnalyticsFragment
import com.example.taskflow.ui.fragments.MainScreenFragment
import com.example.taskflow.ui.fragments.NotificationFragment
import com.example.taskflow.ui.fragments.SettingsFragment
import com.example.taskflow.ui.theme.TaskFlowTheme
import android.content.Intent
import com.example.taskflow.ui.auth.AuthActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

// ─────────────────────────────────────────────
// Bottom navigation item descriptor
// ─────────────────────────────────────────────

private data class NavItem(
    val label       : String,
    val filledIcon  : ImageVector,
    val outlinedIcon: ImageVector,
    val tag         : String
)

private val NAV_ITEMS = listOf(
    NavItem("Home",          Icons.Filled.Home,          Icons.Outlined.Home,          "home"),
    NavItem("Alerts",        Icons.Filled.Notifications, Icons.Outlined.Notifications, "notifications"),
    NavItem("Analytics",     Icons.Filled.BarChart,      Icons.Outlined.BarChart,      "analytics"),
    NavItem("Settings",      Icons.Filled.Settings,      Icons.Outlined.Settings,      "settings")
)

// ─────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────

class MainActivity : AppCompatActivity() {

    // Fragment instances — created once, shown/hidden for instant Steam-style switching
    private val fragments: Array<Fragment> by lazy {
        arrayOf(
            MainScreenFragment(),
            NotificationFragment(),
            AnalyticsFragment(),
            SettingsFragment()
        )
    }

    // Tracks which tab is currently visible (survives config-changes via instance state)
    private var currentTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ── STRICT DUAL-FACTOR ENTRY GATE ────────────────────────────────────
        // Blocks dashboard access using a synchronous Room + SharedPreferences check.
        // This runs BEFORE setContentView so absolutely no UI is ever rendered for
        // an unauthenticated user, even if the Activity is somehow on the back-stack.
        val repository = (application as TaskFlowApplication).repository

        val isLoggedIn = repository.isLoggedIn()
        val userExists = runBlocking(Dispatchers.IO) { repository.getUserProfile() != null }

        if (!isLoggedIn || !userExists) {
            // ── Security violation detected ──────────────────────────────────
            // Either the SP flag is stale (cloud-restored) or the DB is wiped.
            // Force-clear the session flag and eject to the Auth gateway.
            repository.setLoggedIn(false)

            val intent = Intent(this, AuthActivity::class.java).apply {
                // Clear entire back-stack so the user cannot press Back into dashboard
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return // Stop onCreate — no fragments, no views are created below
        }
        // ── Gate passed: user is fully authenticated ─────────────────────────

        setContentView(com.example.taskflow.R.layout.activity_main)

        // ── Restore or create fragments ───────────────────────────────────────
        if (savedInstanceState == null) {
            // First launch: add all fragments, hide all except Home
            supportFragmentManager.beginTransaction().apply {
                fragments.forEachIndexed { index, fragment ->
                    add(com.example.taskflow.R.id.fragment_container, fragment, NAV_ITEMS[index].tag)
                    if (index != 0) hide(fragment)
                }
            }.commitNow()
        } else {
            // Config-change restore: find already-added fragments
            currentTabIndex = savedInstanceState.getInt(KEY_SELECTED_TAB, 0)
            NAV_ITEMS.forEachIndexed { i, item ->
                val found = supportFragmentManager.findFragmentByTag(item.tag)
                if (found != null) {
                    // Assign found fragment back into our array slot via reflection-safe cast
                    @Suppress("UNCHECKED_CAST")
                    (fragments as Array<Fragment?>)[i] = found
                }
            }
        }

        // ── Full-Compose shell with NavigationBar ─────────────────────────────
        val composeView = findViewById<androidx.compose.ui.platform.ComposeView>(com.example.taskflow.R.id.bottom_nav_compose)
        composeView.setContent {
            TaskFlowTheme {
                TaskFlowAppShell(
                    initialTab          = currentTabIndex,
                    onTabSelected       = { index -> switchToTab(index) }
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_TAB, currentTabIndex)
    }

    // ── Steam-style instant tab switch: show/hide, no recreation ─────────────

    private fun switchToTab(index: Int) {
        if (index == currentTabIndex) return
        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            hide(fragments[currentTabIndex])
            show(fragments[index])
        }.commit()
        currentTabIndex = index
    }

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
    }
}

// ─────────────────────────────────────────────
// Compose shell (Scaffold + NavigationBar)
// ─────────────────────────────────────────────

@Composable
private fun TaskFlowAppShell(
    initialTab         : Int,
    onTabSelected      : (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    Surface(
        color = MaterialTheme.colorScheme.background
    ) {
        TaskFlowNavigationBar(
            selectedIndex = selectedTab,
            onItemClick   = { index ->
                selectedTab = index
                onTabSelected(index)
            }
        )
    }
}

// ─────────────────────────────────────────────
// NavigationBar
// ─────────────────────────────────────────────

@Composable
private fun TaskFlowNavigationBar(
    selectedIndex: Int,
    onItemClick  : (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NAV_ITEMS.forEachIndexed { index, item ->
            val isSelected = selectedIndex == index

            NavigationBarItem(
                selected  = isSelected,
                onClick   = { onItemClick(index) },
                icon      = {
                    Icon(
                        imageVector        = if (isSelected) item.filledIcon else item.outlinedIcon,
                        contentDescription = item.label
                    )
                },
                label     = {
                    Text(
                        text       = item.label,
                        fontSize   = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors    = NavigationBarItemDefaults.colors(
                    selectedIconColor       = MaterialTheme.colorScheme.primary,
                    selectedTextColor       = MaterialTheme.colorScheme.primary,
                    indicatorColor          = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor     = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor     = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}