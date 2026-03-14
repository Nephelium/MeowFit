package com.example.calorietracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.calorietracker.ui.BackupViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    viewModel: BackupViewModel,
    selectedThemeIndex: Int = 0,
    onBack: () -> Unit
) {
    val status by viewModel.status.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val lastAutoBackup by viewModel.lastAutoBackupTime.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()
    val selectedTheme = remember(selectedThemeIndex) { getTodayVisualTheme(selectedThemeIndex) }
    val accentColor = remember(selectedTheme, isDarkTheme) { themedAccentColor(selectedTheme, isDarkTheme) }
    var showClearDialog by remember { mutableStateOf(false) }
    var clearCountdown by remember { mutableIntStateOf(0) }
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.performManualBackup(it) }
    }

    // Launcher for opening a file (Restore)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreBackup(it) }
    }

    LaunchedEffect(showClearDialog) {
        if (showClearDialog) {
            clearCountdown = 5
            while (clearCountdown > 0) {
                delay(1000)
                clearCountdown--
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("备份与恢复") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Auto Backup Info
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("自动备份", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("系统每天会自动备份当前数据，覆盖前一天的自动备份文件。", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("上次自动备份: $lastAutoBackup", style = MaterialTheme.typography.bodySmall, color = accentColor)
                }
            }

            Divider()

            // Manual Actions
            Text("手动操作", style = MaterialTheme.typography.titleMedium, color = accentColor)

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = accentColor)
            }

            Text(
                text = status,
                color = if (status.contains("成功")) accentColor else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = {
                    viewModel.performQuickBackup()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                Text("快速备份到 Downloads/MeowFit")
            }

            Button(
                onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    createDocumentLauncher.launch("calorie_tracker_backup_$timestamp.zip")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                Text("手动备份 (选择位置)")
            }
            
            OutlinedButton(
                onClick = {
                    openDocumentLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = accentColor
                )
            ) {
                Text("从文件恢复")
            }
            
            Text(
                text = "注意：恢复操作将覆盖当前的同名记录，请谨慎操作。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

            Button(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("清空所有数据及缓存")
            }

            Text(
                text = "清空后将只能从备份恢复。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("确认清空所有数据？", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Text(
                        text = "此操作会清空所有记录、用户资料、聊天记录与本地缓存，且不可撤销。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearDialog = false
                            viewModel.clearAllDataAndCache()
                        },
                        enabled = clearCountdown == 0 && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        if (clearCountdown > 0) {
                            Text("再次确认 (${clearCountdown}s)")
                        } else {
                            Text("再次确认并清空")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("取消", color = accentColor)
                    }
                }
            )
        }
    }
}
