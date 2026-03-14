package com.example.calorietracker.util

import com.example.calorietracker.data.UserProfileEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

object CalorieUtils {
    enum class MealCategory(val label: String) {
        BREAKFAST("早餐"),
        MORNING_EXTRA("早加餐"),
        LUNCH("午餐"),
        AFTERNOON_EXTRA("午加餐"),
        AFTERNOON_TEA("下午茶"),
        DINNER("晚餐"),
        EVENING_EXTRA("晚加餐"),
        SNACK("零食"),
        NIGHT_SNACK("夜宵");

        companion object {
            fun fromLabel(label: String?): MealCategory? {
                if (label.isNullOrBlank()) return null
                return entries.firstOrNull { it.label == label.trim() }
            }
        }
    }

    val manualSelectableMealCategories = listOf(
        MealCategory.BREAKFAST,
        MealCategory.MORNING_EXTRA,
        MealCategory.LUNCH,
        MealCategory.AFTERNOON_EXTRA,
        MealCategory.AFTERNOON_TEA,
        MealCategory.DINNER,
        MealCategory.EVENING_EXTRA,
        MealCategory.SNACK,
        MealCategory.NIGHT_SNACK
    )
    
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

    private fun normalizeGoal(goal: String): String {
        return when (goal.trim().lowercase(Locale.getDefault())) {
            "loss", "lose", "减重", "减脂" -> "lose"
            "gain", "增重", "增肌" -> "gain"
            else -> "maintain"
        }
    }

    private fun normalizeActivityLevel(activityLevel: String): String {
        val normalized = activityLevel.trim().lowercase(Locale.getDefault())
        return if (normalized in ACTIVITY_MULTIPLIERS.keys) normalized else "sedentary"
    }

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
        val multiplier = ACTIVITY_MULTIPLIERS[normalizeActivityLevel(activityLevel)] ?: 1.2f
        val tdee = bmr * multiplier
        val normalizedGoal = normalizeGoal(goal)
        val adjustment = when (normalizedGoal) {
            "lose" -> (-tdee * 0.15f).coerceIn(-700f, -250f)
            "gain" -> (tdee * 0.10f).coerceIn(120f, 450f)
            else -> 0f
        }
        val minSafeTarget = if (gender.trim().lowercase(Locale.getDefault()) == "male") 1400f else 1200f
        return (tdee + adjustment).coerceAtLeast(minSafeTarget).roundToInt()
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

    fun getMealCategoryByTime(time: String): MealCategory {
        val minuteOfDay = parseMinuteOfDay(time) ?: return MealCategory.DINNER
        return when {
            minuteOfDay in 240 until 600 -> MealCategory.BREAKFAST
            minuteOfDay in 600 until 840 -> MealCategory.LUNCH
            minuteOfDay in 840 until 1020 -> MealCategory.AFTERNOON_TEA
            minuteOfDay in 1020 until 1320 -> MealCategory.DINNER
            else -> MealCategory.NIGHT_SNACK
        }
    }

    fun resolveMealCategory(mealCategoryLabel: String?, time: String): MealCategory {
        return MealCategory.fromLabel(mealCategoryLabel) ?: getMealCategoryByTime(time)
    }

    private fun parseMinuteOfDay(time: String): Int? {
        val parts = time.split(":")
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    private data class ParsedDate(val year: Int, val month: Int, val day: Int)

    private fun parseDateParts(date: String?): ParsedDate? {
        val raw = date?.trim().orEmpty()
        if (raw.isBlank()) return null
        val parts = raw.split(Regex("[^0-9]+")).filter { it.isNotBlank() }
        if (parts.size < 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        return ParsedDate(year, month, day)
    }

    private fun dateOrderValue(date: ParsedDate): Int {
        return date.year * 10000 + date.month * 100 + date.day
    }

    fun getEffectiveWeight(
        dateStr: String,
        records: List<com.example.calorietracker.data.DailyRecordEntity>,
        userProfile: UserProfileEntity?
    ): Float {
        val targetDate = parseDateParts(dateStr)
        val record = records.firstOrNull {
            val parsed = parseDateParts(it.date) ?: return@firstOrNull false
            val target = targetDate ?: return@firstOrNull false
            parsed.year == target.year && parsed.month == target.month && parsed.day == target.day
        }
        if (record != null && record.weight != null && record.weight.isFinite() && record.weight > 0) {
            return record.weight
        }

        val targetValue = targetDate?.let { dateOrderValue(it) }
        if (targetValue != null) {
            val previousRecord = records
                .mapNotNull {
                    val parsed = parseDateParts(it.date) ?: return@mapNotNull null
                    val weight = it.weight ?: return@mapNotNull null
                    if (!weight.isFinite() || weight <= 0f) return@mapNotNull null
                    dateOrderValue(parsed) to weight
                }
                .filter { (dateValue, _) -> dateValue < targetValue }
                .maxByOrNull { it.first }
            if (previousRecord != null) {
                return previousRecord.second
            }
        }

        return userProfile?.weight ?: 70f
    }

    // Calculate Macro Targets (Carbs, Protein, Fat) in grams
    // Returns Triple(Carbs, Protein, Fat)
    fun calculateMacroTargets(
        gender: String,
        age: Int,
        weight: Float, // kg
        activityLevel: String,
        goal: String, // "loss" (减重), "maintain" (保持), "gain" (增重)
        dailyCalorieTarget: Int
    ): Triple<Int, Int, Int> {
        val normalizedGoal = normalizeGoal(goal)
        val normalizedActivity = normalizeActivityLevel(activityLevel)
        val normalizedGender = gender.trim().lowercase(Locale.getDefault())

        val goalProteinPerKg = when (normalizedGoal) {
            "lose" -> 2.0f
            "gain" -> 1.8f
            else -> 1.6f
        }
        val activityProteinAdj = when (normalizedActivity) {
            "sedentary" -> -0.1f
            "light" -> 0f
            "moderate" -> 0.1f
            "active" -> 0.2f
            "very_active" -> 0.25f
            else -> 0f
        }
        val ageProteinAdj = when {
            age >= 60 -> 0.2f
            age >= 45 -> 0.1f
            age <= 25 -> 0.05f
            else -> 0f
        }
        val genderProteinAdj = if (normalizedGender == "male") 0.05f else 0f

        val proteinPerKg = (goalProteinPerKg + activityProteinAdj + ageProteinAdj + genderProteinAdj)
            .coerceIn(1.3f, 2.4f)
        var protein = (weight * proteinPerKg).roundToInt()

        var fatRatio = when (normalizedGoal) {
            "lose" -> 0.25f
            "gain" -> 0.27f
            else -> 0.28f
        }
        fatRatio += when {
            age >= 50 -> 0.02f
            age <= 25 -> -0.01f
            else -> 0f
        }
        fatRatio += if (normalizedGender == "female") 0.01f else 0f
        fatRatio += when (normalizedActivity) {
            "active" -> -0.01f
            "very_active" -> -0.02f
            else -> 0f
        }
        fatRatio = fatRatio.coerceIn(0.20f, 0.35f)

        var fat = ((dailyCalorieTarget * fatRatio) / 9f).roundToInt()
        val minFat = (weight * if (normalizedGender == "female") 0.65f else 0.60f).roundToInt()
        fat = fat.coerceAtLeast(minFat)

        var carbs = ((dailyCalorieTarget - protein * 4 - fat * 9) / 4f).roundToInt()

        val minCarbsPerKg = when (normalizedActivity) {
            "sedentary" -> 1.5f
            "light" -> 2.0f
            "moderate" -> 2.5f
            "active" -> 3.0f
            "very_active" -> 3.5f
            else -> 2.0f
        }
        val minCarbs = (weight * minCarbsPerKg).roundToInt()

        if (carbs < minCarbs) {
            var neededCalories = (minCarbs - carbs) * 4

            val maxFatReduction = (fat - minFat).coerceAtLeast(0)
            val fatReduction = min(maxFatReduction, ceil(neededCalories / 9f).toInt())
            fat -= fatReduction
            neededCalories -= fatReduction * 9

            if (neededCalories > 0) {
                val minProteinPerKg = if (normalizedGoal == "lose") 1.6f else 1.4f
                val minProtein = (weight * minProteinPerKg).roundToInt()
                val maxProteinReduction = (protein - minProtein).coerceAtLeast(0)
                val proteinReduction = min(maxProteinReduction, ceil(neededCalories / 4f).toInt())
                protein -= proteinReduction
            }

            carbs = ((dailyCalorieTarget - protein * 4 - fat * 9) / 4f).roundToInt()
        }

        carbs = carbs.coerceAtLeast(0)
        protein = protein.coerceAtLeast(0)
        fat = fat.coerceAtLeast(0)
        return Triple(carbs, protein, fat)
    }
}
