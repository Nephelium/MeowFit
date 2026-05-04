package com.example.calorietracker

import android.app.Application
import com.example.calorietracker.data.AppDatabase
import com.example.calorietracker.data.CalorieRepository
import com.example.calorietracker.data.NutritionDatabase

class CalorieTrackerApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { CalorieRepository(database.userDao(), database.recordDao(), database.aiDao()) }

    override fun onCreate() {
        super.onCreate()
        NutritionDatabase.init(this)
    }
}
