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

    fun calculateAge(birthDateStr: String): Int {
        if (birthDateStr.isBlank()) return 0
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDate = sdf.parse(birthDateStr) ?: return 0
            val today = java.util.Calendar.getInstance()
            val dob = java.util.Calendar.getInstance().apply { time = birthDate }
            
            var age = today.get(java.util.Calendar.YEAR) - dob.get(java.util.Calendar.YEAR)
            
            if (today.get(java.util.Calendar.DAY_OF_YEAR) < dob.get(java.util.Calendar.DAY_OF_YEAR)) {
                age--
            }
            
            return age
        } catch (e: Exception) {
            return 0
        }
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

    // Calculate Macro Targets (Carbs, Protein, Fat) in grams
    // Returns Triple(Carbs, Protein, Fat)
    fun calculateMacroTargets(
        weight: Float, // kg
        goal: String, // "loss" (减重), "maintain" (保持), "gain" (增重)
        dailyCalorieTarget: Int
    ): Triple<Int, Int, Int> {
        // Strategy: Protein & Fat based on body weight, Carbs is remainder
        // Protein: Loss=2.0, Maintain=1.5, Gain=1.8 (g/kg)
        // Fat: Loss=0.8, Maintain=1.0, Gain=1.0 (g/kg)
        
        val proteinPerKg = when(goal) {
            "loss", "减重" -> 2.0f
            "gain", "增重" -> 1.8f
            else -> 1.5f
        }
        
        val fatPerKg = when(goal) {
            "loss", "减重" -> 0.8f
            else -> 1.0f
        }
        
        var protein = (weight * proteinPerKg).toInt()
        var fat = (weight * fatPerKg).toInt()
        
        // Calories from P & F
        val proteinCals = protein * 4
        val fatCals = fat * 9
        
        // Remaining for Carbs
        val remainingCals = dailyCalorieTarget - proteinCals - fatCals
        var carbs = (remainingCals / 4).coerceAtLeast(0) // Ensure not negative
        
        // Safety check: if calculation is weird, fallback to percentages
        if (carbs == 0 && dailyCalorieTarget > 0) {
             // Fallback: 50/30/20 ratio
             carbs = (dailyCalorieTarget * 0.5 / 4).toInt()
             protein = (dailyCalorieTarget * 0.3 / 4).toInt()
             fat = (dailyCalorieTarget * 0.2 / 9).toInt()
        }
        
        return Triple(carbs, protein, fat)
    }
}
