package com.example.calorietracker.domain.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalculatorTest {

    @Test
    fun `scales kcal nutrition from arbitrary gram basis`() {
        val result = NutritionCalculator.calculate(
            NutritionInput(
                referenceAmount = 30.0,
                actualAmount = 45.0,
                amountUnit = AmountUnit.GRAM,
                energyValue = 120.0,
                energyUnit = EnergyUnit.KCAL,
                carbs = 18.0,
                protein = 4.0,
                fat = 3.0
            )
        )

        val success = result as NutritionCalculation.Success
        assertEquals(1.5, success.value.ratio, 0.000001)
        assertEquals(180.0, success.value.energyKcal, 0.000001)
        assertEquals(27.0, success.value.carbs, 0.000001)
        assertEquals(6.0, success.value.protein, 0.000001)
        assertEquals(4.5, success.value.fat, 0.000001)
    }

    @Test
    fun `converts kilojoules to kcal before scaling`() {
        val result = NutritionCalculator.calculate(
            NutritionInput(
                referenceAmount = 100.0,
                actualAmount = 250.0,
                amountUnit = AmountUnit.GRAM,
                energyValue = 500.0,
                energyUnit = EnergyUnit.KJ,
                carbs = 10.0,
                protein = 5.0,
                fat = 2.0
            )
        )

        val success = result as NutritionCalculation.Success
        assertEquals((500.0 / 4.184) * 2.5, success.value.energyKcal, 0.000001)
        assertEquals(25.0, success.value.carbs, 0.000001)
    }

    @Test
    fun `supports milliliter and serving ratios`() {
        val milliliters = NutritionCalculator.calculate(
            NutritionInput(
                referenceAmount = 100.0,
                actualAmount = 330.0,
                amountUnit = AmountUnit.MILLILITER,
                energyValue = 42.0,
                energyUnit = EnergyUnit.KCAL
            )
        ) as NutritionCalculation.Success
        val servings = NutritionCalculator.calculate(
            NutritionInput(
                referenceAmount = 1.0,
                actualAmount = 1.5,
                amountUnit = AmountUnit.SERVING,
                energyValue = 200.0,
                energyUnit = EnergyUnit.KCAL
            )
        ) as NutritionCalculation.Success

        assertEquals(138.6, milliliters.value.energyKcal, 0.000001)
        assertEquals(300.0, servings.value.energyKcal, 0.000001)
    }

    @Test
    fun `reports all invalid numeric fields without calculating`() {
        val result = NutritionCalculator.calculate(
            NutritionInput(
                referenceAmount = 0.0,
                actualAmount = -1.0,
                amountUnit = AmountUnit.GRAM,
                energyValue = Double.NaN,
                energyUnit = EnergyUnit.KCAL,
                carbs = -2.0,
                protein = Double.POSITIVE_INFINITY,
                fat = -3.0
            )
        )

        val invalid = result as NutritionCalculation.Invalid
        assertTrue(NutritionFieldError.REFERENCE_AMOUNT in invalid.errors)
        assertTrue(NutritionFieldError.ACTUAL_AMOUNT in invalid.errors)
        assertTrue(NutritionFieldError.ENERGY in invalid.errors)
        assertTrue(NutritionFieldError.CARBS in invalid.errors)
        assertTrue(NutritionFieldError.PROTEIN in invalid.errors)
        assertTrue(NutritionFieldError.FAT in invalid.errors)
    }
}
