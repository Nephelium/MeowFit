package com.example.calorietracker.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import com.example.calorietracker.ui.screens.ChatMessage

data class AiConfig(
    val apiKey: String,
    val provider: String = "OpenAI", // OpenAI, Custom
    val baseUrl: String = "https://api.openai.com/v1/",
    val modelName: String = "gpt-4o-mini",
    val maxContext: Int = 10,
    val customChatPrompt: String? = null,
    val customImagePrompt: String? = null
)

data class AiResponseItem(
    val name: String,
    val calories: Int,
    val carbs: Int = 0,
    val protein: Int = 0,
    val fat: Int = 0,
    val type: String, // "food" or "exercise"
    val time: String? = null,
    val notes: String? = null
)

data class AiResponse(
    val items: List<AiResponseItem>,
    val summary: String? = null
)

class AiService(context: Context) {
    private val prefs = context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        const val DEFAULT_CHAT_PROMPT = """
你是一位专业的营养师和运动教练。你的任务是与用户进行自然对话，并从用户的描述中识别食物摄入和运动消耗记录。

重要规则：
1. 如果用户的描述模糊（例如只说“吃了牛肉”没有重量，或“跑步”没有时长），**请不要猜测**。请在 "message" 字段中询问用户具体的重量、数量或时长。此时 "items" 返回空数组 []。
2. 只有当用户提供了足够的信息（如“吃了200克牛肉”或“跑步30分钟”），或者用户明确要求你估算时，才进行记录。
3. 对于用户上传的图片，如果能看清营养成分表，请根据成分表和估计的重量计算；如果没有，请根据视觉估算。
4. **必须估算食物的碳水化合物、蛋白质和脂肪含量（克）**。如果用户未提供，请根据常见营养数据进行估算。

请返回一个 JSON 对象，包含两个字段：
1. "message": (字符串) 你对用户的回复。如果需要确认信息，请在这里提问。如果识别成功，请简要确认。
2. "items": (数组) 识别到的记录列表。

"items" 数组中的每个对象应包含以下字段：
- "name": (字符串) 食物或运动的名称（尽量具体）。
- "calories": (整数) 估算的卡路里数值（食物为正数，运动消耗也为正数）。
- "carbs": (整数) 碳水化合物（克），运动为0。
- "protein": (整数) 蛋白质（克），运动为0。
- "fat": (整数) 脂肪（克），运动为0。
- "type": (字符串) "food" 或 "exercise"。
- "time": (字符串) 可选，时间（格式 HH:mm）。
- "notes": (字符串) 可选，备注信息（份量、强度等）。
"""

        const val DEFAULT_IMAGE_PROMPT = """
你是一个专业的营养师和运动教练。请分析用户上传的图片（可能有多张），判断是食物还是运动场景。

1. 如果是食物：
   - 如果包含营养成分表，请优先根据成分表和食物的大致重量计算热量及三大营养素。
   - 如果没有成分表，请根据视觉估算份量、热量及三大营养素。
   - 识别食物名称（尽量具体）。

2. 如果是运动：
   - 识别运动类型。
   - 估算消耗的卡路里。

3. 返回格式（纯JSON）：
{
  "items": [
    {
      "type": "food", 
      "name": "...", 
      "calories": 0, 
      "carbs": 0,
      "protein": 0,
      "fat": 0,
      "notes": "..."
    }
  ],
  "summary": "简要说明识别结果，如果有多张图片请综合说明。"
}

只返回JSON格式，不要其他文字。
"""
    }

    fun getConfig(): AiConfig {
        return AiConfig(
            apiKey = prefs.getString("api_key", "") ?: "",
            provider = prefs.getString("provider", "OpenAI") ?: "OpenAI",
            baseUrl = prefs.getString("base_url", "https://api.openai.com/v1/") ?: "https://api.openai.com/v1/",
            modelName = prefs.getString("model_name", "gpt-4o-mini") ?: "gpt-4o-mini",
            maxContext = prefs.getInt("max_context", 10),
            customChatPrompt = prefs.getString("custom_chat_prompt", null),
            customImagePrompt = prefs.getString("custom_image_prompt", null)
        )
    }

    fun saveConfig(config: AiConfig) {
        prefs.edit()
            .putString("api_key", config.apiKey)
            .putString("provider", config.provider)
            .putString("base_url", config.baseUrl)
            .putString("model_name", config.modelName)
            .putInt("max_context", config.maxContext)
            .putString("custom_chat_prompt", config.customChatPrompt)
            .putString("custom_image_prompt", config.customImagePrompt)
            .apply()
    }

    suspend fun analyzeText(text: String, userWeight: Float, history: List<ChatMessage> = emptyList()): AiResponse {
        // Delegate to sendMessageWithImage with no images
        return sendMessageWithImage(text, emptyList(), userWeight, history)
    }
    
    suspend fun sendMessageWithImage(text: String, bitmaps: List<Bitmap>, userWeight: Float, history: List<ChatMessage> = emptyList()): AiResponse {
        val config = getConfig()
        if (config.apiKey.isBlank()) throw Exception("API Key is missing")
        
        // Use custom prompt if available, otherwise default
        val basePrompt = config.customChatPrompt?.takeIf { it.isNotBlank() } ?: DEFAULT_CHAT_PROMPT
        
        // Append user context dynamically
        val systemPrompt = "$basePrompt\n\n当前用户体重: ${userWeight}kg。"

        val contextMessages = history.takeLast(config.maxContext).map { msg ->
            mapOf("role" to msg.role, "content" to msg.content)
        }
        
        val messages = mutableListOf<Map<String, Any>>()
        messages.add(mapOf("role" to "system", "content" to systemPrompt))
        messages.addAll(contextMessages)
        
        if (bitmaps.isNotEmpty()) {
            val contentList = mutableListOf<Map<String, Any>>()
            contentList.add(mapOf("type" to "text", "text" to text))
            
            bitmaps.forEach { bitmap ->
                val base64Image = encodeImage(bitmap)
                contentList.add(mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64Image")))
            }
            
            messages.add(mapOf("role" to "user", "content" to contentList))
        } else {
            messages.add(mapOf("role" to "user", "content" to text))
        }

        return withContext(Dispatchers.IO) {
            val requestBody = mapOf(
                "model" to config.modelName,
                "messages" to messages
            )
            val jsonBody = gson.toJson(requestBody)
            val request = Request.Builder()
                .url(if (config.baseUrl.endsWith("/")) "${config.baseUrl}chat/completions" else "${config.baseUrl}/chat/completions")
                .header("Authorization", "Bearer ${config.apiKey}")
                .header("Content-Type", "application/json")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()

            executeRequest(request)
        }
    }

    suspend fun analyzeImage(bitmaps: List<Bitmap>, userWeight: Float, notes: String? = null): AiResponse {
        val config = getConfig()
        if (config.apiKey.isBlank()) throw Exception("API Key is missing")

        val basePrompt = config.customImagePrompt?.takeIf { it.isNotBlank() } ?: DEFAULT_IMAGE_PROMPT
        
        var prompt = "$basePrompt\n\n当前用户信息:"
        prompt += "\n- 体重: ${userWeight}kg"
        if (!notes.isNullOrBlank()) {
            prompt += "\n- 用户备注: $notes (请重点参考备注内容)"
        }

        return callLlmApiWithImages(config, prompt, bitmaps)
    }

    suspend fun testConnection(config: AiConfig): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Use a simple chat completion request with max_tokens=1 to test auth and model access
                val requestBody = mapOf(
                    "model" to config.modelName,
                    "messages" to listOf(
                        mapOf("role" to "user", "content" to "Hello")
                    ),
                    "max_tokens" to 5
                )
                
                val jsonBody = gson.toJson(requestBody)
                // Ensure proper URL construction
                val url = if (config.baseUrl.endsWith("/")) "${config.baseUrl}chat/completions" else "${config.baseUrl}/chat/completions"
                
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer ${config.apiKey}")
                    .post(jsonBody.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private suspend fun callLlmApi(config: AiConfig, systemPrompt: String): AiResponse {
        return withContext(Dispatchers.IO) {
            val requestBody = mapOf(
                "model" to config.modelName,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to systemPrompt),
                    mapOf("role" to "user", "content" to "请分析我的输入") // Placeholder user message if prompt is all system
                )
                // Remove temperature
            )
            
            val jsonBody = gson.toJson(requestBody)
            val request = Request.Builder()
            .url(if (config.baseUrl.endsWith("/")) "${config.baseUrl}chat/completions" else "${config.baseUrl}/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json") // Explicitly add Content-Type header
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

            executeRequest(request)
        }
    }

    private suspend fun callLlmApiWithImages(config: AiConfig, prompt: String, bitmaps: List<Bitmap>): AiResponse {
        return withContext(Dispatchers.IO) {
            val contentList = mutableListOf<Map<String, Any>>()
            contentList.add(mapOf("type" to "text", "text" to prompt))
            
            bitmaps.forEach { bitmap ->
                val base64Image = encodeImage(bitmap)
                contentList.add(mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64Image")))
            }
            
            val requestBody = mapOf(
                "model" to config.modelName, 
                "messages" to listOf(
                    mapOf("role" to "user", "content" to contentList) // Use User role for prompt + images
                )
            )

            val jsonBody = gson.toJson(requestBody)
            val request = Request.Builder()
                .url(if (config.baseUrl.endsWith("/")) "${config.baseUrl}chat/completions" else "${config.baseUrl}/chat/completions")
                .header("Authorization", "Bearer ${config.apiKey}")
                .header("Content-Type", "application/json")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()

            executeRequest(request)
        }
    }

    private fun executeRequest(request: Request): AiResponse {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // Try to read error body
                val errorBody = response.body?.string() ?: "No error body"
                throw Exception("API Error: ${response.code} ${response.message} - $errorBody")
            }
            
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            val content = try {
                // Parse OpenAI format more safely
                val jsonObject = com.google.gson.JsonParser.parseString(responseBody).asJsonObject
                val choices = jsonObject.getAsJsonArray("choices")
                if (choices == null || choices.size() == 0) {
                     return AiResponse(emptyList())
                }
                val message = choices.get(0).asJsonObject.getAsJsonObject("message")
                if (!message.has("content") || message.get("content").isJsonNull) {
                    return AiResponse(emptyList())
                }
                message.get("content").asString
            } catch (e: Exception) {
                throw Exception("Failed to parse response structure: ${e.localizedMessage}")
            }

            if (content.isBlank()) {
                return AiResponse(emptyList())
            }
            
            // Robust JSON extraction
            var jsonString = content.trim()
            
            // 1. Remove markdown code blocks if present
            // Simple removal of ```json and ```
            if (jsonString.startsWith("```")) {
                val firstNewline = jsonString.indexOf('\n')
                if (firstNewline != -1) {
                    jsonString = jsonString.substring(firstNewline + 1)
                }
                val lastBackticks = jsonString.lastIndexOf("```")
                if (lastBackticks != -1) {
                    jsonString = jsonString.substring(0, lastBackticks)
                }
            }
            
            jsonString = jsonString.trim()
            
            // 2. Find first '{' or '['
            val firstBrace = jsonString.indexOfFirst { it == '{' } // Expecting Object now
            if (firstBrace == -1) {
                // If no JSON object found, maybe it's just text?
                // If we changed the prompt to ALWAYS return JSON, this is an error.
                // But for robustness, if it looks like plain text, treat as message with empty items.
                 return AiResponse(emptyList(), jsonString)
            }
            
            // 3. Find last '}'
            val lastBrace = jsonString.lastIndexOf('}')
            if (lastBrace == -1 || lastBrace < firstBrace) {
                 throw Exception("Incomplete JSON in response: $content")
            }
            
            jsonString = jsonString.substring(firstBrace, lastBrace + 1)

            try {
                val jsonElement = com.google.gson.JsonParser.parseString(jsonString)
                
                if (jsonElement.isJsonObject) {
                    val jsonObject = jsonElement.asJsonObject
                    
                    var message: String? = null
                    if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull) {
                        message = jsonObject.get("message").asString
                    }
                    
                    var items: List<AiResponseItem> = emptyList()
                    if (jsonObject.has("items")) {
                        val itemsArray = jsonObject.get("items")
                        if (itemsArray.isJsonArray) {
                            val itemType = object : TypeToken<List<AiResponseItem>>() {}.type
                            items = gson.fromJson<List<AiResponseItem>>(itemsArray, itemType) ?: emptyList()
                        }
                    }
                    
                    // Fallback for old prompt format (if user didn't update app but server changed? unlikely for local)
                    // Or if model output "summary" instead of "message"
                    if (message == null && jsonObject.has("summary")) {
                        message = jsonObject.get("summary").asString
                    }
                    
                    return AiResponse(items, message)
                }
                
                // If it returned an array directly (old format), handle it
                if (jsonElement.isJsonArray) {
                    val itemType = object : TypeToken<List<AiResponseItem>>() {}.type
                    val items = gson.fromJson<List<AiResponseItem>>(jsonElement, itemType) ?: emptyList()
                    return AiResponse(items, "已识别 ${items.size} 条记录")
                }
                
                return AiResponse(emptyList(), content)
            } catch (e: Exception) {
                // If JSON parsing fails, return the content as message
                return AiResponse(emptyList(), content)
            }
        }
    }

    fun encodeImage(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize if too large to avoid payload limits
        var resized = bitmap
        if (bitmap.width > 1024 || bitmap.height > 1024) {
             val scale = 1024.0f / maxOf(bitmap.width, bitmap.height)
             resized = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        }
        resized.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
