package com.example.calorietracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "food_templates",
    indices = [Index(value = ["name"]), Index(value = ["updatedAt"])]
)
data class FoodTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val referenceAmount: Double,
    val amountUnit: String,
    val energyValue: Double,
    val energyUnit: String,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface FoodTemplateDao {
    @Query("SELECT * FROM food_templates ORDER BY updatedAt DESC, name COLLATE NOCASE ASC")
    fun getAllTemplates(): Flow<List<FoodTemplateEntity>>

    @Query(
        """
        SELECT * FROM food_templates
        WHERE name LIKE '%' || :keyword || '%'
        ORDER BY CASE WHEN name = :keyword THEN 0 ELSE 1 END, updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun searchTemplates(keyword: String, limit: Int = 20): List<FoodTemplateEntity>

    @Query("SELECT * FROM food_templates")
    suspend fun getAllTemplatesSync(): List<FoodTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: FoodTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplates(templates: List<FoodTemplateEntity>)

    @Delete
    suspend fun deleteTemplate(template: FoodTemplateEntity)

    @Query("DELETE FROM food_templates")
    suspend fun clearTemplates()
}
