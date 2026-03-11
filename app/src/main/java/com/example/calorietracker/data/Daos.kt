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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CalorieItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalorieItems(items: List<CalorieItemEntity>)

    @Delete
    suspend fun deleteItem(item: CalorieItemEntity)

    @Query("DELETE FROM calorie_items WHERE id = :id")
    suspend fun deleteItemById(id: String)
    
    @Query("SELECT * FROM daily_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_records")
    suspend fun getAllRecordsSync(): List<DailyRecordEntity>

    @Query("SELECT * FROM calorie_items ORDER BY date DESC, time DESC")
    fun getAllCalorieItems(): Flow<List<CalorieItemEntity>>

    @Query("SELECT * FROM calorie_items")
    suspend fun getAllCalorieItemsSync(): List<CalorieItemEntity>
}
