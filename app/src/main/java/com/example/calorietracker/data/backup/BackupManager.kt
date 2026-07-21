package com.example.calorietracker.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.calorietracker.data.AiChatMessageEntity
import com.example.calorietracker.data.AppDatabase
import com.example.calorietracker.data.CalorieItemEntity
import com.example.calorietracker.data.DailyRecordEntity
import com.example.calorietracker.data.FoodTemplateEntity
import com.example.calorietracker.data.UserProfileEntity
import com.example.calorietracker.data.WeeklySummaryEntity
import com.example.calorietracker.domain.nutrition.AmountUnit
import com.example.calorietracker.domain.nutrition.EnergyUnit
import com.example.calorietracker.util.AppImageStore
import com.example.calorietracker.util.ImageStorageUtils
import com.example.calorietracker.util.IconManager
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

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
    val weekStartDay: Int? = null,
    val selectedTodayThemeIndex: Int? = null,
    val hasSelectedTodayTheme: Boolean? = null,
    val excludedExercises: String? = null,
    val medicationEnabled: Boolean? = null,
    val medications: String? = null,
    val medicationTimes: String? = null,
    val createdAt: String? = null
) {
    fun toEntity(existing: UserProfileEntity? = null): UserProfileEntity {
        return UserProfileEntity(
            id = 1,
            name = name ?: existing?.name ?: "User",
            gender = gender ?: existing?.gender ?: "male",
            age = age ?: existing?.age ?: 25,
            birthDate = birthDate ?: existing?.birthDate ?: "",
            height = height ?: existing?.height ?: 170f,
            weight = weight ?: existing?.weight ?: 60f,
            targetWeight = targetWeight ?: existing?.targetWeight ?: 55f,
            activityLevel = activityLevel ?: existing?.activityLevel ?: "sedentary",
            goal = goal ?: existing?.goal ?: "lose",
            dailyCalorieTarget = dailyCalorieTarget ?: existing?.dailyCalorieTarget ?: 2000,
            sleepGoal = sleepGoal ?: existing?.sleepGoal ?: 7.5f,
            showMacros = showMacros ?: existing?.showMacros ?: false,
            weekStartDay = when (weekStartDay ?: existing?.weekStartDay) {
                Calendar.MONDAY -> Calendar.MONDAY
                else -> Calendar.SUNDAY
            },
            selectedTodayThemeIndex = selectedTodayThemeIndex ?: existing?.selectedTodayThemeIndex ?: 0,
            hasSelectedTodayTheme = hasSelectedTodayTheme ?: existing?.hasSelectedTodayTheme ?: false,
            excludedExercises = excludedExercises ?: existing?.excludedExercises ?: "",
            medicationEnabled = medicationEnabled ?: existing?.medicationEnabled ?: false,
            medications = medications ?: existing?.medications ?: "",
            medicationTimes = medicationTimes ?: existing?.medicationTimes ?: "",
            createdAt = createdAt ?: existing?.createdAt ?: java.time.Instant.now().toString()
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
                weekStartDay = entity.weekStartDay,
                selectedTodayThemeIndex = entity.selectedTodayThemeIndex,
                hasSelectedTodayTheme = entity.hasSelectedTodayTheme,
                excludedExercises = entity.excludedExercises,
                medicationEnabled = entity.medicationEnabled,
                medications = entity.medications,
                medicationTimes = entity.medicationTimes,
                createdAt = entity.createdAt
            )
        }
    }
}

data class BackupData(
    val userProfile: BackupUserProfile?,
    val dailyRecords: List<DailyRecordEntity>,
    val calorieItems: List<CalorieItemEntity>,
    val weeklySummaries: List<WeeklySummaryEntity> = emptyList(),
    val aiChatMessages: List<AiChatMessageEntity> = emptyList(),
    val foodTemplates: List<FoodTemplateEntity> = emptyList(),
    val customChatPrompt: String? = null,
    val customImagePrompt: String? = null,
    val customAnalysisPrompt: String? = null,
    val version: Int = 6,
    val timestamp: Long = System.currentTimeMillis()
)

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val userDao = database.userDao()
    private val recordDao = database.recordDao()
    private val analysisDao = database.analysisDao()
    private val aiDao = database.aiDao()
    private val foodTemplateDao = database.foodTemplateDao()
    private val gson = Gson()
    private val backupDir = File(context.filesDir, "backups")

    private companion object {
        const val CURRENT_BACKUP_VERSION = 6
        const val MAX_ARCHIVE_BYTES = 100L * 1024L * 1024L
        const val MAX_JSON_BYTES = 10L * 1024L * 1024L
        const val MAX_IMAGE_BYTES = 20L * 1024L * 1024L
    }

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
        return runCatching { java.time.LocalDate.of(year, month, day).toString() }.getOrNull()
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
        fun nonNegativeOrNull(value: Double?): Double? = value?.takeIf { it.isFinite() && it >= 0.0 }
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
                sleepDuration = runCatching { record.sleepDuration }.getOrDefault(0).coerceAtLeast(0),
                // Gson 反序列化旧备份缺失字段时不会抛异常而是留下 null，必须判 null 兜底
                medicationTaken = (runCatching { record.medicationTaken }.getOrNull()) ?: ""
            )
        }.associateBy { it.date }.values.toList()

        val normalizedItems = backupData.calorieItems.mapNotNull { item ->
            val date = normalizeDate(runCatching { item.date }.getOrNull()) ?: return@mapNotNull null
            val type = if (runCatching { item.type }.getOrDefault("food") == "exercise") "exercise" else "food"
            val normalizedId = runCatching { item.id }.getOrNull().takeUnless { it.isNullOrBlank() }
                ?: UUID.randomUUID().toString()
            val normalizedName = runCatching { item.name }.getOrNull().takeUnless { it.isNullOrBlank() }
                ?: if (type == "exercise") "运动" else "食物"
            val calories = runCatching { item.calories }.getOrDefault(0.0).let { if (it.isFinite()) it else 0.0 }.coerceAtLeast(0.0)
            val carbs = runCatching { item.carbs }.getOrDefault(0.0).let { if (it.isFinite()) it else 0.0 }.coerceAtLeast(0.0)
            val protein = runCatching { item.protein }.getOrDefault(0.0).let { if (it.isFinite()) it else 0.0 }.coerceAtLeast(0.0)
            val fat = runCatching { item.fat }.getOrDefault(0.0).let { if (it.isFinite()) it else 0.0 }.coerceAtLeast(0.0)
            CalorieItemEntity(
                id = normalizedId,
                date = date,
                type = type,
                name = normalizedName,
                calories = calories,
                carbs = carbs,
                protein = protein,
                fat = fat,
                time = normalizeTime(runCatching { item.time }.getOrNull()),
                mealCategory = runCatching { item.mealCategory }.getOrNull(),
                imageUrl = runCatching { item.imageUrl }.getOrNull(),
                notes = runCatching { item.notes }.getOrNull(),
                nutritionReferenceAmount = nonNegativeOrNull(runCatching { item.nutritionReferenceAmount }.getOrNull())
                    ?.takeIf { it > 0.0 },
                nutritionActualAmount = nonNegativeOrNull(runCatching { item.nutritionActualAmount }.getOrNull()),
                nutritionAmountUnit = runCatching { item.nutritionAmountUnit }.getOrNull()
                    ?.let { AmountUnit.fromStorage(it).name },
                nutritionReferenceEnergy = nonNegativeOrNull(runCatching { item.nutritionReferenceEnergy }.getOrNull()),
                nutritionEnergyUnit = runCatching { item.nutritionEnergyUnit }.getOrNull()
                    ?.let { EnergyUnit.fromStorage(it).name },
                nutritionReferenceCarbs = nonNegativeOrNull(runCatching { item.nutritionReferenceCarbs }.getOrNull()),
                nutritionReferenceProtein = nonNegativeOrNull(runCatching { item.nutritionReferenceProtein }.getOrNull()),
                nutritionReferenceFat = nonNegativeOrNull(runCatching { item.nutritionReferenceFat }.getOrNull()),
                createdAt = runCatching { item.createdAt }.getOrNull().takeUnless { it.isNullOrBlank() }
                    ?: Date().toString()
            )
        }

        // 周报：旧备份可能缺失非空字段（summaryText/status 等），逐字段兜底而非整条丢弃
        val normalizedSummaries = backupData.weeklySummaries.mapNotNull { summary ->
            val weekStart = normalizeDate(runCatching { summary.weekStartDate }.getOrNull())
                ?: return@mapNotNull null
            val weekEnd = (runCatching { summary.weekEndDate }.getOrNull())?.let(::normalizeDate) ?: weekStart
            WeeklySummaryEntity(
                weekStartDate = weekStart,
                weekEndDate = weekEnd,
                summaryText = (runCatching { summary.summaryText }.getOrNull()) ?: "",
                recommendations = (runCatching { summary.recommendations }.getOrNull()) ?: "",
                dietDays = runCatching { summary.dietDays }.getOrDefault(0).coerceAtLeast(0),
                exerciseDays = runCatching { summary.exerciseDays }.getOrDefault(0).coerceAtLeast(0),
                generatedAt = runCatching { summary.generatedAt }.getOrDefault(System.currentTimeMillis()),
                status = (runCatching { summary.status }.getOrNull())?.takeIf { it.isNotBlank() } ?: "generated"
            )
        }

        val normalizedTemplates = backupData.foodTemplates.mapNotNull { template ->
            val name = runCatching { template.name }.getOrNull()?.trim().takeUnless { it.isNullOrBlank() }
                ?: return@mapNotNull null
            val referenceAmount = runCatching { template.referenceAmount }.getOrDefault(0.0)
            val energyValue = runCatching { template.energyValue }.getOrDefault(0.0)
            if (!referenceAmount.isFinite() || referenceAmount <= 0.0 || !energyValue.isFinite() || energyValue < 0.0) {
                return@mapNotNull null
            }
            template.copy(
                id = runCatching { template.id }.getOrNull().takeUnless { it.isNullOrBlank() }
                    ?: UUID.randomUUID().toString(),
                name = name,
                referenceAmount = referenceAmount,
                amountUnit = AmountUnit.fromStorage(template.amountUnit).name,
                energyValue = energyValue,
                energyUnit = EnergyUnit.fromStorage(template.energyUnit).name,
                carbs = template.carbs.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                protein = template.protein.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                fat = template.fat.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
            )
        }

        val dateSet = normalizedDailyRecords.map { it.date }.toMutableSet()
        normalizedItems.forEach { dateSet += it.date }
        val existingDates = normalizedDailyRecords.map { it.date }.toSet()
        val generatedDailyRecords = dateSet
            .filter { it !in existingDates }
            .map { DailyRecordEntity(date = it) }
        val finalDailyRecords = (normalizedDailyRecords + generatedDailyRecords).sortedBy { it.date }

        val normalizedMessages = backupData.aiChatMessages.mapNotNull { message ->
            val role = runCatching { message.role }.getOrNull()
                ?.takeIf { it == "user" || it == "assistant" } ?: return@mapNotNull null
            val content = runCatching { message.content }.getOrNull()
                ?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val originalWeek = runCatching { message.weekStartDate }.getOrNull()
            val normalizedWeek = originalWeek?.let(::normalizeDate)
            if (originalWeek != null && normalizedWeek == null) return@mapNotNull null
            message.copy(role = role, content = content, weekStartDate = normalizedWeek)
        }

        return backupData.copy(
            dailyRecords = finalDailyRecords,
            calorieItems = normalizedItems,
            weeklySummaries = normalizedSummaries,
            aiChatMessages = normalizedMessages,
            foodTemplates = normalizedTemplates
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
        val weeklySummaries = parseWeeklySummaries(obj)
        val foodTemplates = parseFoodTemplates(obj)
        val version = obj.getSafeInt("version") ?: 1
        val timestamp = obj.getSafeLong("timestamp") ?: System.currentTimeMillis()
        val customChatPrompt = obj.getSafeString("customChatPrompt")
        val customImagePrompt = obj.getSafeString("customImagePrompt")
        val customAnalysisPrompt = obj.getSafeString("customAnalysisPrompt")
        val aiChatMessages = parseAiChatMessages(obj)
        return BackupData(
            userProfile = userProfile,
            dailyRecords = dailyRecords,
            calorieItems = calorieItems,
            weeklySummaries = weeklySummaries,
            aiChatMessages = aiChatMessages,
            foodTemplates = foodTemplates,
            customChatPrompt = customChatPrompt,
            customImagePrompt = customImagePrompt,
            customAnalysisPrompt = customAnalysisPrompt,
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

    private fun parseWeeklySummaries(obj: JsonObject): List<WeeklySummaryEntity> {
        return parseArray(obj.get("weeklySummaries")).mapNotNull { element ->
            runCatching { gson.fromJson(element, WeeklySummaryEntity::class.java) }.getOrNull()
        }
    }

    private fun parseAiChatMessages(obj: JsonObject): List<AiChatMessageEntity> {
        return parseArray(obj.get("aiChatMessages")).mapNotNull { element ->
            runCatching { gson.fromJson(element, AiChatMessageEntity::class.java) }.getOrNull()
        }
    }

    private fun parseFoodTemplates(obj: JsonObject): List<FoodTemplateEntity> {
        return parseArray(obj.get("foodTemplates")).mapNotNull { element ->
            runCatching { gson.fromJson(element, FoodTemplateEntity::class.java) }.getOrNull()
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

    private fun JsonObject.getSafeString(key: String): String? {
        val value = get(key) ?: return null
        if (value.isJsonNull || !value.isJsonPrimitive) return null
        return runCatching { value.asString }.getOrNull()
    }

    private fun isSafeImageEntryName(entryName: String): Boolean {
        if (!entryName.startsWith("images/")) return false
        val relative = entryName.removePrefix("images/")
        if (relative.isBlank() || relative.contains("..") || relative.contains("\\")) return false
        val parts = relative.split('/')
        return when (parts.size) {
            1 -> parts[0].isNotBlank() // v1-v5 flat image entries
            2 -> parts[0] in setOf("records", "chat") && parts[1].isNotBlank()
            else -> false
        }
    }

    private fun fileFromStoredUri(value: String): File? = runCatching {
        val uri = Uri.parse(value)
        when (uri.scheme) {
            "file" -> uri.path?.let(::File)
            null -> File(value)
            else -> null
        }
    }.getOrNull()

    private fun safeArchiveFilename(file: File): String =
        file.name.replace(Regex("[^A-Za-z0-9._-]"), "_").takeLast(100).ifBlank { "image.jpg" }

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
                    if (file.exists() && file.isFile && file.length() in 1..MAX_IMAGE_BYTES) {
                        val entryName = "images/records/${imageIndex}_${safeArchiveFilename(file)}"
                        imageIndex += 1
                        mappedImages += file to entryName
                        item.copy(imageUrl = entryName)
                    } else {
                        item.copy(imageUrl = null)
                    }
                }
            }

            // Include custom app icon in backup if exists
            val customIconFile = IconManager.getCustomIconFile(context)
            if (customIconFile.exists() && customIconFile.isFile && customIconFile.length() in 1..MAX_IMAGE_BYTES) {
                mappedImages += customIconFile to "custom_app_icon.png"
            }

            val aiChatMessages = aiDao.getAllMessagesSync().map { message ->
                val remapped = message.imageUrl
                    ?.split('|')
                    ?.mapNotNull { storedValue ->
                        val file = fileFromStoredUri(storedValue)
                        if (file?.exists() == true && file.isFile && file.length() in 1..MAX_IMAGE_BYTES) {
                            val entryName = "images/chat/${imageIndex}_${safeArchiveFilename(file)}"
                            imageIndex += 1
                            mappedImages += file to entryName
                            entryName
                        } else {
                            null
                        }
                    }
                    .orEmpty()
                message.copy(imageUrl = remapped.takeIf { it.isNotEmpty() }?.joinToString("|"))
            }

            val weeklySummaries = analysisDao.getAllSummariesSync()
            val foodTemplates = foodTemplateDao.getAllTemplatesSync()

            // Read custom system prompts from SharedPreferences for backup
            val aiPrefs = context.getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
            val customChatPrompt = aiPrefs.getString("custom_chat_prompt", null)
            val customImagePrompt = aiPrefs.getString("custom_image_prompt", null)
            val customAnalysisPrompt = aiPrefs.getString("custom_analysis_prompt", null)

            BackupPayload(
                BackupData(
                    userProfile?.let { BackupUserProfile.fromEntity(it) },
                    dailyRecords,
                    remappedItems,
                    weeklySummaries,
                    aiChatMessages = aiChatMessages,
                    foodTemplates = foodTemplates,
                    customChatPrompt = customChatPrompt,
                    customImagePrompt = customImagePrompt,
                    customAnalysisPrompt = customAnalysisPrompt,
                    version = CURRENT_BACKUP_VERSION
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
                require(output.size().toLong() <= MAX_ARCHIVE_BYTES) { "备份文件过大" }
            }
        }
        return output.toByteArray().also {
            require(it.size.toLong() <= MAX_ARCHIVE_BYTES) { "备份文件过大" }
        }
    }

    suspend fun performAutoBackup(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val payload = getBackupPayload()
                val zipBytes = buildBackupZipBytes(payload)
                val file = File(backupDir, "auto_backup.zip")
                FileOutputStream(file).use { it.write(zipBytes) }
                val exported = exportBackupToDownloads(zipBytes)
                if (!exported) file.setLastModified(0L) // Keep the local recovery copy but retry export on next launch.
                exported
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    fun exportBackupToDownloads(zipBytes: ByteArray): Boolean {
        return try {
            // Fixed filename so auto-backups always overwrite the previous one
            val filename = "MeowFit_AutoBackup.zip"
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val relativePath = android.os.Environment.DIRECTORY_DOWNLOADS + "/MeowFit"

                // Query existing auto-backup to overwrite instead of creating new
                val existingUri: android.net.Uri? = try {
                    resolver.query(
                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        arrayOf(android.provider.MediaStore.MediaColumns._ID),
                        "${android.provider.MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${android.provider.MediaStore.MediaColumns.RELATIVE_PATH}=?",
                        arrayOf(filename, relativePath),
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(0)
                            android.content.ContentUris.withAppendedId(
                                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, id
                            )
                        } else null
                    }
                } catch (_: Exception) { null }

                val uri: android.net.Uri?
                if (existingUri != null) {
                    // Overwrite existing file
                    uri = existingUri
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    resolver.update(uri, contentValues, null, null)
                } else {
                    // Create new entry
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                }

                val outputUri = uri ?: return false
                val written = try {
                    resolver.openOutputStream(outputUri, "wt")?.use { os ->
                        os.write(zipBytes)
                    } != null
                } catch (e: Exception) {
                    false
                }
                if (!written) {
                    // 写入失败：删除仍处于 IS_PENDING=1 的条目，避免留下孤儿
                    runCatching { resolver.delete(outputUri, null, null) }
                    return false
                }
                val finishValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(outputUri, finishValues, null, null)
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadsDir, "MeowFit")
                if (!appDir.exists()) appDir.mkdirs()
                val file = File(appDir, filename)
                FileOutputStream(file).use { it.write(zipBytes) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun performManualBackup(targetUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val payload = getBackupPayload()
                val zipBytes = buildBackupZipBytes(payload)
                // "wt" 模式截断目标文件，防止旧文件更长时残留尾部字节
                val outputStream = context.contentResolver.openOutputStream(targetUri, "wt") ?: return@withContext false
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

    /** 恢复过程中待落盘的图片/图标文件：事务成功前都留在 restore_tmp 临时目录。 */
    private data class PendingRestoreFiles(
        val moves: List<Pair<File, File>>, // 临时文件 -> 正式文件
        val tmpDirs: List<File>
    ) {
        /** 事务成功后调用：同分区 rename 是原子的，rename 失败退化为复制。 */
        fun commit() {
            moves.forEach { (tmp, target) ->
                runCatching {
                    if (!tmp.exists()) return@forEach
                    if (target.exists()) target.delete()
                    if (!tmp.renameTo(target)) {
                        tmp.copyTo(target, overwrite = true)
                        tmp.delete()
                    }
                }
            }
            tmpDirs.forEach { it.deleteRecursively() }
        }

        /** 事务失败时调用：删除临时目录，避免孤儿文件。 */
        fun cleanup() {
            tmpDirs.forEach { it.deleteRecursively() }
        }
    }

    private data class RestoredBackup(
        val data: BackupData,
        val pendingFiles: PendingRestoreFiles
    )

    suspend fun restoreFromUri(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            var pendingRestoreFiles: PendingRestoreFiles? = null
            try {
                // 恢复前留一份当前状态；即使这一步失败，数据库事务仍会保护原数据。
                try {
                    val recoveryBytes = buildBackupZipBytes(getBackupPayload())
                    FileOutputStream(File(backupDir, "recovery_before_restore.zip")).use { it.write(recoveryBytes) }
                } catch (_: Exception) {
                    // Continue: restore itself remains transactional.
                }

                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
                val zipBytes = inputStream.use { readAllLimited(it, MAX_ARCHIVE_BYTES) }
                val restored = readBackupDataFromZip(zipBytes)
                pendingRestoreFiles = restored.pendingFiles
                restoreData(restored.data)
                // 数据库事务成功后才把图片/图标从临时目录原子移动到正式位置
                restored.pendingFiles.commit()
                pendingRestoreFiles = null
                true
            } catch (e: Exception) {
                e.printStackTrace()
                pendingRestoreFiles?.cleanup()
                false
            }
        }
    }

    private fun readAllLimited(input: InputStream, maxBytes: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "备份压缩包过大" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun readZipEntryLimited(zip: ZipInputStream, maxBytes: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = zip.read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "备份条目过大" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun readBackupDataFromZip(zipBytes: ByteArray): RestoredBackup {
        var backupJson = ""
        var customIconBytes: ByteArray? = null
        val imageEntries = linkedMapOf<String, ByteArray>()
        var totalExtracted = 0L

        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    if (entry.name == "backup.json") {
                        val bytes = readZipEntryLimited(zip, MAX_JSON_BYTES)
                        backupJson = bytes.toString(Charsets.UTF_8)
                        totalExtracted += bytes.size
                    } else if (entry.name == "custom_app_icon.png") {
                        customIconBytes = readZipEntryLimited(zip, MAX_IMAGE_BYTES)
                        totalExtracted += customIconBytes?.size ?: 0
                    } else if (isSafeImageEntryName(entry.name)) {
                        require(entry.name !in imageEntries) { "备份中存在重复图片条目" }
                        val bytes = readZipEntryLimited(zip, MAX_IMAGE_BYTES)
                        imageEntries[entry.name] = bytes
                        totalExtracted += bytes.size
                    }
                    require(totalExtracted <= MAX_ARCHIVE_BYTES) { "备份解压内容过大" }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val parsed = parseBackupDataSafely(backupJson)
        val imageDir = ImageStorageUtils.getImageDir(context)
        val chatDir = AppImageStore.chatDirectory(context)
        val iconFile = IconManager.getCustomIconFile(context)

        // 文件先写入同目录下的 restore_tmp 临时目录，DB 事务成功后再原子移动
        val imageTmpDir = File(imageDir, "restore_tmp")
        val chatTmpDir = File(chatDir, "restore_tmp")
        val iconTmpDir = File(iconFile.parentFile ?: context.filesDir, "restore_tmp")
        val tmpDirs = listOf(imageTmpDir, chatTmpDir, iconTmpDir)
        tmpDirs.forEach {
            it.deleteRecursively()
            it.mkdirs()
        }
        val moves = mutableListOf<Pair<File, File>>()

        // 写入临时文件，返回正式位置的绝对路径（存库用），并登记待移动映射
        fun restoreEntry(entryName: String, targetDir: File, tmpDir: File): String? {
            val bytes = imageEntries[entryName] ?: return null
            val filename = entryName.substringAfterLast('/')
            if (filename.isBlank()) return null
            val tmpTarget = File(tmpDir, filename)
            val target = File(targetDir, filename)
            val canonicalTmp = tmpDir.canonicalPath + File.separator
            require(tmpTarget.canonicalPath.startsWith(canonicalTmp)) { "非法图片路径" }
            val canonicalParent = targetDir.canonicalPath + File.separator
            require(target.canonicalPath.startsWith(canonicalParent)) { "非法图片路径" }
            FileOutputStream(tmpTarget).use { it.write(bytes) }
            moves += tmpTarget to target
            return target.absolutePath
        }

        val restoredItems = parsed.calorieItems.map { item ->
            val p = item.imageUrl
            if (!p.isNullOrBlank() && p.startsWith("images/")) {
                item.copy(imageUrl = restoreEntry(p, imageDir, imageTmpDir))
            } else if (!p.isNullOrBlank()) {
                // Absolute paths from another installation are not portable or trustworthy.
                item.copy(imageUrl = null)
            } else {
                item
            }
        }

        val restoredMessages = parsed.aiChatMessages.map { message ->
            val restoredUrls = message.imageUrl
                ?.split('|')
                ?.mapNotNull { entryName ->
                    if (entryName.startsWith("images/chat/")) {
                        restoreEntry(entryName, chatDir, chatTmpDir)?.let { Uri.fromFile(File(it)).toString() }
                    } else {
                        null
                    }
                }
                .orEmpty()
            message.copy(imageUrl = restoredUrls.takeIf { it.isNotEmpty() }?.joinToString("|"))
        }

        customIconBytes?.let { bytes ->
            val tmpIcon = File(iconTmpDir, iconFile.name)
            FileOutputStream(tmpIcon).use { it.write(bytes) }
            moves += tmpIcon to iconFile
        }
        return RestoredBackup(
            parsed.copy(calorieItems = restoredItems, aiChatMessages = restoredMessages),
            PendingRestoreFiles(moves, tmpDirs)
        )
    }

    private suspend fun restoreData(backupData: BackupData) {
        val sanitized = sanitizeBackupData(backupData)
        database.withTransaction {
            sanitized.userProfile?.let { profile ->
                userDao.insertUserProfile(profile.toEntity(userDao.getUserProfileSync()))
            }

            sanitized.dailyRecords.forEach { record ->
                val existing = recordDao.getDailyRecordSync(record.date)
                if (existing == null) {
                    recordDao.insertDailyRecord(record)
                } else {
                    // 不 REPLACE 父表：SQLite REPLACE 会触发级联删除，导致现有条目消失。
                    recordDao.updateWeightValue(record.date, record.weight)
                    recordDao.updateWaterValue(record.date, record.totalWater)
                    recordDao.updateSleepValue(record.date, record.sleepDuration)
                    recordDao.updateMedicationTakenValue(record.date, record.medicationTaken)
                }
            }

            if (sanitized.calorieItems.isNotEmpty()) {
                val affectedDates = sanitized.calorieItems.mapTo(mutableSetOf()) { it.date }
                sanitized.calorieItems.forEach { incoming ->
                    recordDao.getItemById(incoming.id)?.date?.let(affectedDates::add)
                }
                recordDao.insertCalorieItems(sanitized.calorieItems)
                recalculateDates(affectedDates)
            }
            if (sanitized.weeklySummaries.isNotEmpty()) analysisDao.insertSummaries(sanitized.weeklySummaries)
            sanitized.aiChatMessages.forEach { message ->
                // 幂等恢复：按 timestamp+role+content 判重，避免重复恢复时聊天记录翻倍
                if (!aiDao.messageExists(message.timestamp, message.role, message.content)) {
                    aiDao.insertMessage(message.copy(id = 0))
                }
            }
            if (sanitized.foodTemplates.isNotEmpty()) foodTemplateDao.upsertTemplates(sanitized.foodTemplates)
        }

        val aiPrefs = context.getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
        val editor = aiPrefs.edit()
        sanitized.customChatPrompt?.let { editor.putString("custom_chat_prompt", it) }
        sanitized.customImagePrompt?.let { editor.putString("custom_image_prompt", it) }
        sanitized.customAnalysisPrompt?.let { editor.putString("custom_analysis_prompt", it) }
        editor.apply()
    }

    private suspend fun recalculateDates(dates: Set<String>) {
        dates.forEach { date ->
            val items = recordDao.getItemsForDateSync(date)
            val foodItems = items.filter { it.type == "food" }
            val exerciseItems = items.filter { it.type == "exercise" }
            val intake = foodItems.sumOf { it.calories }.roundToInt()
            val burned = exerciseItems.sumOf { it.calories }.roundToInt()
            recordDao.updateCalculatedTotals(
                date = date,
                totalIntake = intake,
                totalBurned = burned,
                netCalories = intake - burned,
                totalCarbs = foodItems.sumOf { it.carbs }.roundToInt(),
                totalProtein = foodItems.sumOf { it.protein }.roundToInt(),
                totalFat = foodItems.sumOf { it.fat }.roundToInt()
            )
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
