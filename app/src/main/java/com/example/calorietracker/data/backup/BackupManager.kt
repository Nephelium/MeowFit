package com.example.calorietracker.data.backup

import android.content.Context
import android.net.Uri
import com.example.calorietracker.data.CalorieItemEntity
import com.example.calorietracker.data.DailyRecordEntity
import com.example.calorietracker.data.RecordDao
import com.example.calorietracker.data.UserDao
import com.example.calorietracker.data.UserProfileEntity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// DTO for UserProfile to handle version compatibility (missing fields in JSON)
data class BackupUserProfile(
    val id: Int = 1,
    val name: String? = null,
    val gender: String? = null,
    val age: Int? = null,
    val birthDate: String? = null,
    val height: Float? = null,
    val weight: Float? = null,
    val targetWeight: Float? = null,
    val activityLevel: String? = null,
    val goal: String? = null,
    val dailyCalorieTarget: Int? = null,
    val sleepGoal: Float? = null,
    val showMacros: Boolean? = null,
    val excludedExercises: String? = null,
    val createdAt: String? = null
) {
    fun toEntity(): UserProfileEntity {
        return UserProfileEntity(
            id = id,
            name = name ?: "User",
            gender = gender ?: "male",
            age = age ?: 25,
            birthDate = birthDate ?: "",
            height = height ?: 170f,
            weight = weight ?: 60f,
            targetWeight = targetWeight ?: 55f,
            activityLevel = activityLevel ?: "sedentary",
            goal = goal ?: "lose",
            dailyCalorieTarget = dailyCalorieTarget ?: 2000,
            sleepGoal = sleepGoal ?: 7.5f,
            showMacros = showMacros ?: false,
            excludedExercises = excludedExercises ?: "",
            createdAt = createdAt ?: java.util.Date().toString()
        )
    }

    companion object {
        fun fromEntity(entity: UserProfileEntity): BackupUserProfile {
            return BackupUserProfile(
                id = entity.id,
                name = entity.name,
                gender = entity.gender,
                age = entity.age,
                birthDate = entity.birthDate,
                height = entity.height,
                weight = entity.weight,
                targetWeight = entity.targetWeight,
                activityLevel = entity.activityLevel,
                goal = entity.goal,
                dailyCalorieTarget = entity.dailyCalorieTarget,
                sleepGoal = entity.sleepGoal,
                showMacros = entity.showMacros,
                excludedExercises = entity.excludedExercises,
                createdAt = entity.createdAt
            )
        }
    }
}

data class BackupData(
    val userProfile: BackupUserProfile?,
    val dailyRecords: List<DailyRecordEntity>,
    val calorieItems: List<CalorieItemEntity>,
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

class BackupManager(
    private val context: Context,
    private val userDao: UserDao,
    private val recordDao: RecordDao
) {
    private val gson = Gson()
    private val backupDir = File(context.filesDir, "backups")

    init {
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }

    private suspend fun getBackupData(): BackupData {
        return withContext(Dispatchers.IO) {
            val userProfile = userDao.getUserProfileSync()
            val dailyRecords = recordDao.getAllRecordsSync()
            val calorieItems = recordDao.getAllCalorieItemsSync()
            BackupData(
                userProfile?.let { BackupUserProfile.fromEntity(it) }, 
                dailyRecords, 
                calorieItems
            )
        }
    }

    suspend fun performAutoBackup(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val data = getBackupData()
                val json = gson.toJson(data)
                val file = File(backupDir, "auto_backup.json")
                file.writeText(json)
                
                // Also save to Downloads folder as requested
                exportBackupToDownloads(json)
                
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    fun exportBackupToDownloads(json: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val filename = "MeowFit_Backup_$timestamp.json"
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/MeowFit")
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }
                
                val resolver = context.contentResolver
                // Check if file exists (optional, MediaStore handles duplicates by appending numbers usually)
                // But to avoid spamming duplicates if backup runs multiple times a day, we could query first.
                // For simplicity and safety, we let MediaStore handle it.
                
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { os ->
                        os.write(json.toByteArray())
                    }
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadsDir, "MeowFit")
                if (!appDir.exists()) appDir.mkdirs()
                val file = File(appDir, filename)
                file.writeText(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun performManualBackup(targetUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val data = getBackupData()
                val json = gson.toJson(data)
                
                context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun restoreFromUri(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = InputStreamReader(inputStream)
                    val backupData = gson.fromJson(reader, BackupData::class.java)
                    restoreData(backupData)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private suspend fun restoreData(backupData: BackupData) {
        // Restore User Profile
        backupData.userProfile?.let {
            userDao.insertUserProfile(it.toEntity())
        }
        
        // Restore Records
        if (backupData.dailyRecords.isNotEmpty()) {
            recordDao.insertDailyRecords(backupData.dailyRecords)
        }
        
        // Restore Items
        if (backupData.calorieItems.isNotEmpty()) {
            recordDao.insertCalorieItems(backupData.calorieItems)
        }
    }

    fun getAutoBackupTime(): Long {
        val file = File(backupDir, "auto_backup.json")
        return if (file.exists()) file.lastModified() else 0L
    }
}
