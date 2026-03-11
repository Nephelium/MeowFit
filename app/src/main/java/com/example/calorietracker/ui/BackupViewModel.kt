package com.example.calorietracker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.AppDatabase
import com.example.calorietracker.data.backup.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val backupManager = BackupManager(application, db.userDao(), db.recordDao())

    private val _status = MutableStateFlow<String>("就绪")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastAutoBackupTime = MutableStateFlow(formatTime(backupManager.getAutoBackupTime()))
    val lastAutoBackupTime: StateFlow<String> = _lastAutoBackupTime.asStateFlow()

    init {
        checkAutoBackup()
    }

    private fun checkAutoBackup() {
        val lastTime = backupManager.getAutoBackupTime()
        _lastAutoBackupTime.value = formatTime(lastTime)
        
        // Simple daily check: if last backup was not today, perform backup
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastBackupDay = if (lastTime > 0) SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(lastTime)) else ""
        
        if (today != lastBackupDay) {
            performAutoBackup()
        }
    }

    private fun formatTime(time: Long): String {
        return if (time > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(time))
        } else {
            "无"
        }
    }

    fun performAutoBackup() {
        viewModelScope.launch {
            try {
                backupManager.performAutoBackup()
                _lastAutoBackupTime.value = formatTime(backupManager.getAutoBackupTime())
            } catch (e: Exception) {
                // Silent fail for auto backup or log
            }
        }
    }

    fun performManualBackup(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _status.value = "正在备份..."
            try {
                val success = backupManager.performManualBackup(uri)
                _status.value = if (success) "备份成功" else "备份失败"
            } catch (e: Exception) {
                _status.value = "错误: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _status.value = "正在恢复..."
            try {
                val success = backupManager.restoreFromUri(uri)
                _status.value = if (success) "恢复成功" else "恢复失败"
            } catch (e: Exception) {
                _status.value = "错误: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class BackupViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BackupViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
