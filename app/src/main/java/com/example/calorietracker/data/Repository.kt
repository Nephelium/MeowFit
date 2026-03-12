package com.example.calorietracker.data

import com.example.calorietracker.util.CalorieUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class CalorieRepository(private val userDao: UserDao, private val recordDao: RecordDao, private val aiDao: AiDao) {

    val userProfile: Flow<UserProfileEntity?> = userDao.getUserProfile()

    fun getAllAiMessages(): Flow<List<AiChatMessageEntity>> = aiDao.getAllMessages()

    suspend fun addAiMessage(message: AiChatMessageEntity) {
        aiDao.insertMessage(message)
    }

    suspend fun clearAiMessages() {
        aiDao.clearMessages()
    }

    fun getDailyRecord(date: String): Flow<DailyRecordEntity?> = recordDao.getDailyRecord(date)
    
    fun getItemsForDate(date: String): Flow<List<CalorieItemEntity>> = recordDao.getItemsForDate(date)

    fun getAllRecords(): Flow<List<DailyRecordEntity>> = recordDao.getAllRecords()

    fun getAllCalorieItems(): Flow<List<CalorieItemEntity>> = recordDao.getAllCalorieItems()

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        if (profile.id == 1) { // Ensure singleton ID
             // Check if exists to decide update or insert, but REPLACE strategy handles it
             userDao.insertUserProfile(profile)
        } else {
             userDao.insertUserProfile(profile.copy(id = 1))
        }
    }

    suspend fun addRecordItem(item: CalorieItemEntity) {
        // 1. Ensure DailyRecord exists
        var record = recordDao.getDailyRecordSync(item.date)
        if (record == null) {
            record = DailyRecordEntity(date = item.date)
            recordDao.insertDailyRecord(record)
        }

        // 2. Insert Item
        recordDao.insertItem(item)

        // 3. Update Totals
        updateDailyTotals(item.date)
    }

    suspend fun updateItem(item: CalorieItemEntity) {
        recordDao.insertItem(item)
        updateDailyTotals(item.date)
    }

    suspend fun deleteItem(item: CalorieItemEntity) {
        recordDao.deleteItem(item)
        updateDailyTotals(item.date)
    }

    suspend fun updateWater(date: String, amount: Int) {
        var record = recordDao.getDailyRecordSync(date)
        if (record == null) {
            record = DailyRecordEntity(date = date, totalWater = amount)
            recordDao.insertDailyRecord(record)
        } else {
            recordDao.updateDailyRecord(record.copy(totalWater = amount))
        }
    }

    suspend fun updateSleep(date: String, duration: Int) {
        var record = recordDao.getDailyRecordSync(date)
        if (record == null) {
            record = DailyRecordEntity(date = date, sleepDuration = duration)
            recordDao.insertDailyRecord(record)
        } else {
            recordDao.updateDailyRecord(record.copy(sleepDuration = duration))
        }
    }
    suspend fun updateWeight(date: String, weight: Float) {
        var record = recordDao.getDailyRecordSync(date)
        if (record == null) {
            record = DailyRecordEntity(date = date, weight = weight)
            recordDao.insertDailyRecord(record)
        } else {
            recordDao.updateDailyRecord(record.copy(weight = weight))
        }
        
        // Also update user profile weight if it's today or latest
        val profile = userDao.getUserProfile().firstOrNull()
        if (profile != null && date == CalorieUtils.getTodayString()) {
             // Recalculate target
             val newTarget = CalorieUtils.calculateDailyTarget(
                 profile.gender, weight, profile.height, profile.age, profile.activityLevel, profile.goal
             )
             userDao.insertUserProfile(profile.copy(weight = weight, dailyCalorieTarget = newTarget))
        }
    }

    private suspend fun updateDailyTotals(date: String) {
        val items = recordDao.getItemsForDate(date).first()
        val totalIntake = items.filter { it.type == "food" }.sumOf { it.calories }
        val totalBurned = items.filter { it.type == "exercise" }.sumOf { it.calories }
        val net = totalIntake - totalBurned

        val record = recordDao.getDailyRecordSync(date)
        if (record != null) {
            recordDao.updateDailyRecord(record.copy(
                totalIntake = totalIntake,
                totalBurned = totalBurned,
                netCalories = net
            ))
        }
    }
}
