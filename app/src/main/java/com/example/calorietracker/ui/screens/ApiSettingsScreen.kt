package com.example.calorietracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.calorietracker.ui.AiViewModel

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsScreen(
    viewModel: AiViewModel,
    onBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var provider by remember { mutableStateOf(config.provider) }
    var baseUrl by remember { mutableStateOf(config.baseUrl) }
    var modelName by remember { mutableStateOf(config.modelName) }
    var maxContext by remember { mutableIntStateOf(config.maxContext) }

    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    val providers = mapOf(
        "OpenAI" to "https://api.openai.com/v1/",
        "DeepSeek" to "https://api.deepseek.com/",
        "Moonshot (Kimi)" to "https://api.moonshot.cn/v1/",
        "Aliyun (Qwen)" to "https://dashscope.aliyuncs.com/compatible-mode/v1/",
        "Zhipu (GLM)" to "https://open.bigmodel.cn/api/paas/v4/",
        "01.AI (Yi)" to "https://api.lingyiwanwu.com/v1/",
        "Baidu (Ernie)" to "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/", // Note: Baidu API usually requires access token flow, might not work directly with standard OpenAI client format. Keeping for reference or proxy usage.
        "SiliconFlow" to "https://api.siliconflow.cn/v1/",
        "Custom" to ""
    )

    val models = mapOf(
        "OpenAI" to listOf(
            "gpt-5", "gpt-5.4"
        ),
        "Anthropic" to listOf(
            "claude-4.5-opus", "claude-4.6-opus", "claude-4.6-sonnet", "claude-4.6-haiku"
        ),
        "DeepSeek" to listOf(
            "deepseek-chat", "deepseek-reasoner"
        ),
        "Moonshot (Kimi)" to listOf(
            "moonshot-v1-32k", "moonshot-v1-128k","kimi-k2", "kimi-k2.5"
        ),
        "Aliyun (Qwen)" to listOf(
             "qwen-plus", "qwen-max", "qwen3.5-27b"
        ),
        "Zhipu (GLM)" to listOf(
            "glm-5", "glm-4.7", "glm-4.7-flash"
        ),
        "Custom" to emptyList()
    )

    var testStatus by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }

    LaunchedEffect(config) {
        apiKey = config.apiKey
        provider = config.provider
        baseUrl = config.baseUrl
        modelName = config.modelName
        maxContext = config.maxContext
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 设置") },
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
            Text(
                "配置大语言模型 API",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Provider Dropdown
            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = !providerExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = provider,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("服务商") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false }
                ) {
                    providers.keys.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                provider = selectionOption
                                providerExpanded = false
                                if (selectionOption != "Custom") {
                                    baseUrl = providers[selectionOption] ?: ""
                                    // Default to first model
                                    val availableModels = models[selectionOption] ?: emptyList()
                                    if (availableModels.isNotEmpty()) {
                                        modelName = availableModels.first()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("API 地址 (Base URL)") },
                placeholder = { Text("https://api.openai.com/v1/") },
                modifier = Modifier.fillMaxWidth(),
                enabled = true
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            // Model Dropdown or Text Field
            val currentModels = models[provider] ?: emptyList()
            
            // 使用 Box + OutlinedTextField + DropdownMenu 来实现手动输入 + 下拉选择
            // 这种方式比 ExposedDropdownMenuBox 更稳定，避免了输入焦点和删除字符的问题
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("模型名称") },
                    trailingIcon = { 
                        if (currentModels.isNotEmpty()) {
                            IconButton(onClick = { modelExpanded = !modelExpanded }) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (currentModels.isNotEmpty()) {
                    DropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f) // 稍微调整宽度以适配
                    ) {
                        currentModels.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    modelName = selectionOption
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (isTesting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            if (testStatus.isNotEmpty()) {
                Text(
                    text = testStatus,
                    color = if (testStatus.startsWith("成功")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Max Context Setting
            Column {
                Text(
                    text = "上下文长度: $maxContext 条",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "发送给 AI 的历史消息数量，数值越大消耗 Token 越多。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = maxContext.toFloat(),
                    onValueChange = { maxContext = it.toInt() },
                    valueRange = 0f..20f,
                    steps = 19
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            isTesting = true
                            testStatus = "正在测试连接..."
                            try {
                                val success = viewModel.testConnection(apiKey, baseUrl, modelName)
                                testStatus = if (success) "成功: 连接正常" else "失败: 无法连接或认证失败"
                            } catch (e: Exception) {
                                testStatus = "错误: ${e.localizedMessage}"
                            } finally {
                                isTesting = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isTesting
                ) {
                    Text("测试连接")
                }

                Button(
                    onClick = {
                        viewModel.updateConfig(apiKey, provider, baseUrl, modelName, maxContext)
                        onBack()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isTesting
                ) {
                    Text("保存")
                }
            }
            
            Text(
                text = "说明：\n1. 请确保 Base URL 格式正确 (通常以 /v1/ 结尾)。\n2. 模型名称需要与服务商支持的模型一致。\n3. 若要使用识图功能，请确保选择支持多模态能力（Vision）的模型（如 gpt-4o, qwen-vl 等），纯语言模型无法识别图片。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
