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
    val birthDate: String = "", // YYYY-MM-DD
    val height: Float, // cm
    val weight: Float, // kg
    val targetWeight: Float, // kg
    val activityLevel: String,
    val goal: String,
    val dailyCalorieTarget: Int,
    val sleepGoal: Float = 7.5f, // Hours, default 7.5
    val showMacros: Boolean = false, // Whether to show carbs/protein/fat
    val selectedTodayThemeIndex: Int = 0, // Selected Today page background theme
    val hasSelectedTodayTheme: Boolean = false, // Whether user has explicitly selected a theme
    val excludedExercises: String = "", // Comma-separated list of excluded exercises
    val createdAt: String
)

@Entity(tableName = "daily_records")
data class DailyRecordEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val weight: Float? = null,
    val totalIntake: Int = 0,
    val totalBurned: Int = 0,
    val netCalories: Int = 0,
    val totalCarbs: Int = 0, // grams
    val totalProtein: Int = 0, // grams
    val totalFat: Int = 0, // grams
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
    val carbs: Int = 0, // grams
    val protein: Int = 0, // grams
    val fat: Int = 0, // grams
    val time: String,
    val imageUrl: String? = null,
    val notes: String? = null,
    val createdAt: String
)
