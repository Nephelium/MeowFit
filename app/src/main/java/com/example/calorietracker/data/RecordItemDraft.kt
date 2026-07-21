package com.example.calorietracker.data

data class RecordItemDraft(
    val type: String,
    val name: String,
    val calories: Double,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val time: String = "",
    val mealCategory: String? = null,
    val notes: String? = null,
    val imageUrl: String? = null,
    val nutritionReferenceAmount: Double? = null,
    val nutritionActualAmount: Double? = null,
    val nutritionAmountUnit: String? = null,
    val nutritionReferenceEnergy: Double? = null,
    val nutritionEnergyUnit: String? = null,
    val nutritionReferenceCarbs: Double? = null,
    val nutritionReferenceProtein: Double? = null,
    val nutritionReferenceFat: Double? = null
)
