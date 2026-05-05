package com.example.calorietracker.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.CalorieItemEntity
import com.example.calorietracker.data.CalorieRepository
import com.example.calorietracker.data.DailyRecordEntity
import com.example.calorietracker.data.UserProfileEntity
import com.example.calorietracker.util.CalorieUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.calorietracker.data.update.UpdateManager
import com.example.calorietracker.data.update.UpdateStatus
import com.example.calorietracker.data.update.ReleaseInfo
import com.example.calorietracker.util.CalorieUtils

// Medication reminder state
data class MedicationReminderState(
    val show: Boolean = false,
    val medicationNames: List<String> = emptyList(),
    val medicationIndices: List<Int> = emptyList()
)

class MainViewModel(private val repository: CalorieRepository) : ViewModel() {

    private val updateManager = UpdateManager()
    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    // Auto-update on startup
    private val _autoUpdateRelease = MutableStateFlow<ReleaseInfo?>(null)
    val autoUpdateRelease: StateFlow<ReleaseInfo?> = _autoUpdateRelease.asStateFlow()

    // Medication reminder
    private val _medicationReminder = MutableStateFlow(MedicationReminderState())
    val medicationReminder: StateFlow<MedicationReminderState> = _medicationReminder.asStateFlow()

    // Scroll offset for status bar blur effect (0f = not scrolled, 1f = fully scrolled)
    private val _scrollBlurAmount = MutableStateFlow(0f)
    val scrollBlurAmount: StateFlow<Float> = _scrollBlurAmount.asStateFlow()

    fun updateScrollBlur(amount: Float) {
        _scrollBlurAmount.value = amount.coerceIn(0f, 1f)
    }

    private val _selectedDate = MutableStateFlow(CalorieUtils.getTodayString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyRecord: StateFlow<DailyRecordEntity?> = _selectedDate
        .flatMapLatest { date -> repository.getDailyRecord(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyItems: StateFlow<List<CalorieItemEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getItemsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecords: StateFlow<List<DailyRecordEntity>> = repository.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCalorieItems: StateFlow<List<CalorieItemEntity>> = repository.getAllCalorieItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun saveProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
        }
    }

    fun addRecordItem(
        type: String, 
        name: String, 
        calories: Double, 
        carbs: Double = 0.0, 
        protein: Double = 0.0, 
        fat: Double = 0.0, 
        time: String = "", 
        mealCategory: String? = null,
        notes: String? = null, 
        imageUrl: String? = null,
        targetDate: String? = null
    ) {
        viewModelScope.launch {
            val item = CalorieItemEntity(
                id = CalorieUtils.generateId(),
                date = targetDate ?: _selectedDate.value,
                type = type,
                name = name,
                calories = calories,
                carbs = carbs,
                protein = protein,
                fat = fat,
                time = time.ifEmpty { 
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()) 
                },
                mealCategory = mealCategory,
                imageUrl = imageUrl,
                notes = notes,
                createdAt = java.time.Instant.now().toString()
            )
            repository.addRecordItem(item)
        }
    }

    fun updateRecordItem(item: CalorieItemEntity) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun deleteItem(item: CalorieItemEntity) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun updateWeight(weight: Float, targetDate: String? = null) {
        viewModelScope.launch {
            val w = if (weight <= 0f) null else weight
            repository.updateWeight(targetDate ?: _selectedDate.value, w)
        }
    }

    fun updateExcludedExercises(exercises: String) {
        viewModelScope.launch {
            repository.updateExcludedExercises(exercises)
        }
    }

    fun updateShowMacros(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowMacros(show)
        }
    }

    fun updateTodayThemeIndex(index: Int) {
        viewModelScope.launch {
            repository.updateTodayThemeIndex(index)
        }
    }

    fun updateWeekStartDay(weekStartDay: Int) {
        viewModelScope.launch {
            repository.updateWeekStartDay(weekStartDay)
        }
    }

    fun updateWater(amount: Int, targetDate: String? = null) {
        viewModelScope.launch {
            repository.updateWater(targetDate ?: _selectedDate.value, amount)
        }
    }

    fun updateSleep(duration: Int, targetDate: String? = null) {
        viewModelScope.launch {
            repository.updateSleep(targetDate ?: _selectedDate.value, duration)
        }
    }

    fun updateMedicationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateMedicationEnabled(enabled)
        }
    }

    fun updateMedications(medications: String, medicationTimes: String = "") {
        viewModelScope.launch {
            repository.updateMedications(medications, medicationTimes)
        }
    }

    fun updateMedicationTaken(date: String, taken: String) {
        viewModelScope.launch {
            repository.updateMedicationTaken(date, taken)
        }
    }

    fun checkForUpdate(currentVersion: String) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Checking
            _updateStatus.value = updateManager.checkForUpdate(currentVersion)
        }
    }
    
    fun resetUpdateStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }

    fun checkMedicationReminder() {
        viewModelScope.launch {
            try {
                val profile = userProfile.first()
                if (!profile.medicationEnabled || profile.medications.isBlank()) return@launch
                
                val meds = profile.medications.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val times = profile.medicationTimes.split(",").map { it.trim() }
                
                val today = CalorieUtils.getTodayString()
                val record = repository.getDailyRecord(today).first()
                val takenStr = record?.medicationTaken ?: ""
                val taken = if (takenStr.isBlank()) List(meds.size) { "0" }
                            else takenStr.split(",").map { it.trim() }
                
                val now = java.util.Calendar.getInstance()
                val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMinute = now.get(java.util.Calendar.MINUTE)
                val currentTimeMinutes = currentHour * 60 + currentMinute
                
                val overdueNames = mutableListOf<String>()
                val overdueIndices = mutableListOf<Int>()
                
                for (i in meds.indices) {
                    val timeStr = times.getOrElse(i) { "" }
                    if (timeStr.isBlank()) continue
                    val parts = timeStr.split(":")
                    if (parts.size != 2) continue
                    val medHour = parts[0].toIntOrNull() ?: continue
                    val medMinute = parts[1].toIntOrNull() ?: continue
                    val medTimeMinutes = medHour * 60 + medMinute
                    
                    val isTaken = taken.getOrElse(i) { "0" } == "1"
                    
                    if (currentTimeMinutes >= medTimeMinutes && !isTaken) {
                        overdueNames.add(meds[i])
                        overdueIndices.add(i)
                    }
                }
                
                if (overdueNames.isNotEmpty()) {
                    _medicationReminder.value = MedicationReminderState(
                        show = true,
                        medicationNames = overdueNames,
                        medicationIndices = overdueIndices
                    )
                }
            } catch (_: Exception) {
                // Silently fail
            }
        }
    }

    fun dismissMedicationReminder() {
        _medicationReminder.value = MedicationReminderState()
    }

    fun markMedicationsTaken(date: String, indices: List<Int>) {
        viewModelScope.launch {
            try {
                val profile = userProfile.first()
                val meds = profile.medications.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val record = repository.getDailyRecord(date).first()
                val currentTaken = record?.medicationTaken?.split(",")?.map { it.trim() }?.toMutableList()
                    ?: MutableList(meds.size) { "0" }
                while (currentTaken.size < meds.size) currentTaken.add("0")
                for (idx in indices) {
                    if (idx < currentTaken.size) currentTaken[idx] = "1"
                }
                repository.updateMedicationTaken(date, currentTaken.joinToString(","))
                _medicationReminder.value = MedicationReminderState()
            } catch (_: Exception) {
                // Silently fail
            }
        }
    }

    fun checkAutoUpdate(context: Context, currentVersion: String) {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
                val lastDismissed = prefs.getString("last_dismissed_version", "") ?: ""
                
                val status = updateManager.checkForUpdate(currentVersion)
                if (status is UpdateStatus.UpdateAvailable) {
                    val remoteVersion = status.release.tagName.removePrefix("v")
                    if (remoteVersion != lastDismissed) {
                        _autoUpdateRelease.value = status.release
                    }
                }
            } catch (_: Exception) {
                // Silently fail on auto-check - don't bother user
            }
        }
    }

    fun dismissAutoUpdate(context: Context) {
        val release = _autoUpdateRelease.value ?: return
        val version = release.tagName.removePrefix("v")
        context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("last_dismissed_version", version)
            .apply()
        _autoUpdateRelease.value = null
    }

    fun openUpdateInBrowser(context: Context) {
        val release = _autoUpdateRelease.value ?: return
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(release.htmlUrl))
        context.startActivity(intent)
        // Also dismiss this version
        dismissAutoUpdate(context)
    }
}

class MainViewModelFactory(private val repository: CalorieRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
