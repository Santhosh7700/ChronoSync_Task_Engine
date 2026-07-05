package com.example.taskflow.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.taskflow.TaskFlowApplication
import com.example.taskflow.ui.theme.TaskFlowTheme
import com.example.taskflow.viewmodel.SettingsViewModel
import com.example.taskflow.viewmodel.ViewModelFactory

class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels {
        val app = requireContext().applicationContext as TaskFlowApplication
        ViewModelFactory(app.repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            TaskFlowTheme { SettingsScreen(viewModel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val taskCount by viewModel.taskCount.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showProfileUnlocked by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    
    var isEditingProfile by remember { mutableStateOf(false) }
    var editFullName by remember { mutableStateOf("") }
    var editUsername by remember { mutableStateOf("") }
    var editPin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val deleteSuccess by viewModel.deleteSuccess.collectAsState()

    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) {
            val intent = Intent(context, com.example.taskflow.ui.auth.AuthActivity::class.java)
            context.startActivity(intent)
            (context as? Activity)?.finish()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.generateBackupData { jsonString ->
                if (jsonString != null) {
                    try {
                        contentResolver.openOutputStream(uri)?.use {
                            it.write(jsonString.toByteArray())
                        }
                        Toast.makeText(context, "Backup saved!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to write backup.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "No data to backup.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Profile PIN Verification Dialog ──────────────────────────────────────
    if (showProfileDialog) {
        if (!showProfileUnlocked) {
            AlertDialog(
                onDismissRequest = {
                    showProfileDialog = false
                    pinInput = ""
                    pinError = null
                },
                title = { Text("Verify Identity", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Enter your 4-digit PIN to view your profile details.")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 4) pinInput = it },
                            label = { Text("4-Digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        pinError?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (userProfile?.securityPin == pinInput) {
                            showProfileUnlocked = true
                            pinError = null
                        } else {
                            pinError = "Incorrect PIN."
                        }
                    }) { Text("Unlock") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showProfileDialog = false
                        pinInput = ""
                        pinError = null
                    }) { Text("Cancel") }
                }
            )
        } else {
            // ── Unlocked Profile Dialog ──────────────────────────────────────
            Dialog(onDismissRequest = {
                showProfileDialog = false
                showProfileUnlocked = false
                pinInput = ""
                isEditingProfile = false
            }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header with optional edit icon
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (!isEditingProfile) {
                                IconButton(onClick = {
                                    editFullName = userProfile?.fullName ?: ""
                                    editUsername = userProfile?.username ?: ""
                                    editPin = userProfile?.securityPin ?: ""
                                    pinVisible = false
                                    isEditingProfile = true
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isEditingProfile) {
                            OutlinedTextField(
                                value = editFullName,
                                onValueChange = { editFullName = it },
                                label = { Text("Full Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editUsername,
                                onValueChange = { editUsername = it },
                                label = { Text("Username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editPin,
                                onValueChange = { if (it.length <= 4) editPin = it },
                                label = { Text("4-Digit PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = if (pinVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    val icon = if (pinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                    IconButton(onClick = { pinVisible = !pinVisible }) {
                                        Icon(icon, contentDescription = "Toggle PIN visibility")
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { isEditingProfile = false }) { Text("Cancel") }
                                Button(onClick = {
                                    if (editFullName.isNotBlank() && editUsername.isNotBlank() && editPin.length == 4) {
                                        viewModel.updateUserProfile(editFullName, editUsername, editPin)
                                        isEditingProfile = false
                                        Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Please fill all fields and 4-digit PIN", Toast.LENGTH_SHORT).show()
                                    }
                                }) { Text("Save") }
                            }
                        } else {
                            Text(
                                text = userProfile?.fullName ?: "—",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            ProfileDetailRow(label = "Username", value = "@${userProfile?.username ?: "—"}")
                            Spacer(modifier = Modifier.height(8.dp))
                            ProfileDetailRow(label = "Security PIN", value = "••••")
                            Spacer(modifier = Modifier.height(20.dp))
                            TextButton(onClick = {
                                showProfileDialog = false
                                showProfileUnlocked = false
                                pinInput = ""
                            }) { Text("Close") }
                        }
                    }
                }
            }
        }
    }

    // ── Delete Confirmation Dialog ────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure? This permanently deletes all your data and cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.handleDeleteAccountWipe()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete", color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Main Scaffold ─────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("App Settings", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Profile Card ─────────────────────────────────────────────────
            Card(
                onClick = {
                    showProfileUnlocked = false
                    pinInput = ""
                    pinError = null
                    showProfileDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = userProfile?.fullName ?: "Loading...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "@${userProfile?.username ?: "..."}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Tap to view details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Dark Theme Toggle ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌙  Dark Theme",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleTheme(it) }
                    )
                }
            }

            // ── Section Label ─────────────────────────────────────────────────
            Text(
                text = "DATA",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )

            // ── Backup Button (Blue/Primary) ──────────────────────────────────
            Button(
                onClick = { 
                    if (taskCount == 0) {
                        Toast.makeText(context, "Cannot export backup. Please add at least one task to your dashboard first.", Toast.LENGTH_LONG).show()
                    } else {
                        exportLauncher.launch("TaskFlow_Backup.json") 
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "☁  Backup Data to Device",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // ── Section Label ─────────────────────────────────────────────────
            Text(
                text = "ACCOUNT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )

            // ── Remove Account Button (Amber/Warning) ─────────────────────────
            Button(
                onClick = {
                    viewModel.handleRemoveAccountSession()
                    val intent = Intent(context, com.example.taskflow.ui.auth.AuthActivity::class.java).apply {
                        putExtra("FORCE_LOGIN", true)
                    }
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B) // Amber
                )
            ) {
                Text(
                    text = "🚪  Remove Account (Log Out)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // ── Delete Account Button (Red/Error) ─────────────────────────────
            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "🗑  Delete Account & All Data",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onError
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
