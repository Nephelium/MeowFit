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

    private var appContext: Context? = null
    private var allFoods: List<NutritionFoodItem>? = null

    /**
     * Call once during app init. Only stores the context; the ~886KB JSON is parsed
     * lazily on first use (off the main thread) instead of blocking Application.onCreate.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    @Synchronized
    private fun ensureLoaded() {
        if (allFoods != null) return
        val context = appContext
        allFoods = if (context == null) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<NutritionFoodItem>>() {}.type
                val parsed: List<NutritionFoodItem>? = context.assets.open("nutrition_database.json").use { stream ->
                    InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                        Gson().fromJson(reader, type)
                    }
                }
                parsed ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList() // Don't retry
            }
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
        ensureLoaded()
        val foods = allFoods ?: return emptyList()
        val kw = keyword.trim().lowercase()

        return foods
            .filter { it.foodName.lowercase().contains(kw) }
            .sortedWith(
                compareBy<NutritionFoodItem> { !it.foodName.lowercase().equals(kw) }  // exact match first
                    .thenBy { !it.foodName.lowercase().startsWith(kw) }                // starts-with second
                    .thenBy { it.foodName.length }                                      // shorter names first
            )
            .take(limit)
    }

    fun isLoaded(): Boolean = allFoods != null

    fun size(): Int {
        ensureLoaded()
        return allFoods?.size ?: 0
    }

    /** Get a food by exact name match */
    fun getByName(name: String): NutritionFoodItem? {
        ensureLoaded()
        return allFoods?.firstOrNull { it.foodName == name.trim() }
    }
}
