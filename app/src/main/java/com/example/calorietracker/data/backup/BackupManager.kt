package com.example.calorietracker.data.backup

import android.content.Context
import android.net.Uri
import com.example.calorietracker.data.CalorieItemEntity
import com.example.calorietracker.data.DailyRecordEntity
import com.example.calorietracker.data.RecordDao
import com.example.calorietracker.data.UserDao
import com.example.calorietracker.data.UserProfileEntity
import com.example.calorietracker.util.ImageStorageUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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
    val selectedTodayThemeIndex: Int? = null,
    val hasSelectedTodayTheme: Boolean? = null,
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
            selectedTodayThemeIndex = selectedTodayThemeIndex ?: 0,
            hasSelectedTodayTheme = hasSelectedTodayTheme ?: false,
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
                selectedTodayThemeIndex = entity.selectedTodayThemeIndex,
                hasSelectedTodayTheme = entity.hasSelectedTodayTheme,
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
    val version: Int = 2,
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

    private data class BackupPayload(
        val data: BackupData,
        val imageSources: List<Pair<File, String>>
    )

    private suspend fun getBackupPayload(): BackupPayload {
        return withContext(Dispatchers.IO) {
            val userProfile = userDao.getUserProfileSync()
            val dailyRecords = recordDao.getAllRecordsSync()
            val calorieItems = recordDao.getAllCalorieItemsSync()
            val mappedImages = mutableListOf<Pair<File, String>>()
            var imageIndex = 0

            val remappedItems = calorieItems.map { item ->
                val path = item.imageUrl
                if (path.isNullOrBlank()) {
                    item
                } else {
                    val file = File(path)
                    if (file.exists() && file.isFile) {
                        val entryName = "images/${imageIndex}_${file.name}"
                        imageIndex += 1
                        mappedImages += file to entryName
                        item.copy(imageUrl = entryName)
                    } else {
                        item.copy(imageUrl = null)
                    }
                }
            }

            BackupPayload(
                BackupData(
                    userProfile?.let { BackupUserProfile.fromEntity(it) },
                    dailyRecords,
                    remappedItems
                ),
                mappedImages
            )
        }
    }

    private fun buildBackupZipBytes(payload: BackupPayload): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(gson.toJson(payload.data).toByteArray())
            zip.closeEntry()

            payload.imageSources.forEach { (file, entryName) ->
                zip.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    suspend fun performAutoBackup(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val payload = getBackupPayload()
                val zipBytes = buildBackupZipBytes(payload)
                val file = File(backupDir, "auto_backup.zip")
                FileOutputStream(file).use { it.write(zipBytes) }
                exportBackupToDownloads(zipBytes)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    fun exportBackupToDownloads(zipBytes: ByteArray) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val filename = "MeowFit_Backup_$timestamp.zip"
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/MeowFit")
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }
                
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { os ->
                        os.write(zipBytes)
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
                FileOutputStream(file).use { it.write(zipBytes) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun performManualBackup(targetUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val payload = getBackupPayload()
                val zipBytes = buildBackupZipBytes(payload)
                context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                    outputStream.write(zipBytes)
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
                    val backupData = readBackupDataFromZip(inputStream.readBytes())
                    restoreData(backupData)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun readBackupDataFromZip(zipBytes: ByteArray): BackupData {
        var backupJson = ""
        val imageDir = ImageStorageUtils.getImageDir(context)

        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    if (entry.name == "backup.json") {
                        backupJson = zip.readBytes().toString(Charsets.UTF_8)
                    } else if (entry.name.startsWith("images/")) {
                        val filename = entry.name.removePrefix("images/")
                        if (filename.isNotBlank()) {
                            val target = File(imageDir, filename)
                            FileOutputStream(target).use { output ->
                                zip.copyTo(output)
                            }
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val parsed = gson.fromJson(backupJson, BackupData::class.java)
        val restoredItems = parsed.calorieItems.map { item ->
            val p = item.imageUrl
            if (!p.isNullOrBlank() && p.startsWith("images/")) {
                val filename = p.removePrefix("images/")
                item.copy(imageUrl = File(imageDir, filename).absolutePath)
            } else {
                item
            }
        }
        return parsed.copy(calorieItems = restoredItems)
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
        val file = File(backupDir, "auto_backup.zip")
        return if (file.exists()) file.lastModified() else 0L
    }
}
