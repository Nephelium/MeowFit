package com.example.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun SettingsScreen(
    userProfile: UserProfileEntity?,
    availableExercises: List<String>,
    onEditProfile: () -> Unit,
    onBackupSettings: () -> Unit,
    onAiSettings: () -> Unit,
    onSystemPromptSettings: () -> Unit,
    onUpdateSleepGoal: (Float) -> Unit,
    onUpdateExcludedExercises: (String) -> Unit,
    onUpdateShowMacros: (Boolean) -> Unit
) {
    var showSleepDialog by remember { mutableStateOf(false) }
    var showExcludedDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    if (showUpdateDialog) {
        CheckUpdateDialog(onDismiss = { showUpdateDialog = false })
    }

    if (showSleepDialog && userProfile != null) {
        SleepGoalDialog(
            currentGoal = userProfile.sleepGoal,
            onDismiss = { showSleepDialog = false },
            onConfirm = { 
                onUpdateSleepGoal(it)
                showSleepDialog = false
            }
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
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "设置", 
            style = MaterialTheme.typography.headlineMedium, 
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        Text(
            "账户与资料", 
            style = MaterialTheme.typography.labelMedium, 
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        
        SettingsItem(
            icon = Icons.Default.Person,
            title = "个人信息",
            subtitle = if (userProfile != null) "${userProfile.name} · 目标: ${userProfile.targetWeight}kg" else "点击完善资料",
            onClick = onEditProfile
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "目标设置", 
            style = MaterialTheme.typography.labelMedium, 
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        SettingsItem(
            icon = Icons.Default.Bedtime,
            title = "每日睡眠目标",
            subtitle = if (userProfile != null) "${userProfile.sleepGoal} 小时" else "未设置",
            onClick = { showSleepDialog = true }
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem(
            icon = Icons.Default.Close, // Using Close as Block/Ban icon
            title = "排除运动项目",
            subtitle = if (userProfile != null && userProfile.excludedExercises.isNotEmpty()) 
                "已排除: ${userProfile.excludedExercises}" 
            else "点击选择不计入统计的运动",
            onClick = { showExcludedDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "显示设置", 
            style = MaterialTheme.typography.labelMedium, 
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        SwitchSettingsItem(
            icon = Icons.Default.Restaurant,
            title = "显示营养成分",
            subtitle = "在首页和添加记录时显示三大营养素",
            checked = userProfile?.showMacros ?: false,
            onCheckedChange = { onUpdateShowMacros(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "AI 助手", 
            style = MaterialTheme.typography.labelMedium, 
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        SettingsItem(
            icon = Icons.Default.Face,
            title = "API 设置",
            subtitle = "配置 LLM 模型与密钥",
            onClick = onAiSettings
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SettingsItem(
            icon = Icons.Default.Edit,
            title = "系统提示词设置",
            subtitle = "修改 AI 对话与识图提示词",
            onClick = onSystemPromptSettings
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "数据管理", 
            style = MaterialTheme.typography.labelMedium, 
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        SettingsItem(
            icon = Icons.Default.Backup,
            title = "备份与恢复",
            subtitle = "本地自动备份与手动导出",
            onClick = onBackupSettings
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "关于",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        SettingsItem(
            icon = Icons.Default.Update,
            title = "检查更新",
            subtitle = "当前版本: 1.3.2",
            onClick = { showUpdateDialog = true }
        )
    }
}

@Composable
fun CheckUpdateDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("检查更新") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("当前版本: 1.3.2")
                Text("请前往 GitHub 查看是否有新版本。")
                
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Nephelium/MeowFit/releases/latest"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("去 GitHub 下载")
                }
                
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://zhuanlan.zhihu.com/p/2015441841684766800")) 
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("下载慢？跳转文章查看")
                }
                
                Text(
                    "注意: 作者手动上传至百度网盘的时间可能会有一定的延迟，请耐心等待。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun ExcludedExercisesDialog(
    currentExcluded: String,
    availableExercises: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
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
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                Text(
                    "被选中的项目将不计入日历运动消耗和运动统计，但仍会参与热量缺口计算。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    }
                                )
                                Text(
                                    text = exercise,
                                    style = MaterialTheme.typography.bodyMedium,
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
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun SleepGoalDialog(currentGoal: Float, onDismiss: () -> Unit, onConfirm: (Float) -> Unit) {
    var goalStr by remember { mutableStateOf(currentGoal.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置每日睡眠目标") },
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
