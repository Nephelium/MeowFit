package com.example.calorietracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiDao {
    @Query("SELECT * FROM ai_chat_messages WHERE weekStartDate IS NULL ORDER BY timestamp ASC, id ASC")
    fun getGlobalMessages(): Flow<List<AiChatMessageEntity>>

    @Query("SELECT * FROM ai_chat_messages WHERE weekStartDate = :weekStartDate ORDER BY timestamp ASC, id ASC")
    fun getMessagesByWeek(weekStartDate: String): Flow<List<AiChatMessageEntity>>

    @Query("SELECT * FROM ai_chat_messages WHERE weekStartDate IS NULL ORDER BY timestamp ASC, id ASC")
    suspend fun getGlobalMessagesSync(): List<AiChatMessageEntity>

    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesSync(): List<AiChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiChatMessageEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM ai_chat_messages WHERE timestamp = :timestamp AND role = :role AND content = :content)")
    suspend fun messageExists(timestamp: Long, role: String, content: String): Boolean

    @Query("DELETE FROM ai_chat_messages WHERE weekStartDate IS NULL")
    suspend fun clearGlobalMessages()

    @Query("DELETE FROM ai_chat_messages WHERE weekStartDate = :weekStartDate")
    suspend fun clearMessagesByWeek(weekStartDate: String)

    @Query("DELETE FROM ai_chat_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)
}
