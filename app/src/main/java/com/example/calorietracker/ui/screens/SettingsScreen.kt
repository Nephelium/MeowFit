package com.example.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.calorietracker.data.UserProfileEntity

import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Update
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

import com.example.calorietracker.data.update.UpdateStatus

@Composable
fun SettingsScreen(
    userProfile: UserProfileEntity?,
    availableExercises: List<String>,
    updateStatus: UpdateStatus = UpdateStatus.Idle,
    onEditProfile: () -> Unit,
    onBackupSettings: () -> Unit,
    onAiSettings: () -> Unit,
    onSystemPromptSettings: () -> Unit,
    onUpdateSleepGoal: (Float) -> Unit,
    onUpdateExcludedExercises: (String) -> Unit,
    onUpdateShowMacros: (Boolean) -> Unit,
    onUpdateTodayThemeIndex: (Int) -> Unit,
    onCheckUpdate: (String) -> Unit = {},
    onDismissUpdateDialog: () -> Unit = {}
) {
    val currentVersion = "1.4.2"
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val selectedThemeIndex = userProfile?.selectedTodayThemeIndex ?: 0
    val selectedTheme = remember(selectedThemeIndex) { getTodayVisualTheme(selectedThemeIndex) }
    val backgroundSeed = remember(selectedThemeIndex) { (selectedThemeIndex + 1) * 1031 }
    val cardColor = remember(selectedTheme, isDarkTheme) { themedDashboardCardColor(selectedTheme, isDarkTheme) }
    val onCardColor = if (isDarkTheme) Color.White else if (calculatePerceivedLuminance(cardColor) > 0.5f) Color(0xFF1E1E1E) else Color(0xFFF4F4F4)
    val accentColor = remember(selectedTheme, isDarkTheme) { themedAccentColor(selectedTheme, isDarkTheme) }
    val sectionTitleColor = if (isDarkTheme) Color.White else Color.Black

    var showSleepDialog by remember { mutableStateOf(false) }
    var showExcludedDialog by remember { mutableStateOf(false) }
    var showTodayThemeDialog by remember { mutableStateOf(false) }
    
    // Auto-show dialog if status changes to something relevant
    if (updateStatus !is UpdateStatus.Idle) {
        CheckUpdateDialog(
            status = updateStatus,
            onDismiss = onDismissUpdateDialog,
            onCheck = { onCheckUpdate(currentVersion) },
            containerColor = cardColor,
            textColor = onCardColor,
            accentColor = accentColor,
            currentVersion = currentVersion
        )
    }

    if (showSleepDialog && userProfile != null) {
        SleepGoalDialog(
            currentGoal = userProfile.sleepGoal,
            onDismiss = { showSleepDialog = false },
            onConfirm = { 
                onUpdateSleepGoal(it)
                showSleepDialog = false
            },
            containerColor = cardColor,
            textColor = onCardColor,
            accentColor = accentColor
        )
    }

    if (showExcludedDialog && userProfile != null) {
        ExcludedExercisesDialog(
            currentExcluded = userProfile.excludedExercises,
            availableExercises = availableExercises,
            onDismiss = { showExcludedDialog = false },
            onConfirm = { 
                onUpdateExcludedExercises(it)
                showExcludedDialog = false
            },
            containerColor = cardColor,
            textColor = onCardColor,
            accentColor = accentColor
        )
    }

    if (showTodayThemeDialog) {
        TodayThemeDialog(
            currentIndex = userProfile?.selectedTodayThemeIndex ?: 0,
            onDismiss = { showTodayThemeDialog = false },
            onConfirm = {
                onUpdateTodayThemeIndex(it)
                showTodayThemeDialog = false
            },
            containerColor = cardColor,
            textColor = onCardColor,
            accentColor = accentColor
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TodayBackground(
            theme = selectedTheme,
            seed = backgroundSeed,
            isDarkTheme = isDarkTheme,
            modifier = Modifier
                .matchParentSize()
                .blur(if (isDarkTheme) 22.dp else 10.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .verticalScroll(rememberScrollState())
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .padding(16.dp)
        ) {
        Text(
            "设置", 
            style = MaterialTheme.typography.headlineMedium, 
            color = onCardColor,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        Text(
            "账户与资料", 
            style = MaterialTheme.typography.titleMedium,
            color = sectionTitleColor,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        
        SettingsItem(
            icon = Icons.Default.Person,
            title = "个人信息",
            subtitle = if (userProfile != null) "${userProfile.name} · 目标: ${userProfile.targetWeight}kg" else "点击完善资料",
            onClick = onEditProfile,
            cardColor = cardColor,
            iconTint = accentColor,
            textColor = onCardColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "目标设置", 
            style = MaterialTheme.typography.titleMedium,
            color = sectionTitleColor,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        SettingsItem(
            icon = Icons.Default.Bedtime,
            title = "每日睡眠目标",
            subtitle = if (userProfile != null) "${userProfile.sleepGoal} 小时" else "未设置",
            onClick = { showSleepDialog = true },
            cardColor = cardColor,
            iconTint = accentColor,
            textColor = onCardColor
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            icon = Icons.Default.Close, // Using Close as Block/Ban icon
            title = "排除运动项目",
            subtitle = if (userProfile != null && userProfile.excludedExercises.isNotEmpty()) 
                "已排除: ${userProfile.excludedExercises}" 
            else "点击选择不计入统计的运动",
            onClick = { showExcludedDialog = true },
            cardColor = cardColor,
            iconTint = accentColor,
            textColor = onCardColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "显示设置", 
            style = MaterialTheme.typography.titleMedium,
            color = sectionTitleColor,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        SwitchSettingsItem(
            icon = Icons.Default.Restaurant,
            title = "显示营养成分",
            subtitle = "在首页和添加记录时显示三大营养素",
            checked = userProfile?.showMacros ?: false,
            onCheckedChange = { onUpdateShowMacros(it) },
            cardColor = cardColor,
            iconTint = accentColor,
            textColor = onCardColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            icon = Icons.Default.Edit,
            title = "主页背景风格",
            subtitle = todayVisualThemePool[(userProfile?.selectedTodayThemeIndex ?: 0).coerceIn(0, todayVisualThemePool.lastIndex)].name,
            onClick = { showTodayThemeDialog = true },
            cardColor = cardColor,
            iconTint = accentColor,
            textColor = onCardColor
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "AI 助手", 
            style = MaterialTheme.typography.titleMedium,
            color = sectionTitleColor,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        SettingsItem(
            icon = Icons.Default.Face,
            title = "API 设置",
            subtitle = "配置 LLM 模型与密钥",
            onClick = onAiSettings,
            cardColor = cardColor,
            iconTint = accentColor,
            textColor = onCardColor
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SettingsItem(
            icon = Icons.Default.Edit,
            title = "系统提示词设置",
            subtitle = "修改 AI 对话与识图提示词",
            onClick = onSystemPromptSettings,
            cardColor = cardColor,
            iconTint = accentColor,
            textColor = onCardColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "数据管理", 
            style = MaterialTheme.typography.titleMedium,
            color = sectionTitleColor,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        SettingsItem(
            icon = Icons.Default.Backup,
            title = "备份与恢复",
            subtitle = "本地自动备份与手动导出",
            onClick = onBackupSettings,
            cardColor = cardColor,
            iconTint = accentColor,
            textColor = onCardColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "关于",
            style = MaterialTheme.typography.titleMedium,
            color = sectionTitleColor,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        SettingsItem(
            icon = Icons.Default.Update,
            title = "检查更新",
            subtitle = "当前版本: $currentVersion",
            onClick = { onCheckUpdate(currentVersion) },
            cardColor = cardColor,
            iconTint = accentColor,
            textColor = onCardColor
        )
    }
    }
}

@Composable
fun CheckUpdateDialog(
    status: UpdateStatus,
    onDismiss: () -> Unit,
    onCheck: () -> Unit,
    containerColor: Color,
    textColor: Color,
    accentColor: Color,
    currentVersion: String
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("检查更新") },
        containerColor = containerColor,
        titleContentColor = textColor,
        textContentColor = textColor,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("当前版本: $currentVersion")
                Text(
                    "近期更新\n" +
                        "1.4.1：新增记录图片上传与预览；优化餐次分组（早餐4:00起，22:00-4:00归宵夜）；首页分享升级为精美长图并支持主题背景、卡片阴影、宏量营养显示与缩略图展示；备份升级为含图片资源的zip方案；完善记录与运动交互并修复保存/分享稳定性问题。\n" +
                        "1.4.0：日历与分享体验优化，界面与主题表现改进，已知问题修复。",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.9f)
                )
                
                when (status) {
                    is UpdateStatus.Idle -> {
                         LaunchedEffect(Unit) {
                             onCheck()
                         }
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             CircularProgressIndicator(
                                 modifier = Modifier.size(24.dp),
                                 color = accentColor
                             )
                             Spacer(modifier = Modifier.width(16.dp))
                             Text("正在连接服务器...")
                         }
                    }
                    is UpdateStatus.Checking -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             CircularProgressIndicator(
                                 modifier = Modifier.size(24.dp),
                                 color = accentColor
                             )
                             Spacer(modifier = Modifier.width(16.dp))
                             Text("正在检查更新...")
                         }
                    }
                    is UpdateStatus.UpdateAvailable -> {
                        Text(
                            "发现新版本: ${status.release.tagName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = accentColor
                        )
                        if (status.release.body.isNotBlank()) {
                            Text(
                                status.release.body,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 10,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(status.release.htmlUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            )
                        ) {
                            Text("去 GitHub 下载")
                        }
                    }
                    is UpdateStatus.NoUpdate -> {
                        Text("当前已是最新版本。", color = accentColor)
                    }
                    is UpdateStatus.Error -> {
                        Text(
                            "检查失败: ${status.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = onCheck,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            )
                        ) {
                            Text("重试")
                        }
                    }
                }
                
                if (status !is UpdateStatus.UpdateAvailable) {
                    Divider()
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://zhuanlan.zhihu.com/p/2015441841684766800")) 
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = accentColor
                        )
                    ) {
                        Text("下载慢？跳转文章查看")
                    }
                    
                    Text(
                        "注意: 作者手动上传至百度网盘的时间可能会有一定的延迟，请耐心等待。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
            ) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    cardColor: Color = MaterialTheme.colorScheme.surface,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = textColor)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.82f))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun TodayThemeDialog(
    currentIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    containerColor: Color,
    textColor: Color,
    accentColor: Color
) {
    var selected by remember(currentIndex) { mutableIntStateOf(currentIndex.coerceIn(0, todayVisualThemePool.lastIndex)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主页背景风格") },
        containerColor = containerColor,
        titleContentColor = textColor,
        textContentColor = textColor,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "选择一个你喜欢的主页背景风格",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.82f)
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(todayVisualThemePool) { index, theme ->
                        val isSelected = selected == index
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = index },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) accentColor.copy(alpha = 0.18f) else textColor.copy(alpha = 0.06f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.width(56.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(11.dp)
                                            .background(Color(theme.lightBgColor), RoundedCornerShape(6.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(11.dp)
                                            .background(Color(theme.lightTopGradientColor), RoundedCornerShape(6.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = theme.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = textColor
                                    )
                                    Text(
                                        text = theme.patternEmoji.take(3).joinToString("  "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor.copy(alpha = 0.82f)
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selected = index },
                                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("应用", color = accentColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = textColor.copy(alpha = 0.82f))
            }
        }
    )
}

@Composable
fun ExcludedExercisesDialog(
    currentExcluded: String,
    availableExercises: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    containerColor: Color,
    textColor: Color,
    accentColor: Color
) {
    // Parse current excluded list
    val currentSet = remember(currentExcluded) {
        if (currentExcluded.isBlank()) emptySet() 
        else currentExcluded.split(",").map { it.trim() }.toSet()
    }
    
    // Track selected items
    val selectedItems = remember { mutableStateListOf<String>().apply { addAll(currentSet) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择排除的运动项目") },
        containerColor = containerColor,
        titleContentColor = textColor,
        textContentColor = textColor,
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                Text(
                    "被选中的项目将不计入日历运动消耗和运动统计，但仍会参与热量缺口计算。",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.82f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (availableExercises.isEmpty()) {
                    Text("暂无运动记录可选", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    LazyColumn {
                        items(availableExercises) { exercise ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedItems.contains(exercise)) {
                                            selectedItems.remove(exercise)
                                        } else {
                                            selectedItems.add(exercise)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedItems.contains(exercise),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedItems.add(exercise) else selectedItems.remove(exercise)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accentColor
                                    )
                                )
                                Text(
                                    text = exercise,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedItems.joinToString(","))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = textColor.copy(alpha = 0.82f))
            }
        }
    )
}

@Composable
fun SleepGoalDialog(
    currentGoal: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
    containerColor: Color,
    textColor: Color,
    accentColor: Color
) {
    var goalStr by remember { mutableStateOf(currentGoal.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置每日睡眠目标") },
        containerColor = containerColor,
        titleContentColor = textColor,
        textContentColor = textColor,
        text = {
            Column {
                OutlinedTextField(
                    value = goalStr,
                    onValueChange = { 
                        goalStr = it
                        error = null
                    },
                    label = { Text("目标时长 (小时)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Text(
                    "建议设置在 7-9 小时之间 (范围: 5-10)",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.82f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val goal = goalStr.toFloatOrNull()
                    if (goal != null && goal > 5 && goal < 10) {
                        onConfirm(goal)
                    } else {
                        error = "请输入大于5且小于10的数值"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = textColor.copy(alpha = 0.82f))
            }
        }
    )
}
