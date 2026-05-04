package com.example.calorietracker.data

/**
 * Represents a food item from the official China Food Composition Table (6th Edition).
 * All nutrition values are per 100g edible portion.
 */
data class NutritionFoodItem(
    val foodCode: String,
    val foodName: String,
    val edible: String = "100",       // Edible portion percentage
    val energyKCal: String = "0",      // Energy in kcal per 100g
    val energyKJ: String = "0",        // Energy in kJ per 100g
    val protein: String = "0",         // Protein in g per 100g
    val fat: String = "0",            // Fat in g per 100g
    val CHO: String = "0",            // Carbohydrates in g per 100g
    val dietaryFiber: String = "0",   // Dietary fiber in g per 100g
    val cholesterol: String = "0",    // Cholesterol in mg per 100g
    val vitaminA: String = "0",
    val vitaminC: String = "0",
    val Ca: String = "0",            // Calcium in mg per 100g
    val Fe: String = "0",            // Iron in mg per 100g
    val Zn: String = "0",            // Zinc in mg per 100g
    val Se: String = "0",            // Selenium in µg per 100g
    val remark: String = ""
) {
    /** Parse numeric value, treating "—", "Tr", "..." as 0 */
    fun parseValue(raw: String): Double {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || trimmed == "—" || trimmed == "…" || trimmed == "Tr" || trimmed == "-") {
            return 0.0
        }
        return trimmed.toDoubleOrNull() ?: 0.0
    }

    val calories: Double get() = parseValue(energyKCal)
    val proteinG: Double get() = parseValue(protein)
    val fatG: Double get() = parseValue(fat)
    val carbsG: Double get() = parseValue(CHO)
    val fiberG: Double get() = parseValue(dietaryFiber)
}
