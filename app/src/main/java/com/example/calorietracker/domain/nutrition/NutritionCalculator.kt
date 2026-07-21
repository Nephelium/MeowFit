package com.example.calorietracker.domain.nutrition

object NutritionCalculator {

    fun calculate(input: NutritionInput): NutritionCalculation {
        val errors = buildSet {
            if (!input.referenceAmount.isFinite() || input.referenceAmount <= 0.0) {
                add(NutritionFieldError.REFERENCE_AMOUNT)
            }
            if (!input.actualAmount.isFinite() || input.actualAmount < 0.0) {
                add(NutritionFieldError.ACTUAL_AMOUNT)
            }
            if (!input.energyValue.isFinite() || input.energyValue < 0.0) {
                add(NutritionFieldError.ENERGY)
            }
            if (!input.carbs.isFinite() || input.carbs < 0.0) {
                add(NutritionFieldError.CARBS)
            }
            if (!input.protein.isFinite() || input.protein < 0.0) {
                add(NutritionFieldError.PROTEIN)
            }
            if (!input.fat.isFinite() || input.fat < 0.0) {
                add(NutritionFieldError.FAT)
            }
        }
        if (errors.isNotEmpty()) return NutritionCalculation.Invalid(errors)

        val ratio = input.actualAmount / input.referenceAmount
        val energyKcal = input.energyUnit.toKcal(input.energyValue) * ratio
        val result = CalculatedNutrition(
            ratio = ratio,
            energyKcal = energyKcal,
            carbs = input.carbs * ratio,
            protein = input.protein * ratio,
            fat = input.fat * ratio
        )

        return if (
            result.ratio.isFinite() &&
            result.energyKcal.isFinite() &&
            result.carbs.isFinite() &&
            result.protein.isFinite() &&
            result.fat.isFinite()
        ) {
            NutritionCalculation.Success(result)
        } else {
            NutritionCalculation.Invalid(setOf(NutritionFieldError.ENERGY))
        }
    }
}
