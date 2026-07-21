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
    val weekStartDay: Int = java.util.Calendar.SUNDAY, // Calendar.SUNDAY or Calendar.MONDAY
    val selectedTodayThemeIndex: Int = 0, // Selected Today page background theme
    val hasSelectedTodayTheme: Boolean = false, // Whether user has explicitly selected a theme
    val excludedExercises: String = "", // Comma-separated list of excluded exercises
    val medicationEnabled: Boolean = false, // Whether medication tracking is enabled
    val medications: String = "", // Comma-separated medication names
    val medicationTimes: String = "", // Comma-separated medication times (e.g. "08:00,12:00,20:00")
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
    val sleepDuration: Int = 0, // minutes
    val medicationTaken: String = "" // comma-separated "1,0,1" for each medication
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
    val calories: Double,
    val carbs: Double = 0.0, // grams
    val protein: Double = 0.0, // grams
    val fat: Double = 0.0, // grams
    val time: String,
    val mealCategory: String? = null,
    val imageUrl: String? = null,
    val notes: String? = null,
    val nutritionReferenceAmount: Double? = null,
    val nutritionActualAmount: Double? = null,
    val nutritionAmountUnit: String? = null,
    val nutritionReferenceEnergy: Double? = null,
    val nutritionEnergyUnit: String? = null,
    val nutritionReferenceCarbs: Double? = null,
    val nutritionReferenceProtein: Double? = null,
    val nutritionReferenceFat: Double? = null,
    val createdAt: String
)

@Entity(tableName = "weekly_summaries")
data class WeeklySummaryEntity(
    @PrimaryKey val weekStartDate: String, // YYYY-MM-DD (Monday)
    val weekEndDate: String,               // YYYY-MM-DD (Sunday)
    val summaryText: String,               // AI analysis text
    val recommendations: String,           // Menu + exercise recommendations
    val dietDays: Int,                     // Days with food records
    val exerciseDays: Int,                 // Days with exercise records
    val generatedAt: Long,                 // Timestamp
    val status: String                     // "pending", "generated", "failed"
)
