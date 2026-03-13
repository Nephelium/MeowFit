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

    private val _photoUiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val photoUiState: StateFlow<AiUiState> = _photoUiState.asStateFlow()

    // Persistent lists for recognized items to survive tab switching
    private val _chatRecognizedItems = mutableListOf<com.example.calorietracker.ui.screens.EntryItem>()
    val chatRecognizedItems: List<com.example.calorietracker.ui.screens.EntryItem> get() = _chatRecognizedItems

    private val _photoRecognizedItems = mutableListOf<com.example.calorietracker.ui.screens.EntryItem>()
    val photoRecognizedItems: List<com.example.calorietracker.ui.screens.EntryItem> get() = _photoRecognizedItems
    
    // Helper to update lists (using a flow or just exposing the list? 
    // Compose needs State. Let's use MutableStateFlow for the lists or just MutableStateList if we were in Compose, 
    // but here we are in ViewModel.
    // Better to use MutableStateFlow<List<EntryItem>>
    
    private val _chatItemsFlow = MutableStateFlow<List<com.example.calorietracker.ui.screens.EntryItem>>(emptyList())
    val chatItemsFlow: StateFlow<List<com.example.calorietracker.ui.screens.EntryItem>> = _chatItemsFlow.asStateFlow()

    private val _photoItemsFlow = MutableStateFlow<List<com.example.calorietracker.ui.screens.EntryItem>>(emptyList())
    val photoItemsFlow: StateFlow<List<com.example.calorietracker.ui.screens.EntryItem>> = _photoItemsFlow.asStateFlow()

    private val _config = MutableStateFlow(aiService.getConfig())
    val config: StateFlow<AiConfig> = _config.asStateFlow()
    
    val chatMessages = repository.getAllAiMessages()
        .map { list ->
            list.map { entity ->
                ChatMessage(
                    id = entity.id.toString(),
                    role = entity.role,
                    content = entity.content,
                    imageUrl = entity.imageUrl
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
    fun analyzeImage(bitmaps: List<Bitmap>, userWeight: Float, notes: String? = null) {
        viewModelScope.launch {
            _photoUiState.value = AiUiState.Loading
            try {
                val response = aiService.analyzeImage(bitmaps, userWeight, notes)
                val newItems = response.items.map { 
                    com.example.calorietracker.ui.screens.EntryItem(it.type, it.name, it.calories, it.carbs, it.protein, it.fat, it.time ?: "", it.notes ?: "") 
                }
                // Update the persistent list
                val current = _photoItemsFlow.value.toMutableList()
                current.addAll(newItems)
                _photoItemsFlow.value = current
                
                _photoUiState.value = AiUiState.Success(response.items, response.summary)
            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("429") == true) "AI服务繁忙 (429)，请稍后再试" else e.localizedMessage ?: "Unknown error"
                _photoUiState.value = AiUiState.Error(errorMsg)
            }
        }
    }
    
    // For AiDialogueTab (Persistent)
    fun sendMessage(text: String, userWeight: Float) {
        viewModelScope.launch {
            // Capture history BEFORE adding new message to avoid duplication in prompt
            val history = chatMessages.value

            // Save user message
            repository.addAiMessage(AiChatMessageEntity(role = "user", content = text))
            
            _uiState.value = AiUiState.Loading
            // Call AI
            try {
                val response = aiService.analyzeText(text, userWeight, history)
                
                // Save AI response
                repository.addAiMessage(AiChatMessageEntity(role = "assistant", content = response.summary ?: "已识别 ${response.items.size} 条记录。"))
                
                // Add to chat recognized items
                val newItems = response.items.map { 
                    com.example.calorietracker.ui.screens.EntryItem(it.type, it.name, it.calories, it.carbs, it.protein, it.fat, it.time ?: "", it.notes ?: "") 
                }
                if (newItems.isNotEmpty()) {
                    val current = _chatItemsFlow.value.toMutableList()
                    current.addAll(newItems)
                    _chatItemsFlow.value = current
                }

                // Update UI state for "Add" buttons (latest interaction)
                _uiState.value = AiUiState.Success(response.items, response.summary)
            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("429") == true) "AI服务繁忙 (429)，请稍后再试" else e.localizedMessage ?: "Unknown error"
                _uiState.value = AiUiState.Error(errorMsg)
                // Add error message to chat as system message so user sees it in history? No, just UI state error is enough.
            }
        }
    }
    
    fun sendMessageWithImage(text: String, bitmaps: List<Bitmap>, imageUris: List<String>, userWeight: Float) {
         viewModelScope.launch {
            // Capture history BEFORE adding new message
            val history = chatMessages.value

            // Save images to internal storage to persist them
            val savedImageUris = bitmaps.mapIndexed { index, bitmap ->
                saveImageToInternalStorage(bitmap, "chat_image_${System.currentTimeMillis()}_$index.jpg")
            }
            
            // Save user message
            val imageTag = if (bitmaps.isNotEmpty()) "" else "[图片]" // Hide text tag if we have real images
            // Store URIs as pipe-separated string
            val imageUrls = if (savedImageUris.isNotEmpty()) savedImageUris.joinToString("|") else null
            
            repository.addAiMessage(AiChatMessageEntity(
                role = "user", 
                content = "$text $imageTag".trim(),
                imageUrl = imageUrls
            ))
            
            _uiState.value = AiUiState.Loading
            try {
                val response = aiService.sendMessageWithImage(text, bitmaps, userWeight, history)
                
                repository.addAiMessage(AiChatMessageEntity(role = "assistant", content = response.summary ?: "已识别 ${response.items.size} 条记录。"))
                
                // Add to chat recognized items
                val newItems = response.items.map { 
                    com.example.calorietracker.ui.screens.EntryItem(it.type, it.name, it.calories, it.carbs, it.protein, it.fat, it.time ?: "", it.notes ?: "") 
                }
                if (newItems.isNotEmpty()) {
                    val current = _chatItemsFlow.value.toMutableList()
                    current.addAll(newItems)
                    _chatItemsFlow.value = current
                }
                
                _uiState.value = AiUiState.Success(response.items, response.summary)
            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("429") == true) "AI服务繁忙 (429)，请稍后再试" else e.localizedMessage ?: "Unknown error"
                _uiState.value = AiUiState.Error(errorMsg)
            }
         }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap, filename: String): String {
        return try {
            val file = java.io.File(getApplication<Application>().filesDir, filename)
            val stream = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            stream.close()
            android.net.Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAiMessages()
            _chatItemsFlow.value = emptyList() // Also clear pending items? Maybe
        }
    }

    fun clearState() {
        _uiState.value = AiUiState.Idle
    }

    fun clearPhotoState() {
        _photoUiState.value = AiUiState.Idle
    }
    
    // Methods to manage the lists from UI
    fun removeChatItem(index: Int) {
        val current = _chatItemsFlow.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _chatItemsFlow.value = current
        }
    }
    
    fun updateChatItem(index: Int, item: com.example.calorietracker.ui.screens.EntryItem) {
        val current = _chatItemsFlow.value.toMutableList()
        if (index in current.indices) {
            current[index] = item
            _chatItemsFlow.value = current
        }
    }
    
    fun clearChatItems() {
        _chatItemsFlow.value = emptyList()
    }

    fun removePhotoItem(index: Int) {
        val current = _photoItemsFlow.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _photoItemsFlow.value = current
        }
    }
    
    fun updatePhotoItem(index: Int, item: com.example.calorietracker.ui.screens.EntryItem) {
        val current = _photoItemsFlow.value.toMutableList()
        if (index in current.indices) {
            current[index] = item
            _photoItemsFlow.value = current
        }
    }
    
    fun clearPhotoItems() {
        _photoItemsFlow.value = emptyList()
    }
}
