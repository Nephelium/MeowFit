package com.example.calorietracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.calorietracker.data.ai.AiService
import com.example.calorietracker.ui.AiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemPromptSettingsScreen(
    viewModel: AiViewModel,
    selectedThemeIndex: Int = 0,
    onBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()
    val selectedTheme = remember(selectedThemeIndex) { getTodayVisualTheme(selectedThemeIndex) }
    val accentColor = remember(selectedTheme, isDarkTheme) { themedAccentColor(selectedTheme, isDarkTheme) }

    var chatPrompt by remember { mutableStateOf(config.customChatPrompt ?: AiService.DEFAULT_CHAT_PROMPT) }
    var imagePrompt by remember { mutableStateOf(config.customImagePrompt ?: AiService.DEFAULT_IMAGE_PROMPT) }
    var analysisPrompt by remember { mutableStateOf(config.customAnalysisPrompt ?: AiService.DEFAULT_ANALYSIS_PROMPT) }

    LaunchedEffect(config) {
        if (config.customChatPrompt != null) chatPrompt = config.customChatPrompt!!
        if (config.customImagePrompt != null) imagePrompt = config.customImagePrompt!!
        if (config.customAnalysisPrompt != null) analysisPrompt = config.customAnalysisPrompt!!
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("系统提示词设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "修改提示词可能会导致 AI 识别准确率下降或格式错误。请谨慎修改。",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(
                "系统会自动追加用户体重和固定 JSON 记录协议；即使自定义提示词，也不会再覆盖卡片所需的返回结构。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tab row for three prompt types
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = accentColor
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("对话", style = MaterialTheme.typography.labelSmall) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("识图", style = MaterialTheme.typography.labelSmall) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    text = { Text("周报分析", style = MaterialTheme.typography.labelSmall) })
            }

            when (selectedTab) {
                0 -> {
                    OutlinedTextField(
                        value = chatPrompt,
                        onValueChange = { chatPrompt = it },
                        label = { Text("AI 对话提示词") },
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
                1 -> {
                    OutlinedTextField(
                        value = imagePrompt,
                        onValueChange = { imagePrompt = it },
                        label = { Text("识图提示词") },
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
                2 -> {
                    OutlinedTextField(
                        value = analysisPrompt,
                        onValueChange = { analysisPrompt = it },
                        label = { Text("周报分析提示词") },
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        when (selectedTab) {
                            0 -> chatPrompt = AiService.DEFAULT_CHAT_PROMPT
                            1 -> imagePrompt = AiService.DEFAULT_IMAGE_PROMPT
                            2 -> analysisPrompt = AiService.DEFAULT_ANALYSIS_PROMPT
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("重置当前")
                }

                Button(
                    onClick = {
                        viewModel.updateConfig(
                            config.apiKey, config.provider, config.baseUrl, config.modelName,
                            config.maxContext,
                            chatPrompt, imagePrompt, analysisPrompt
                        )
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存修改")
                }
            }
        }
    }
}
