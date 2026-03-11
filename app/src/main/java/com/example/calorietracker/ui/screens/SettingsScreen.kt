package com.example.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Face
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun SettingsScreen(
    userProfile: UserProfileEntity?,
    onEditProfile: () -> Unit,
    onBackupSettings: () -> Unit,
    onAiSettings: () -> Unit,
    onSystemPromptSettings: () -> Unit,
    onUpdateSleepGoal: (Float) -> Unit
) {
    var showSleepDialog by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
    }
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
