package com.example.calorietracker.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration16To17Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "migration-16-17-test.db"

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationPreservesItemsAndAddsNutritionSchema() {
        createVersion16Database().use { helper ->
            val database = helper.writableDatabase
            database.execSQL(
                "INSERT INTO calorie_items (id, date, type, name, calories, carbs, protein, fat, time, createdAt) " +
                    "VALUES ('old-item', '2026-07-18', 'food', '旧记录', 120.0, 10.0, 4.0, 2.0, '12:00', 'now')"
            )
        }

        openVersion17Database().use { helper ->
            val database = helper.writableDatabase
            database.query("SELECT name, calories FROM calorie_items WHERE id = 'old-item'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("旧记录", cursor.getString(0))
                assertEquals(120.0, cursor.getDouble(1), 0.0)
            }
            database.query("PRAGMA table_info(calorie_items)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                val columns = buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                }
                assertTrue("nutritionReferenceAmount" in columns)
                assertTrue("nutritionActualAmount" in columns)
                assertTrue("nutritionEnergyUnit" in columns)
            }
            database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='food_templates'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }
    }

    @Test
    fun publishedVersion15CanUpgradeDirectlyTo17WithoutLosingData() {
        createVersion15Database().use { helper ->
            val database = helper.writableDatabase
            database.execSQL(
                "INSERT INTO calorie_items (id, date, type, name, calories, carbs, protein, fat, time, createdAt) " +
                    "VALUES ('v15-item', '2026-07-17', 'food', '旧版食物', 88.0, 8.0, 3.0, 1.0, '08:00', 'old')"
            )
            database.execSQL(
                "INSERT INTO ai_chat_messages (role, content, timestamp) " +
                    "VALUES ('user', '旧版全局对话', 12345)"
            )
        }

        openVersion17Database().use { helper ->
            val database = helper.writableDatabase
            database.query("SELECT name FROM calorie_items WHERE id = 'v15-item'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("旧版食物", cursor.getString(0))
            }
            database.query("SELECT content, weekStartDate FROM ai_chat_messages").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("旧版全局对话", cursor.getString(0))
                assertTrue(cursor.isNull(1))
            }
        }
    }

    private fun createVersion16Database(): SupportSQLiteOpenHelper =
        helper(version = 16, onCreate = { db ->
            db.execSQL(
                """
                CREATE TABLE calorie_items (
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
                    createdAt TEXT NOT NULL
                )
                """.trimIndent()
            )
        })

    private fun createVersion15Database(): SupportSQLiteOpenHelper =
        helper(version = 15, onCreate = { db ->
            db.execSQL(
                """
                CREATE TABLE calorie_items (
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
                    createdAt TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE ai_chat_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    imageUrl TEXT,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )
        })

    private fun openVersion17Database(): SupportSQLiteOpenHelper =
        helper(version = 17, onUpgrade = { db, oldVersion, newVersion ->
            var currentVersion = oldVersion
            if (currentVersion == 15 && newVersion >= 16) {
                AppDatabase.MIGRATION_15_16.migrate(db)
                currentVersion = 16
            }
            if (currentVersion == 16 && newVersion >= 17) {
                AppDatabase.MIGRATION_16_17.migrate(db)
            }
        })

    private fun helper(
        version: Int,
        onCreate: (SupportSQLiteDatabase) -> Unit = {},
        onUpgrade: (SupportSQLiteDatabase, Int, Int) -> Unit = { _, _, _ -> }
    ): SupportSQLiteOpenHelper {
        val callback = object : SupportSQLiteOpenHelper.Callback(version) {
            override fun onCreate(db: SupportSQLiteDatabase) = onCreate(db)
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
                onUpgrade(db, oldVersion, newVersion)
        }
        return FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(callback)
                .build()
        )
    }
}
