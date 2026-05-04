package com.example.calorietracker.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerApp
import com.example.calorietracker.data.*
import com.example.calorietracker.data.ai.AiService
import com.example.calorietracker.ui.AiViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisDetailScreen(
    weekStartDate: String,
    records: List<DailyRecordEntity>,
    allItems: List<CalorieItemEntity>,
    userProfile: UserProfileEntity?,
    aiViewModel: AiViewModel,
    selectedThemeIndex: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as CalorieTrackerApp
    val analysisDao = app.database.analysisDao()
    val aiService = remember { AiService(context) }

    val isDarkTheme = isSystemInDarkTheme()
    val selectedTheme = remember(selectedThemeIndex) { getTodayVisualTheme(selectedThemeIndex) }
    val cardColor = remember(selectedTheme, isDarkTheme) { themedDashboardCardColor(selectedTheme, isDarkTheme) }
    val onCardColor = com.example.calorietracker.ui.theme.onCardColor(cardColor, isDarkTheme)
    val accentColor = remember(selectedTheme, isDarkTheme) { themedAccentColor(selectedTheme, isDarkTheme) }
    val scope = rememberCoroutineScope()

    var summary by remember { mutableStateOf<WeeklySummaryEntity?>(null) }

    LaunchedEffect(weekStartDate) {
        try {
            summary = analysisDao.getSummary(weekStartDate)
        } catch (_: Exception) {
            // 加载周报失败，summary 保持 null，UI 自动隐藏周报区域
        }
    }

    // Panel ratio: report vs chat
    var reportRatio by remember { mutableFloatStateOf(0.65f) }

    // Chat state
    var chatInput by remember { mutableStateOf("") }
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Approximate token counter (rough: ~1.5 chars per token)
    fun estimateTokens(text: String): Int = (text.length / 1.5).toInt()
    val chatTokens = chatMessages.sumOf { estimateTokens(it.content) }
    val MAX_CHAT_TOKENS = 900_000
    val showTokenWarning = chatTokens > MAX_CHAT_TOKENS * 0.8

    // Build system context once when summary is loaded
    val systemContext = remember(summary) {
        if (summary != null && summary!!.status == "generated") {
            val dataText = buildWeeklyDataText(weekStartDate, records, allItems, userProfile)
            """
你是一只叫「猫猫」的元气健康小助手喵~ 🐱✨
你的主人正在喵呜健康APP里查看本周的数据分析，你要用可爱、温暖、元气满满的口吻和主人聊天！
记得多用语气词喵~（比如「喵~」「呢~」「哦~」「呀~」），适当加些emoji让回复更有活力 (๑•̀ㅂ•́)و✧
但是呢，给出健康建议的时候一定要专业靠谱，不能因为是可爱风格就乱说哦！

=== 📊 本周数据 ===
$dataText

=== 📝 AI 周报分析 ===
${summary!!.summaryText}

回应用户的时候，记得结合上面的数据和周报来给出个性化建议喵~ 如果主人做得好要狠狠夸奖！如果还有进步空间就温柔地鼓励~ 保持元气满满，让主人觉得减肥也是一件开心的事呢！(๑˃̵ᴗ˂̵)و
            """.trimIndent()
        } else {
            val dataText = buildWeeklyDataText(weekStartDate, records, allItems, userProfile)
            """
你是一只叫「猫猫」的元气健康小助手喵~ 🐱✨
你的主人正在喵呜健康APP里查看本周的数据分析，你要用可爱、温暖、元气满满的口吻和主人聊天！
记得多用语气词喵~（比如「喵~」「呢~」「哦~」「呀~」），适当加些emoji让回复更有活力 (๑•̀ㅂ•́)و✧
但是呢，给出健康建议的时候一定要专业靠谱，不能因为是可爱风格就乱说哦！

=== 📊 本周数据 ===
$dataText

回应用户的时候，记得结合上面的数据来给出个性化建议喵~ 如果主人做得好要狠狠夸奖！如果还有进步空间就温柔地鼓励~ 保持元气满满，让主人觉得减肥也是一件开心的事呢！(๑˃̵ᴗ˂̵)و
            """.trimIndent()
        }
    }

    fun sendChatMessage() {
        val input = chatInput.trim()
        if (input.isBlank() || isLoading) return
        chatInput = ""
        chatMessages.add(ChatMessage(role = "user", content = input))
        isLoading = true

        scope.launch {
            try {
                val config = aiService.getConfig()
                if (config.apiKey.isBlank()) {
                    chatMessages.add(ChatMessage(role = "assistant", content = "请先在设置中配置 AI API Key。"))
                    isLoading = false
                    return@launch
                }

                // Build message list with system context prepended
                val allMsgs = mutableListOf<Map<String, String>>()
                allMsgs.add(mapOf("role" to "system", "content" to systemContext))
                allMsgs.addAll(chatMessages.map { mapOf("role" to it.role, "content" to it.content) })
                val reply = aiService.sendPlainChat(allMsgs, userProfile?.weight ?: 70f)
                chatMessages.add(ChatMessage(role = "assistant", content = reply))
            } catch (e: Exception) {
                chatMessages.add(ChatMessage(role = "assistant", content = "抱歉，请求失败：${e.localizedMessage}"))
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            getWeekLabel(weekStartDate),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "每周分析 · AI 对话",
                            style = MaterialTheme.typography.labelSmall,
                            color = onCardColor.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = onCardColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = onCardColor
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Summary content area
            val currentSummary = summary
            if (currentSummary != null && currentSummary.status == "generated") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(reportRatio)
                        .clickable { reportRatio = 0.80f }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "📊 周报分析",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = onCardColor
                            )
                            Text(
                                "🍽${currentSummary.dietDays}天 🏃${currentSummary.exerciseDays}天",
                                style = MaterialTheme.typography.labelSmall,
                                color = onCardColor.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        MarkdownText(
                            text = currentSummary.summaryText,
                            baseColor = onCardColor.copy(alpha = 0.85f),
                            baseFontSize = 14f,
                            lineHeight = 22f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Token warning
            if (showTokenWarning) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (chatTokens > MAX_CHAT_TOKENS)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (chatTokens > MAX_CHAT_TOKENS) "⚠ 上下文已满，请清理对话"
                        else "📝 上下文使用: ${(chatTokens * 100 / MAX_CHAT_TOKENS)}%",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (chatTokens > MAX_CHAT_TOKENS)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Chat area
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f - reportRatio)
                    .clickable { reportRatio = 0.30f }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Messages
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(12.dp),
                        state = listState
                    ) {
                        if (chatMessages.isEmpty()) {
                            item {
                                Text(
                                    "💬 可以和 AI 聊聊本周的数据分析，或者任何健康相关的问题~",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onCardColor.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
                                )
                            }
                        }
                        items(chatMessages) { msg ->
                            ChatBubble(
                                message = msg,
                                accentColor = accentColor,
                                cardColor = cardColor,
                                onCardColor = onCardColor
                            )
                        }
                        if (isLoading) {
                            item {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = accentColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("思考中...", style = MaterialTheme.typography.bodySmall,
                                        color = onCardColor.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    // Input area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("输入消息...", color = onCardColor.copy(alpha = 0.4f)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = onCardColor.copy(alpha = 0.2f),
                                focusedTextColor = onCardColor,
                                unfocusedTextColor = onCardColor,
                                cursorColor = accentColor
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = { sendChatMessage() },
                            enabled = chatInput.isNotBlank() && !isLoading,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Send, "发送")
                        }
                    }
                }
            }
        }
    }
}

// ChatBubble defined in AddEntryScreen.kt — reused
