package com.example.taskflow.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskflow.MainActivity
import com.example.taskflow.TaskFlowApplication
import com.example.taskflow.ui.theme.TaskFlowTheme
import com.example.taskflow.viewmodel.AuthViewModel
import com.example.taskflow.viewmodel.ViewModelFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

class AuthActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels {
        ViewModelFactory((application as TaskFlowApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val forceLogin = intent.getBooleanExtra("FORCE_LOGIN", false)
        val repository = (application as TaskFlowApplication).repository
        
        // 1. Dual-Factor Security Check: Verify SP flag AND Room DB (Non-blocking)
        if (!forceLogin && repository.isLoggedIn()) {
            lifecycleScope.launch {
                val user = repository.getUserProfile()
                if (user != null) {
                    startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                    finish()
                } else {
                    // Security loophole: Auto-backup preserved SP flag, but DB is empty
                    repository.setLoggedIn(false)
                    viewModel.checkSessionStatus(forceLogin)
                }
            }
        } else {
            // 2. Otherwise, prepare Auth gateway
            viewModel.checkSessionStatus(forceLogin)
        }

        setContent {
            TaskFlowTheme {
                AuthScreen(viewModel = viewModel, onLoginSuccess = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                })
            }
        }
    }
}

@Composable
fun AuthScreen(viewModel: AuthViewModel, onLoginSuccess: () -> Unit) {
    val loginSuccess by viewModel.loginSuccess.collectAsState()

    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            onLoginSuccess()
        }
    }

    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val restoreNewUserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (json != null) {
                    viewModel.restoreAsNewUserWithTemplate(json, viewModel.fullNameInput, viewModel.usernameInput, viewModel.securityPinInput)
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to read backup file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val restoreExistingUserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (json != null) {
                    viewModel.restoreAsExistingUserRecovery(json, viewModel.usernameInput, viewModel.securityPinInput)
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to read backup file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold { innerPadding ->
        if (!viewModel.isSessionChecking) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradientBrush)
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Logo / Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "TaskFlow",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (viewModel.isExistingUser) {
                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        OutlinedTextField(
                            value = viewModel.usernameInput,
                            onValueChange = { viewModel.usernameInput = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = viewModel.securityPinInput,
                            onValueChange = { if (it.length <= 4) viewModel.securityPinInput = it },
                            label = { Text("4-Digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { viewModel.loginExistingUser() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Login", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = {
                            if (viewModel.usernameInput.isBlank() || viewModel.securityPinInput.length != 4) {
                                Toast.makeText(context, "Please enter your username and PIN to recover account.", Toast.LENGTH_SHORT).show()
                            } else {
                                restoreExistingUserLauncher.launch("*/*")
                            }
                        }) {
                            Text("Restore Account from Backup")
                        }
                        
                        TextButton(onClick = { viewModel.setExistingUserMode(false) }) {
                            Text(
                                text = "New user? Register here",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = "Create Account",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        OutlinedTextField(
                            value = viewModel.fullNameInput,
                            onValueChange = { viewModel.fullNameInput = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = viewModel.usernameInput,
                            onValueChange = { viewModel.usernameInput = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = viewModel.securityPinInput,
                            onValueChange = { if (it.length <= 4) viewModel.securityPinInput = it },
                            label = { Text("4-Digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { viewModel.registerNewUser() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = {
                            if (viewModel.fullNameInput.isBlank() || viewModel.usernameInput.isBlank() || viewModel.securityPinInput.length != 4) {
                                Toast.makeText(context, "Please fill all fields to import tasks.", Toast.LENGTH_SHORT).show()
                            } else {
                                restoreNewUserLauncher.launch("*/*")
                            }
                        }) {
                            Text("Import Tasks from Backup")
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = { viewModel.setExistingUserMode(true) }) {
                            Text(
                                text = "Already have an account? Login",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    viewModel.errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}
}
