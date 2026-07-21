package com.example.calorietracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        DailyRecordEntity::class,
        CalorieItemEntity::class,
        AiChatMessageEntity::class,
        WeeklySummaryEntity::class,
        FoodTemplateEntity::class
    ],
    version = 17,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun recordDao(): RecordDao
    abstract fun aiDao(): AiDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun foodTemplateDao(): FoodTemplateDao

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

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE calorie_items ADD COLUMN mealCategory TEXT")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                fun tableExists(tableName: String): Boolean {
                    database.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use { cursor ->
                        return cursor.moveToFirst()
                    }
                }

                fun columnExists(tableName: String, columnName: String): Boolean {
                    database.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                        val nameIndex = cursor.getColumnIndex("name")
                        while (cursor.moveToNext()) {
                            if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) return true
                        }
                        return false
                    }
                }

                if (!columnExists("user_profile", "weekStartDay")) {
                    database.execSQL("ALTER TABLE user_profile ADD COLUMN weekStartDay INTEGER NOT NULL DEFAULT 1")
                }

                if (tableExists("calorie_items_new")) {
                    database.execSQL("DROP TABLE calorie_items_new")
                }

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS calorie_items_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL,
                        type TEXT NOT NULL,
                        name TEXT NOT NULL,
                        calories REAL NOT NULL,
                        carbs REAL NOT NULL,
                        protein REAL NOT NULL,
                        fat REAL NOT NULL,
                        time TEXT NOT NULL,
                        mealCategory TEXT,
                        imageUrl TEXT,
                        notes TEXT,
                        createdAt TEXT NOT NULL,
                        FOREIGN KEY(date) REFERENCES daily_records(date) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                val hasMealCategory = columnExists("calorie_items", "mealCategory")
                database.execSQL(
                    """
                    INSERT INTO calorie_items_new (id, date, type, name, calories, carbs, protein, fat, time, mealCategory, imageUrl, notes, createdAt)
                    SELECT id, date, type, name,
                           CAST(calories AS REAL),
                           CAST(COALESCE(carbs, 0) AS REAL),
                           CAST(COALESCE(protein, 0) AS REAL),
                           CAST(COALESCE(fat, 0) AS REAL),
                           COALESCE(time, ''),
                           ${if (hasMealCategory) "mealCategory" else "NULL"},
                           imageUrl,
                           notes,
                           COALESCE(createdAt, '')
                    FROM calorie_items
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE calorie_items")
                database.execSQL("ALTER TABLE calorie_items_new RENAME TO calorie_items")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_calorie_items_date ON calorie_items(date)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS weekly_summaries (
                        weekStartDate TEXT NOT NULL PRIMARY KEY,
                        weekEndDate TEXT NOT NULL,
                        summaryText TEXT NOT NULL,
                        recommendations TEXT NOT NULL,
                        dietDays INTEGER NOT NULL,
                        exerciseDays INTEGER NOT NULL,
                        generatedAt INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN medicationEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE user_profile ADD COLUMN medications TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE daily_records ADD COLUMN medicationTaken TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN medicationTimes TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ai_chat_messages ADD COLUMN weekStartDate TEXT")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE calorie_items ADD COLUMN nutritionReferenceAmount REAL")
                database.execSQL("ALTER TABLE calorie_items ADD COLUMN nutritionActualAmount REAL")
                database.execSQL("ALTER TABLE calorie_items ADD COLUMN nutritionAmountUnit TEXT")
                database.execSQL("ALTER TABLE calorie_items ADD COLUMN nutritionReferenceEnergy REAL")
                database.execSQL("ALTER TABLE calorie_items ADD COLUMN nutritionEnergyUnit TEXT")
                database.execSQL("ALTER TABLE calorie_items ADD COLUMN nutritionReferenceCarbs REAL")
                database.execSQL("ALTER TABLE calorie_items ADD COLUMN nutritionReferenceProtein REAL")
                database.execSQL("ALTER TABLE calorie_items ADD COLUMN nutritionReferenceFat REAL")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS food_templates (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        referenceAmount REAL NOT NULL,
                        amountUnit TEXT NOT NULL,
                        energyValue REAL NOT NULL,
                        energyUnit TEXT NOT NULL,
                        carbs REAL NOT NULL,
                        protein REAL NOT NULL,
                        fat REAL NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_food_templates_name ON food_templates(name)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_food_templates_updatedAt ON food_templates(updatedAt)")
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
                .addMigrations(
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
