package com.example.taskflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.model.TaskFlowRepository
import com.example.taskflow.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: TaskFlowRepository
) : ViewModel() {

    private val _isDarkMode = MutableStateFlow(repository.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess.asStateFlow()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    val taskCount: StateFlow<Int> = repository.allTasks
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        viewModelScope.launch {
            _userProfile.value = repository.getUserProfile()
        }
    }

    fun toggleTheme(isDark: Boolean) {
        _isDarkMode.value = isDark
        repository.setDarkMode(isDark)
    }

    fun handleRemoveAccountSession() {
        repository.logoutSession()
    }

    fun handleDeleteAccountWipe() {
        viewModelScope.launch {
            repository.clearAllUserData() // suspends until ALL tables are wiped
            _deleteSuccess.value = true   // only fires AFTER DB wipe is 100% done
        }
    }

    fun generateBackupData(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserProfile()
            if (user != null) {
                val tasks = repository.allTasks.first()
                val json = com.example.taskflow.utils.BackupHelper.backupToJson(user, tasks)
                onResult(json)
            } else {
                onResult(null)
            }
        }
    }

    fun updateUserProfile(fullName: String, username: String, pin: String) {
        viewModelScope.launch {
            val updatedUser = User(
                fullName = fullName.trim(),
                username = username.trim(),
                securityPin = pin
            )
            repository.insertOrUpdateUser(updatedUser)
            _userProfile.value = updatedUser
        }
    }
}
