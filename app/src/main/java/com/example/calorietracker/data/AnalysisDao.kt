package com.example.calorietracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM weekly_summaries ORDER BY weekStartDate DESC")
    fun getAllSummaries(): Flow<List<WeeklySummaryEntity>>

    @Query("SELECT * FROM weekly_summaries ORDER BY weekStartDate DESC")
    suspend fun getAllSummariesSync(): List<WeeklySummaryEntity>

    @Query("SELECT * FROM weekly_summaries WHERE weekStartDate = :weekStartDate")
    suspend fun getSummary(weekStartDate: String): WeeklySummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: WeeklySummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummaries(summaries: List<WeeklySummaryEntity>)

    @Query("DELETE FROM weekly_summaries WHERE weekStartDate = :weekStartDate")
    suspend fun deleteSummary(weekStartDate: String)

    @Query("DELETE FROM weekly_summaries")
    suspend fun clearSummaries()

    @Query("SELECT * FROM weekly_summaries WHERE status = 'failed' ORDER BY weekStartDate DESC")
    suspend fun getFailedSummaries(): List<WeeklySummaryEntity>

    @Query("SELECT * FROM weekly_summaries WHERE status = 'pending' ORDER BY weekStartDate DESC")
    suspend fun getPendingSummaries(): List<WeeklySummaryEntity>
}
