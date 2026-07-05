package com.example.taskflow.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.model.TaskFlowRepository
import com.example.taskflow.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: TaskFlowRepository
) : ViewModel() {

    var isExistingUser by mutableStateOf(false)
        private set

    // True while we are still querying DB on launch — UI stays blank until done
    var isSessionChecking by mutableStateOf(true)
        private set

    var usernameInput by mutableStateOf("")
    var fullNameInput by mutableStateOf("")
    var securityPinInput by mutableStateOf("")

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess.asStateFlow()

    fun checkSessionStatus(forceLoginScreen: Boolean = false) {
        viewModelScope.launch {
            if (repository.isLoggedIn()) {
                _loginSuccess.value = true
                isSessionChecking = false
                return@launch
            }
            if (forceLoginScreen) {
                // Remove Account path: user exists in DB but logged out — go straight to Login
                isExistingUser = true
                isSessionChecking = false
                return@launch
            }
            val user = repository.getUserProfile()
            if (user != null) {
                isExistingUser = true
            }
            isSessionChecking = false
        }
    }

    fun registerNewUser() {
        if (fullNameInput.isBlank() || usernameInput.isBlank() || securityPinInput.length != 4) {
            errorMessage = "Please fill all fields and provide a 4-digit PIN."
            return
        }

        viewModelScope.launch {
            val newUser = User(
                fullName = fullNameInput.trim(),
                username = usernameInput.trim(),
                securityPin = securityPinInput
            )
            repository.insertOrUpdateUser(newUser)
            repository.setLoggedIn(true)
            _loginSuccess.value = true
        }
    }

    fun loginExistingUser() {
        if (usernameInput.isBlank() || securityPinInput.length != 4) {
            errorMessage = "Please enter your username and 4-digit PIN."
            return
        }

        viewModelScope.launch {
            val user = repository.getUserProfile()
            when {
                user == null -> {
                    errorMessage = "Account not found. Please register as a new user."
                }
                user.username != usernameInput.trim() -> {
                    errorMessage = "Account not found. Please register as a new user."
                }
                user.securityPin == securityPinInput -> {
                    repository.setLoggedIn(true)
                    _loginSuccess.value = true
                }
                else -> {
                    errorMessage = "Incorrect PIN. Please try again."
                }
            }
        }
    }

    fun setExistingUserMode(existing: Boolean) {
        isExistingUser = existing
        errorMessage = null
        usernameInput = ""
        securityPinInput = ""
        fullNameInput = ""
    }

    fun restoreAsNewUserWithTemplate(jsonContent: String, typedName: String, typedUsername: String, typedPin: String) {
        val backupData = com.example.taskflow.utils.BackupHelper.restoreFromJson(jsonContent)
        if (backupData == null) {
            errorMessage = "Invalid Backup File."
            return
        }
        
        viewModelScope.launch {
            val tasks = backupData.second
            
            val newUser = User(
                fullName = typedName.trim(),
                username = typedUsername.trim(),
                securityPin = typedPin
            )
            
            repository.clearAllUserData()
            repository.insertOrUpdateUser(newUser)
            repository.insertAll(tasks)
            
            repository.setLoggedIn(true)
            _loginSuccess.value = true
        }
    }

    fun restoreAsExistingUserRecovery(jsonContent: String, typedUsername: String, typedPin: String) {
        val backupData = com.example.taskflow.utils.BackupHelper.restoreFromJson(jsonContent)
        if (backupData == null) {
            errorMessage = "Invalid Backup File."
            return
        }
        
        viewModelScope.launch {
            val user = backupData.first
            val tasks = backupData.second
            
            if (user.username != typedUsername.trim() || user.securityPin != typedPin) {
                errorMessage = "Credentials do not match this backup file."
                return@launch
            }
            
            repository.clearAllUserData()
            repository.insertOrUpdateUser(user)
            repository.insertAll(tasks)
            
            repository.setLoggedIn(true)
            _loginSuccess.value = true
        }
    }
}
