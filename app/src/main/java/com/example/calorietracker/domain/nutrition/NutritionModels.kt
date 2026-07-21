package com.example.calorietracker.domain.nutrition

enum class EnergyUnit(val label: String) {
    KCAL("kcal"),
    KJ("kJ");

    fun toKcal(value: Double): Double = when (this) {
        KCAL -> value
        KJ -> value / KJ_PER_KCAL
    }

    companion object {
        const val KJ_PER_KCAL = 4.184

        fun fromStorage(value: String?): EnergyUnit =
            entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true)
            } ?: KCAL
    }
}

enum class AmountUnit(val label: String, val suffix: String) {
    GRAM("克", "g"),
    MILLILITER("毫升", "ml"),
    SERVING("份", "份");

    companion object {
        fun fromStorage(value: String?): AmountUnit =
            entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.label.equals(value, ignoreCase = true) ||
                    it.suffix.equals(value, ignoreCase = true) ||
                    (it == SERVING && value == "serving")
            } ?: GRAM
    }
}

data class NutritionInput(
    val referenceAmount: Double,
    val actualAmount: Double,
    val amountUnit: AmountUnit,
    val energyValue: Double,
    val energyUnit: EnergyUnit,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0
)

data class CalculatedNutrition(
    val ratio: Double,
    val energyKcal: Double,
    val carbs: Double,
    val protein: Double,
    val fat: Double
)

enum class NutritionFieldError {
    REFERENCE_AMOUNT,
    ACTUAL_AMOUNT,
    ENERGY,
    CARBS,
    PROTEIN,
    FAT
}

sealed interface NutritionCalculation {
    data class Success(val value: CalculatedNutrition) : NutritionCalculation
    data class Invalid(val errors: Set<NutritionFieldError>) : NutritionCalculation
}
