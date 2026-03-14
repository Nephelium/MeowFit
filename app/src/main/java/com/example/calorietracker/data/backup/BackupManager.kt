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
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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

    private fun normalizeDate(date: String?): String? {
        val raw = date?.trim().orEmpty()
        if (raw.isBlank()) return null
        val parts = raw.split(Regex("[^0-9]+")).filter { it.isNotBlank() }
        if (parts.size < 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    private fun normalizeTime(time: String?): String {
        val raw = time?.trim().orEmpty()
        if (raw.isBlank()) return ""
        val parts = raw.split(":")
        if (parts.size < 2) return ""
        val hour = parts[0].toIntOrNull() ?: return ""
        val minute = parts[1].toIntOrNull() ?: return ""
        if (hour !in 0..23 || minute !in 0..59) return ""
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    private fun sanitizeBackupData(backupData: BackupData): BackupData {
        val normalizedDailyRecords = backupData.dailyRecords.mapNotNull { record ->
            val date = normalizeDate(runCatching { record.date }.getOrNull()) ?: return@mapNotNull null
            val weight = runCatching { record.weight }.getOrNull()?.takeIf { it.isFinite() && it > 0f }
            DailyRecordEntity(
                date = date,
                weight = weight,
                totalIntake = runCatching { record.totalIntake }.getOrDefault(0).coerceAtLeast(0),
                totalBurned = runCatching { record.totalBurned }.getOrDefault(0).coerceAtLeast(0),
                netCalories = runCatching { record.netCalories }.getOrDefault(0),
                totalCarbs = runCatching { record.totalCarbs }.getOrDefault(0).coerceAtLeast(0),
                totalProtein = runCatching { record.totalProtein }.getOrDefault(0).coerceAtLeast(0),
                totalFat = runCatching { record.totalFat }.getOrDefault(0).coerceAtLeast(0),
                totalWater = runCatching { record.totalWater }.getOrDefault(0).coerceAtLeast(0),
                sleepDuration = runCatching { record.sleepDuration }.getOrDefault(0).coerceAtLeast(0)
            )
        }.associateBy { it.date }.values.toList()

        val normalizedItems = backupData.calorieItems.mapNotNull { item ->
            val date = normalizeDate(runCatching { item.date }.getOrNull()) ?: return@mapNotNull null
            val type = if (runCatching { item.type }.getOrDefault("food") == "exercise") "exercise" else "food"
            val normalizedId = runCatching { item.id }.getOrNull().takeUnless { it.isNullOrBlank() }
                ?: "${System.currentTimeMillis()}-${date.hashCode()}-${type.hashCode()}"
            val normalizedName = runCatching { item.name }.getOrNull().takeUnless { it.isNullOrBlank() }
                ?: if (type == "exercise") "运动" else "食物"
            CalorieItemEntity(
                id = normalizedId,
                date = date,
                type = type,
                name = normalizedName,
                calories = runCatching { item.calories }.getOrDefault(0).coerceAtLeast(0),
                carbs = runCatching { item.carbs }.getOrDefault(0).coerceAtLeast(0),
                protein = runCatching { item.protein }.getOrDefault(0).coerceAtLeast(0),
                fat = runCatching { item.fat }.getOrDefault(0).coerceAtLeast(0),
                time = normalizeTime(runCatching { item.time }.getOrNull()),
                mealCategory = runCatching { item.mealCategory }.getOrNull(),
                imageUrl = runCatching { item.imageUrl }.getOrNull(),
                notes = runCatching { item.notes }.getOrNull(),
                createdAt = runCatching { item.createdAt }.getOrNull().takeUnless { it.isNullOrBlank() }
                    ?: Date().toString()
            )
        }

        val dateSet = normalizedDailyRecords.map { it.date }.toMutableSet()
        normalizedItems.forEach { dateSet += it.date }
        val existingDates = normalizedDailyRecords.map { it.date }.toSet()
        val generatedDailyRecords = dateSet
            .filter { it !in existingDates }
            .map { DailyRecordEntity(date = it) }
        val finalDailyRecords = (normalizedDailyRecords + generatedDailyRecords).sortedBy { it.date }

        return backupData.copy(
            dailyRecords = finalDailyRecords,
            calorieItems = normalizedItems
        )
    }

    private data class BackupPayload(
        val data: BackupData,
        val imageSources: List<Pair<File, String>>
    )

    private fun parseBackupDataSafely(backupJson: String): BackupData {
        if (backupJson.isBlank()) {
            throw IllegalArgumentException("Backup JSON is empty")
        }
        val root = JsonParser.parseString(backupJson)
        if (!root.isJsonObject) {
            throw IllegalArgumentException("Backup JSON root is not object")
        }
        val obj = root.asJsonObject
        val userProfile = parseUserProfile(obj)
        val dailyRecords = parseDailyRecords(obj)
        val calorieItems = parseCalorieItems(obj)
        val version = obj.getSafeInt("version") ?: 1
        val timestamp = obj.getSafeLong("timestamp") ?: System.currentTimeMillis()
        return BackupData(
            userProfile = userProfile,
            dailyRecords = dailyRecords,
            calorieItems = calorieItems,
            version = version,
            timestamp = timestamp
        )
    }

    private fun parseUserProfile(obj: JsonObject): BackupUserProfile? {
        val userElement = obj.get("userProfile") ?: return null
        if (userElement.isJsonNull) return null
        return runCatching { gson.fromJson(userElement, BackupUserProfile::class.java) }.getOrNull()
    }

    private fun parseDailyRecords(obj: JsonObject): List<DailyRecordEntity> {
        return parseArray(obj.get("dailyRecords")).mapNotNull { element ->
            runCatching { gson.fromJson(element, DailyRecordEntity::class.java) }.getOrNull()
        }
    }

    private fun parseCalorieItems(obj: JsonObject): List<CalorieItemEntity> {
        return parseArray(obj.get("calorieItems")).mapNotNull { element ->
            runCatching { gson.fromJson(element, CalorieItemEntity::class.java) }.getOrNull()
        }
    }

    private fun parseArray(element: com.google.gson.JsonElement?): JsonArray {
        return if (element != null && element.isJsonArray) element.asJsonArray else JsonArray()
    }

    private fun JsonObject.getSafeInt(key: String): Int? {
        val value = get(key) ?: return null
        return runCatching { value.asInt }.getOrNull()
    }

    private fun JsonObject.getSafeLong(key: String): Long? {
        val value = get(key) ?: return null
        return runCatching { value.asLong }.getOrNull()
    }

    private fun isSafeImageEntryName(entryName: String): Boolean {
        if (!entryName.startsWith("images/")) return false
        val filename = entryName.removePrefix("images/")
        if (filename.isBlank()) return false
        if (filename.contains("..")) return false
        if (filename.contains("/") || filename.contains("\\")) return false
        return true
    }

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
            zip.write(gson.toJson(payload.data).toByteArray(Charsets.UTF_8))
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
                val outputStream = context.contentResolver.openOutputStream(targetUri) ?: return@withContext false
                outputStream.use {
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
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
                inputStream.use {
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
                    } else if (isSafeImageEntryName(entry.name)) {
                        val filename = entry.name.removePrefix("images/")
                        if (filename.isNotBlank()) {
                            val target = File(imageDir, filename)
                            val canonicalParent = imageDir.canonicalPath + File.separator
                            val canonicalTarget = target.canonicalPath
                            if (!canonicalTarget.startsWith(canonicalParent)) {
                                zip.closeEntry()
                                entry = zip.nextEntry
                                continue
                            }
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

        val parsed = parseBackupDataSafely(backupJson)
        val restoredItems = parsed.calorieItems.map { item ->
            val p = item.imageUrl
            if (!p.isNullOrBlank() && p.startsWith("images/")) {
                val filename = p.removePrefix("images/")
                val imageFile = File(imageDir, filename)
                item.copy(imageUrl = if (imageFile.exists()) imageFile.absolutePath else null)
            } else {
                item
            }
        }
        return parsed.copy(calorieItems = restoredItems)
    }

    private suspend fun restoreData(backupData: BackupData) {
        val sanitized = sanitizeBackupData(backupData)
        // Restore User Profile
        sanitized.userProfile?.let {
            userDao.insertUserProfile(it.toEntity())
        }
        
        // Restore Records
        if (sanitized.dailyRecords.isNotEmpty()) {
            recordDao.insertDailyRecords(sanitized.dailyRecords)
        }
        
        // Restore Items
        if (sanitized.calorieItems.isNotEmpty()) {
            recordDao.insertCalorieItems(sanitized.calorieItems)
        }
    }

    fun getAutoBackupTime(): Long {
        val file = File(backupDir, "auto_backup.zip")
        return if (file.exists()) file.lastModified() else 0L
    }

    private fun clearDirectoryContents(directory: File): Boolean {
        if (!directory.exists()) return true
        val children = directory.listFiles() ?: return true
        var success = true
        children.forEach { child ->
            val deleted = if (child.isDirectory) child.deleteRecursively() else child.delete()
            if (!deleted) {
                success = false
            }
        }
        return success
    }

    suspend fun clearAllCacheFiles(): Boolean {
        return withContext(Dispatchers.IO) {
            val imageDir = ImageStorageUtils.getImageDir(context)
            val cacheDir = context.cacheDir
            val externalCacheDir = context.externalCacheDir
            val backupCleared = clearDirectoryContents(backupDir)
            val imageCleared = clearDirectoryContents(imageDir)
            val cacheCleared = clearDirectoryContents(cacheDir)
            val externalCacheCleared = externalCacheDir?.let { clearDirectoryContents(it) } ?: true
            backupCleared && imageCleared && cacheCleared && externalCacheCleared
        }
    }
}
