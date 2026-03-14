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

    LaunchedEffect(config) {
        if (config.customChatPrompt != null) {
            chatPrompt = config.customChatPrompt!!
        }
        if (config.customImagePrompt != null) {
            imagePrompt = config.customImagePrompt!!
        }
    }

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

            Text("注意：系统会自动在提示词末尾追加用户的体重信息，无需手动添加。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = chatPrompt,
                onValueChange = { chatPrompt = it },
                label = { Text("AI 对话提示词") },
                modifier = Modifier.fillMaxWidth().height(300.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )
            
            OutlinedTextField(
                value = imagePrompt,
                onValueChange = { imagePrompt = it },
                label = { Text("识图提示词") },
                modifier = Modifier.fillMaxWidth().height(300.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        // Reset to defaults
                        chatPrompt = AiService.DEFAULT_CHAT_PROMPT
                        imagePrompt = AiService.DEFAULT_IMAGE_PROMPT
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accentColor
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("重置默认")
                }
                
                Button(
                    onClick = {
                        viewModel.updateConfig(
                            config.apiKey,
                            config.provider,
                            config.baseUrl,
                            config.modelName,
                            config.maxContext,
                            chatPrompt,
                            imagePrompt
                        )
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存修改")
                }
            }
        }
    }
}
