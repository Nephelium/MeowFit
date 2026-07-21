package com.example.calorietracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileSync(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateUserProfile(profile: UserProfileEntity)
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM daily_records WHERE date = :date")
    fun getDailyRecord(date: String): Flow<DailyRecordEntity?>

    @Query("SELECT * FROM daily_records WHERE date = :date")
    suspend fun getDailyRecordSync(date: String): DailyRecordEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyRecord(record: DailyRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyRecords(records: List<DailyRecordEntity>)

    @Update
    suspend fun updateDailyRecord(record: DailyRecordEntity)

    @Query("SELECT * FROM calorie_items WHERE date = :date ORDER BY time DESC")
    fun getItemsForDate(date: String): Flow<List<CalorieItemEntity>>

    @Query("SELECT * FROM calorie_items WHERE date = :date ORDER BY time DESC")
    suspend fun getItemsForDateSync(date: String): List<CalorieItemEntity>

    @Query("SELECT * FROM calorie_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): CalorieItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CalorieItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalorieItems(items: List<CalorieItemEntity>)

    @Delete
    suspend fun deleteItem(item: CalorieItemEntity)

    @Query("DELETE FROM calorie_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query(
        """
        UPDATE daily_records SET
            totalIntake = :totalIntake,
            totalBurned = :totalBurned,
            netCalories = :netCalories,
            totalCarbs = :totalCarbs,
            totalProtein = :totalProtein,
            totalFat = :totalFat
        WHERE date = :date
        """
    )
    suspend fun updateCalculatedTotals(
        date: String,
        totalIntake: Int,
        totalBurned: Int,
        netCalories: Int,
        totalCarbs: Int,
        totalProtein: Int,
        totalFat: Int
    )

    @Query("UPDATE daily_records SET totalWater = :amount WHERE date = :date")
    suspend fun updateWaterValue(date: String, amount: Int)

    @Query("UPDATE daily_records SET sleepDuration = :duration WHERE date = :date")
    suspend fun updateSleepValue(date: String, duration: Int)

    @Query("UPDATE daily_records SET weight = :weight WHERE date = :date")
    suspend fun updateWeightValue(date: String, weight: Float?)

    @Query("UPDATE daily_records SET medicationTaken = :taken WHERE date = :date")
    suspend fun updateMedicationTakenValue(date: String, taken: String)
    
    @Query("SELECT * FROM daily_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_records")
    suspend fun getAllRecordsSync(): List<DailyRecordEntity>

    @Query("SELECT * FROM calorie_items ORDER BY date DESC, time DESC")
    fun getAllCalorieItems(): Flow<List<CalorieItemEntity>>

    @Query("SELECT * FROM calorie_items")
    suspend fun getAllCalorieItemsSync(): List<CalorieItemEntity>

    @Query(
        """
        SELECT * FROM calorie_items
        WHERE type = :type AND name LIKE '%' || :keyword || '%'
        ORDER BY date DESC, time DESC
        LIMIT :limit
        """
    )
    suspend fun searchRecentItemsByTypeAndKeyword(
        type: String,
        keyword: String,
        limit: Int
    ): List<CalorieItemEntity>

    @Query(
        """
        SELECT * FROM calorie_items
        WHERE name LIKE '%' || :keyword || '%'
        ORDER BY date DESC, time DESC
        LIMIT :limit
        """
    )
    suspend fun searchItemsByKeyword(
        keyword: String,
        limit: Int
    ): List<CalorieItemEntity>
}
