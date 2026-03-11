package com.example.calorietracker.util

import com.example.calorietracker.data.UserProfileEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object CalorieUtils {
    
    fun generateId(): String {
        return "${System.currentTimeMillis()}-${UUID.randomUUID().toString().substring(0, 9)}"
    }

    fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun formatDate(dateStr: String): String {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr)
            val outputFormat = SimpleDateFormat("MM月dd日", Locale.CHINESE)
            return outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            return dateStr
        }
    }

    // Activity Multipliers
    val ACTIVITY_MULTIPLIERS = mapOf(
        "sedentary" to 1.2f,
        "light" to 1.375f,
        "moderate" to 1.55f,
        "active" to 1.725f,
        "very_active" to 1.9f
    )

    // Goal Adjustments
    val GOAL_ADJUSTMENTS = mapOf(
        "lose" to -500,
        "maintain" to 0,
        "gain" to 300
    )

    fun calculateBMR(gender: String, weight: Float, height: Float, age: Int): Int {
        return if (gender == "male") {
            (10 * weight + 6.25 * height - 5 * age + 5).toInt()
        } else {
            (10 * weight + 6.25 * height - 5 * age - 161).toInt()
        }
    }

    fun calculateDailyTarget(
        gender: String,
        weight: Float,
        height: Float,
        age: Int,
        activityLevel: String,
        goal: String
    ): Int {
        val bmr = calculateBMR(gender, weight, height, age)
        val multiplier = ACTIVITY_MULTIPLIERS[activityLevel] ?: 1.2f
        val tdee = bmr * multiplier
        val adjustment = GOAL_ADJUSTMENTS[goal] ?: 0
        return (tdee + adjustment).toInt()
    }

    fun parseDuration(notes: String?): Int {
        if (notes == null) return 0
        val regex = "时长[:：]\\s*(\\d+)\\s*分钟".toRegex()
        val match = regex.find(notes)
        if (match != null) {
            return match.groupValues[1].toIntOrNull() ?: 0
        }
        return 0
    }

    fun getEffectiveWeight(
        dateStr: String,
        records: List<com.example.calorietracker.data.DailyRecordEntity>,
        userProfile: UserProfileEntity?
    ): Float {
        // 1. Try to find weight for the specific date
        val record = records.find { it.date == dateStr }
        if (record != null && record.weight != null && record.weight > 0) {
            return record.weight
        }

        // 2. Fallback to most recent previous weight
        // Sort records by date descending, filter those before dateStr
        val previousRecord = records
            .filter { it.date < dateStr && it.weight != null && it.weight > 0 }
            .maxByOrNull { it.date }
            
        if (previousRecord != null) {
            return previousRecord.weight!!
        }

        // 3. Fallback to user profile weight
        return userProfile?.weight ?: 70f
    }
}
