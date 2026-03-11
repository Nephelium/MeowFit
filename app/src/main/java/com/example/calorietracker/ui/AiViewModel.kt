package com.example.calorietracker.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.CalorieTrackerApp
import com.example.calorietracker.data.AiChatMessageEntity
import com.example.calorietracker.data.ai.AiConfig
import com.example.calorietracker.data.ai.AiResponseItem
import com.example.calorietracker.data.ai.AiService
import com.example.calorietracker.ui.screens.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AiUiState {
    object Idle : AiUiState()
    object Loading : AiUiState()
    data class Success(val items: List<AiResponseItem>, val summary: String? = null) : AiUiState()
    data class Error(val message: String) : AiUiState()
}

class AiViewModel(application: Application) : AndroidViewModel(application) {
    private val aiService = AiService(application)
    private val repository = (application as CalorieTrackerApp).repository

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _config = MutableStateFlow(aiService.getConfig())
    val config: StateFlow<AiConfig> = _config.asStateFlow()
    
    val chatMessages = repository.getAllAiMessages()
        .map { list ->
            list.map { entity ->
                ChatMessage(
                    id = entity.id.toString(),
                    role = entity.role,
                    content = entity.content
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateConfig(apiKey: String, provider: String, baseUrl: String, modelName: String, maxContext: Int, customChatPrompt: String? = null, customImagePrompt: String? = null) {
        val newConfig = AiConfig(apiKey, provider, baseUrl, modelName, maxContext, customChatPrompt, customImagePrompt)
        aiService.saveConfig(newConfig)
        _config.value = newConfig
    }
    
    // Overload for compatibility if needed, or remove if unused
    fun updateConfig(apiKey: String, provider: String, baseUrl: String, modelName: String) {
        updateConfig(apiKey, provider, baseUrl, modelName, _config.value.maxContext, _config.value.customChatPrompt, _config.value.customImagePrompt)
    }

    suspend fun testConnection(apiKey: String, baseUrl: String, modelName: String): Boolean {
        val testConfig = AiConfig(apiKey, _config.value.provider, baseUrl, modelName, _config.value.maxContext, _config.value.customChatPrompt, _config.value.customImagePrompt)
        return aiService.testConnection(testConfig)
    }

    // For PhotoRecognitionTab (One-off)
    fun analyzeText(text: String, userWeight: Float) {
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            try {
                val response = aiService.analyzeText(text, userWeight)
                _uiState.value = AiUiState.Success(response.items, response.summary)
            } catch (e: Exception) {
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    // For PhotoRecognitionTab (One-off)
    fun analyzeImage(bitmaps: List<Bitmap>, userWeight: Float) {
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            try {
                val response = aiService.analyzeImage(bitmaps, userWeight)
                _uiState.value = AiUiState.Success(response.items, response.summary)
            } catch (e: Exception) {
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
    
    // For AiDialogueTab (Persistent)
    fun sendMessage(text: String, userWeight: Float) {
        viewModelScope.launch {
            // Save user message
            repository.addAiMessage(AiChatMessageEntity(role = "user", content = text))
            
            _uiState.value = AiUiState.Loading
            // Call AI
            try {
                val history = chatMessages.value
                val response = aiService.analyzeText(text, userWeight, history)
                
                // Save AI response
                repository.addAiMessage(AiChatMessageEntity(role = "assistant", content = response.summary ?: "已识别 ${response.items.size} 条记录。"))
                
                // Update UI state for "Add" buttons (latest interaction)
                _uiState.value = AiUiState.Success(response.items, response.summary)
            } catch (e: Exception) {
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
    
    fun sendMessageWithImage(text: String, bitmaps: List<Bitmap>, userWeight: Float) {
         viewModelScope.launch {
            // Save user message
            val imageTag = if (bitmaps.size > 1) "[${bitmaps.size}张图片]" else "[图片]"
            repository.addAiMessage(AiChatMessageEntity(role = "user", content = "$text $imageTag"))
            
            _uiState.value = AiUiState.Loading
            try {
                val history = chatMessages.value
                val response = aiService.sendMessageWithImage(text, bitmaps, userWeight, history)
                
                repository.addAiMessage(AiChatMessageEntity(role = "assistant", content = response.summary ?: "已识别 ${response.items.size} 条记录。"))
                _uiState.value = AiUiState.Success(response.items, response.summary)
            } catch (e: Exception) {
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "Unknown error")
            }
         }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAiMessages()
        }
    }

    fun clearState() {
        _uiState.value = AiUiState.Idle
    }
}
