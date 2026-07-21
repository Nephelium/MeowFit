package com.example.calorietracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.calorietracker.util.CalorieUtils

/**
 * 餐次颜色的唯一来源。此前在 TodayScreen 与 OverviewScreen 各复制了一份，
 * 现在统一由这里提供；早餐跟随主题主色。
 */
@Composable
fun mealCategoryColors(): List<Triple<CalorieUtils.MealCategory, String, Color>> = listOf(
    Triple(CalorieUtils.MealCategory.BREAKFAST, "早餐", MaterialTheme.colorScheme.primary),
    Triple(CalorieUtils.MealCategory.MORNING_EXTRA, "早加餐", Color(0xFF6D4C41)),
    Triple(CalorieUtils.MealCategory.LUNCH, "午餐", Color(0xFF26A69A)),
    Triple(CalorieUtils.MealCategory.AFTERNOON_EXTRA, "午加餐", Color(0xFF00897B)),
    Triple(CalorieUtils.MealCategory.AFTERNOON_TEA, "下午茶", Color(0xFFFFB300)),
    Triple(CalorieUtils.MealCategory.DINNER, "晚餐", Color(0xFFFF7043)),
    Triple(CalorieUtils.MealCategory.EVENING_EXTRA, "晚加餐", Color(0xFF5D4037)),
    Triple(CalorieUtils.MealCategory.SNACK, "零食", Color(0xFF8E24AA)),
    Triple(CalorieUtils.MealCategory.NIGHT_SNACK, "夜宵", Color(0xFF7E57C2))
)
