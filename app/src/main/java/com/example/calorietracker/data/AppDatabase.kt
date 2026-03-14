package com.example.calorietracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UserProfileEntity::class, DailyRecordEntity::class, CalorieItemEntity::class, AiChatMessageEntity::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun recordDao(): RecordDao
    abstract fun aiDao(): AiDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN excludedExercises TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add showMacros to UserProfile
                database.execSQL("ALTER TABLE user_profile ADD COLUMN showMacros INTEGER NOT NULL DEFAULT 0")
                
                // Add macro columns to DailyRecord
                database.execSQL("ALTER TABLE daily_records ADD COLUMN totalCarbs INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE daily_records ADD COLUMN totalProtein INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE daily_records ADD COLUMN totalFat INTEGER NOT NULL DEFAULT 0")
                
                // Add macro columns to CalorieItem
                database.execSQL("ALTER TABLE calorie_items ADD COLUMN carbs INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE calorie_items ADD COLUMN protein INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE calorie_items ADD COLUMN fat INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add birthDate to UserProfile
                database.execSQL("ALTER TABLE user_profile ADD COLUMN birthDate TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN selectedTodayThemeIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN hasSelectedTodayTheme INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calorie_tracker_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
