package com.example.calorietracker.data

import android.content.Context
import androidx.room.withTransaction
import com.example.calorietracker.util.CalorieUtils
import com.example.calorietracker.util.ImageStorageUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.roundToInt

class CalorieRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val userDao = database.userDao()
    private val recordDao = database.recordDao()
    private val aiDao = database.aiDao()
    private val foodTemplateDao = database.foodTemplateDao()

    val userProfile: Flow<UserProfileEntity?> = userDao.getUserProfile()

    fun getAllAiMessages(): Flow<List<AiChatMessageEntity>> = aiDao.getGlobalMessages()

    fun getAiMessagesByWeek(weekStartDate: String): Flow<List<AiChatMessageEntity>> =
        aiDao.getMessagesByWeek(weekStartDate)

    suspend fun getAllAiMessagesSync(): List<AiChatMessageEntity> = aiDao.getAllMessagesSync()

    suspend fun getGlobalAiMessagesSync(): List<AiChatMessageEntity> = aiDao.getGlobalMessagesSync()

    suspend fun addAiMessage(message: AiChatMessageEntity) {
        aiDao.insertMessage(message)
    }

    suspend fun clearAiMessages() {
        aiDao.clearGlobalMessages()
    }

    suspend fun clearAiMessagesByWeek(weekStartDate: String) {
        aiDao.clearMessagesByWeek(weekStartDate)
    }

    fun getDailyRecord(date: String): Flow<DailyRecordEntity?> = recordDao.getDailyRecord(date)
    
    fun getItemsForDate(date: String): Flow<List<CalorieItemEntity>> = recordDao.getItemsForDate(date)

    fun getAllRecords(): Flow<List<DailyRecordEntity>> = recordDao.getAllRecords()

    fun getAllCalorieItems(): Flow<List<CalorieItemEntity>> = recordDao.getAllCalorieItems()

    fun getAllFoodTemplates(): Flow<List<FoodTemplateEntity>> = foodTemplateDao.getAllTemplates()

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        if (profile.id == 1) { // Ensure singleton ID
             // Check if exists to decide update or insert, but REPLACE strategy handles it
             userDao.insertUserProfile(profile)
        } else {
             userDao.insertUserProfile(profile.copy(id = 1))
        }
    }

    suspend fun addRecordItem(item: CalorieItemEntity) {
        addRecordItems(listOf(item))
    }

    suspend fun addRecordItems(items: List<CalorieItemEntity>) {
        if (items.isEmpty()) return
        database.withTransaction {
            val dates = items.map { it.date }.toSet()
            dates.forEach { date -> ensureDailyRecord(date) }
            recordDao.insertCalorieItems(items)
            dates.forEach { date -> updateDailyTotalsInTransaction(date) }
        }
    }

    suspend fun updateItem(item: CalorieItemEntity) {
        var oldImageToDelete: String? = null
        database.withTransaction {
            val previous = recordDao.getItemById(item.id)
            val previousDate = previous?.date
            if (previous?.imageUrl != item.imageUrl) oldImageToDelete = previous?.imageUrl
            ensureDailyRecord(item.date)
            recordDao.insertItem(item)
            buildSet {
                previousDate?.let(::add)
                add(item.date)
            }.forEach { date -> updateDailyTotalsInTransaction(date) }
        }
        ImageStorageUtils.deleteRecordImage(context, oldImageToDelete)
    }

    suspend fun deleteItem(item: CalorieItemEntity) {
        database.withTransaction {
            recordDao.deleteItem(item)
            ensureDailyRecord(item.date)
            updateDailyTotalsInTransaction(item.date)
        }
        ImageStorageUtils.deleteRecordImage(context, item.imageUrl)
    }

    suspend fun updateWater(date: String, amount: Int) {
        database.withTransaction {
            ensureDailyRecord(date)
            recordDao.updateWaterValue(date, amount.coerceAtLeast(0))
        }
    }

    suspend fun updateSleep(date: String, duration: Int) {
        database.withTransaction {
            ensureDailyRecord(date)
            recordDao.updateSleepValue(date, duration.coerceAtLeast(0))
        }
    }

    suspend fun updateWeight(date: String, weight: Float?) {
        val normalizedWeight = weight?.takeIf { it.isFinite() && it > 0f }
        database.withTransaction {
            val exists = recordDao.getDailyRecordSync(date) != null
            if (exists || normalizedWeight != null) {
                ensureDailyRecord(date)
                recordDao.updateWeightValue(date, normalizedWeight)
            }

            // Also update user profile weight if it's today or latest.
            // 读-改-写必须同事务，避免与并发 saveProfile 互相覆盖。
            val profile = userDao.getUserProfile().firstOrNull()
            if (profile != null && date == CalorieUtils.getTodayString() && normalizedWeight != null) {
                 // Recalculate target
                 val currentAge = if (profile.birthDate.isNotBlank()) CalorieUtils.calculateAge(profile.birthDate) else profile.age

                 val newTarget = CalorieUtils.calculateDailyTarget(
                     profile.gender, normalizedWeight, profile.height, currentAge, profile.activityLevel, profile.goal
                 )
                 userDao.insertUserProfile(profile.copy(weight = normalizedWeight, dailyCalorieTarget = newTarget, age = currentAge))
            }
        }
    }

    suspend fun updateExcludedExercises(exercises: String) {
        val profile = userDao.getUserProfile().firstOrNull()
        if (profile != null) {
            userDao.insertUserProfile(profile.copy(excludedExercises = exercises))
        }
    }

    suspend fun updateShowMacros(show: Boolean) {
        val profile = userDao.getUserProfile().firstOrNull()
        if (profile != null) {
            userDao.insertUserProfile(profile.copy(showMacros = show))
        }
    }

    suspend fun updateTodayThemeIndex(index: Int) {
        val profile = userDao.getUserProfile().firstOrNull()
        if (profile != null) {
            userDao.insertUserProfile(profile.copy(selectedTodayThemeIndex = index, hasSelectedTodayTheme = true))
        }
    }

    suspend fun updateMedicationEnabled(enabled: Boolean) {
        val profile = userDao.getUserProfile().firstOrNull()
        if (profile != null) {
            userDao.insertUserProfile(profile.copy(medicationEnabled = enabled))
        }
    }

    suspend fun updateMedications(medications: String, medicationTimes: String = "") {
        val profile = userDao.getUserProfile().firstOrNull()
        if (profile != null) {
            userDao.insertUserProfile(profile.copy(medications = medications, medicationTimes = medicationTimes))
        }
    }

    suspend fun updateMedicationTaken(date: String, taken: String) {
        database.withTransaction {
            ensureDailyRecord(date)
            recordDao.updateMedicationTakenValue(date, taken)
        }
    }

    suspend fun updateWeekStartDay(weekStartDay: Int) {
        val profile = userDao.getUserProfile().firstOrNull()
        if (profile != null) {
            userDao.insertUserProfile(profile.copy(weekStartDay = weekStartDay))
        }
    }

    suspend fun searchRecentItemsByTypeAndPrefix(type: String, prefix: String, limit: Int = 8): List<CalorieItemEntity> {
        val normalizedType = if (type == "exercise") "exercise" else "food"
        val normalizedPrefix = prefix.trim()
        if (normalizedPrefix.isBlank()) return emptyList()
        return recordDao.searchRecentItemsByTypeAndKeyword(normalizedType, normalizedPrefix, limit)
    }

    suspend fun searchItemsByKeyword(keyword: String, limit: Int = 120): List<CalorieItemEntity> {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) return emptyList()
        return recordDao.searchItemsByKeyword(normalizedKeyword, limit)
    }

    suspend fun searchFoodTemplates(keyword: String, limit: Int = 20): List<FoodTemplateEntity> {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) return emptyList()
        return foodTemplateDao.searchTemplates(normalizedKeyword, limit)
    }

    suspend fun saveFoodTemplate(template: FoodTemplateEntity) {
        foodTemplateDao.upsertTemplate(template.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteFoodTemplate(template: FoodTemplateEntity) {
        foodTemplateDao.deleteTemplate(template)
    }

    suspend fun getAllFoodTemplatesSync(): List<FoodTemplateEntity> = foodTemplateDao.getAllTemplatesSync()

    suspend fun recalculateDailyTotals(dates: Collection<String>) {
        database.withTransaction {
            dates.distinct().forEach { date ->
                ensureDailyRecord(date)
                updateDailyTotalsInTransaction(date)
            }
        }
    }

    private suspend fun ensureDailyRecord(date: String) {
        recordDao.insertDailyRecord(DailyRecordEntity(date = date))
    }

    private suspend fun updateDailyTotalsInTransaction(date: String) {
        val items = recordDao.getItemsForDateSync(date)
        val totalIntake = items.filter { it.type == "food" }.sumOf { it.calories }
        val totalBurned = items.filter { it.type == "exercise" }.sumOf { it.calories }
        
        // Calculate Macros
        val totalCarbs = items.filter { it.type == "food" }.sumOf { it.carbs }
        val totalProtein = items.filter { it.type == "food" }.sumOf { it.protein }
        val totalFat = items.filter { it.type == "food" }.sumOf { it.fat }
        
        val net = totalIntake - totalBurned

        recordDao.updateCalculatedTotals(
            date = date,
            totalIntake = totalIntake.roundToInt(),
            totalBurned = totalBurned.roundToInt(),
            netCalories = net.roundToInt(),
            totalCarbs = totalCarbs.roundToInt(),
            totalProtein = totalProtein.roundToInt(),
            totalFat = totalFat.roundToInt()
        )
    }
}
