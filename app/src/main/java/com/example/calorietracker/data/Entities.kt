package com.example.calorietracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1, // Singleton, always 1
    val name: String,
    val gender: String, // "male", "female"
    val age: Int,
    val height: Float, // cm
    val weight: Float, // kg
    val targetWeight: Float, // kg
    val activityLevel: String,
    val goal: String,
    val dailyCalorieTarget: Int,
    val sleepGoal: Float = 7.5f, // Hours, default 7.5
    val createdAt: String
)

@Entity(tableName = "daily_records")
data class DailyRecordEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val weight: Float? = null,
    val totalIntake: Int = 0,
    val totalBurned: Int = 0,
    val netCalories: Int = 0,
    val totalWater: Int = 0, // ml
    val sleepDuration: Int = 0 // minutes
)

@Entity(
    tableName = "calorie_items",
    foreignKeys = [
        ForeignKey(
            entity = DailyRecordEntity::class,
            parentColumns = ["date"],
            childColumns = ["date"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("date")]
)
data class CalorieItemEntity(
    @PrimaryKey val id: String,
    val date: String, // Foreign key to DailyRecordEntity
    val type: String, // "food", "exercise"
    val name: String,
    val calories: Int,
    val time: String,
    val imageUrl: String? = null,
    val notes: String? = null,
    val createdAt: String
)
