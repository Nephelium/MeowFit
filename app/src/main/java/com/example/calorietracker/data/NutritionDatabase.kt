package com.example.calorietracker.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

/**
 * Singleton that loads the official China Food Composition Table from assets
 * and provides fuzzy search functionality.
 */
object NutritionDatabase {

    private var allFoods: List<NutritionFoodItem> = emptyList()
    private var loaded = false

    /** Call once during app init to load the database from assets */
    fun init(context: Context) {
        if (loaded) return
        try {
            val stream = context.assets.open("nutrition_database.json")
            val reader = InputStreamReader(stream, "UTF-8")
            val type = object : TypeToken<List<NutritionFoodItem>>() {}.type
            allFoods = Gson().fromJson(reader, type)
            reader.close()
            loaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            allFoods = emptyList()
            loaded = true // Don't retry
        }
    }

    /**
     * Search foods by keyword.
     * Matches if the keyword appears anywhere in the food name (case-insensitive).
     * Returns results sorted by: exact match first, then starts-with, then contains.
     * Max results: [limit].
     */
    fun search(keyword: String, limit: Int = 20): List<NutritionFoodItem> {
        if (keyword.isBlank()) return emptyList()
        val kw = keyword.trim().lowercase()

        return allFoods
            .filter { it.foodName.lowercase().contains(kw) }
            .sortedWith(
                compareBy<NutritionFoodItem> { !it.foodName.lowercase().equals(kw) }  // exact match first
                    .thenBy { !it.foodName.lowercase().startsWith(kw) }                // starts-with second
                    .thenBy { it.foodName.length }                                      // shorter names first
            )
            .take(limit)
    }

    fun isLoaded(): Boolean = loaded

    fun size(): Int = allFoods.size

    /** Get a food by exact name match */
    fun getByName(name: String): NutritionFoodItem? {
        return allFoods.firstOrNull { it.foodName == name.trim() }
    }
}
