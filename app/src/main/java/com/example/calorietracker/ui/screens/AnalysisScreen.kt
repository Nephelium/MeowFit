package com.example.calorietracker.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerApp
import com.example.calorietracker.data.*
import com.example.calorietracker.data.ai.AiService
import com.example.calorietracker.ui.AiViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.*

// --- Week helper functions ---

fun getWeekMonday(date: LocalDate): LocalDate {
    return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

fun getWeekSunday(monday: LocalDate): LocalDate {
    return monday.plusDays(6)
}

fun getWeekLabel(weekStart: String): String {
    val monday = try { LocalDate.parse(weekStart) } catch (_: Exception) { return weekStart }
    val sunday = getWeekSunday(monday)
    val fmt = DateTimeFormatter.ofPattern("MM/dd")
    return "${monday.format(fmt)} - ${sunday.format(fmt)}"
}

fun getCurrentWeekMonday(): LocalDate {
    return getWeekMonday(LocalDate.now())
}

fun getPastWeeks(count: Int): List<LocalDate> {
    val currentMonday = getCurrentWeekMonday()
    return (0 until count).map { currentMonday.minusWeeks(it.toLong()) }
}

fun buildWeeklyDataText(
    weekStart: String,
    records: List<DailyRecordEntity>,
    allItems: List<CalorieItemEntity>,
    userProfile: UserProfileEntity?
): String {
    val monday = LocalDate.parse(weekStart)
    val sunday = getWeekSunday(monday)

    val weekRecords = records.filter { r ->
        try {
            val d = LocalDate.parse(r.date)
            !d.isBefore(monday) && !d.isAfter(sunday)
        } catch (_: Exception) { false }
    }

    val weekItems = allItems.filter { i ->
        try {
            val d = LocalDate.parse(i.date)
            !d.isBefore(monday) && !d.isAfter(sunday)
        } catch (_: Exception) { false }
    }

    val sb = StringBuilder()
    val profile = userProfile
    sb.appendLine("用户信息：")
    if (profile != null) {
        sb.appendLine("- 性别：${if (profile.gender == "male") "男" else "女"}")
        sb.appendLine("- 年龄：${profile.age}岁")
        sb.appendLine("- 身高：${profile.height}cm")
        sb.appendLine("- 当前体重：${profile.weight}kg")
        sb.appendLine("- 目标体重：${profile.targetWeight}kg")
        sb.appendLine("- 目标：${when(profile.goal) { "lose" -> "减脂"; "gain" -> "增肌"; else -> "保持" }}")
        sb.appendLine("- 每日目标热量：${profile.dailyCalorieTarget}kcal")
        sb.appendLine("- 睡眠目标：${profile.sleepGoal}小时")
    }
    sb.appendLine()

    sb.appendLine("=== 本周数据（${weekStart} ~ ${sunday}）===")

    val fmt = DateTimeFormatter.ofPattern("MM/dd")
    var day = monday
    while (!day.isAfter(sunday)) {
        val dateStr = day.toString()
        val record = weekRecords.find { it.date == dateStr }
        val dayItems = weekItems.filter { it.date == dateStr }
        val foodItems = dayItems.filter { it.type == "food" }
        val exerciseItems = dayItems.filter { it.type == "exercise" }

        sb.appendLine("--- ${day.format(fmt)} (${day.dayOfWeek}) ---")
        if (record != null) {
            sb.appendLine("  饮食热量：${record.totalIntake}kcal")
            sb.appendLine("  运动消耗：${record.totalBurned}kcal")
            sb.appendLine("  碳水：${record.totalCarbs}g  蛋白质：${record.totalProtein}g  脂肪：${record.totalFat}g")
            sb.appendLine("  饮水：${record.totalWater}ml")
            val sleepH = record.sleepDuration / 60
            val sleepM = record.sleepDuration % 60
            sb.appendLine("  睡眠：${sleepH}h${sleepM}m")
            record.weight?.let { w ->
                sb.appendLine("  体重：${"%.1f".format(w)}kg")
            }

            if (foodItems.isNotEmpty()) {
                sb.appendLine("  饮食记录：")
                foodItems.forEach { item ->
                    sb.appendLine("    - ${item.name} ${"%.0f".format(item.calories)}kcal (${item.time})")
                }
            }
            if (exerciseItems.isNotEmpty()) {
                sb.appendLine("  运动记录：")
                exerciseItems.forEach { item ->
                    sb.appendLine("    - ${item.name} ${"%.0f".format(item.calories)}kcal (${item.time})")
                }
            }
        } else {
            sb.appendLine("  （今日无记录）")
        }
        day = day.plusDays(1)
    }

    return sb.toString()
}

// --- AnalysisScreen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    records: List<DailyRecordEntity>,
    allItems: List<CalorieItemEntity>,
    userProfile: UserProfileEntity?,
    aiViewModel: AiViewModel,
    selectedThemeIndex: Int,
    onNavigateToDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as CalorieTrackerApp
    val analysisDao = remember {
        try {
            app.database.analysisDao()
        } catch (e: Exception) {
            android.util.Log.e("AnalysisScreen", "Failed to get analysisDao", e)
            null
        }
    }
    val aiService = remember { AiService(context) }

    val isDarkTheme = isSystemInDarkTheme()
    val selectedTheme = remember(selectedThemeIndex) { getTodayVisualTheme(selectedThemeIndex) }
    val cardColor = remember(selectedTheme, isDarkTheme) { themedDashboardCardColor(selectedTheme, isDarkTheme) }
    val onCardColor = com.example.calorietracker.ui.theme.onCardColor(cardColor, isDarkTheme)
    val accentColor = remember(selectedTheme, isDarkTheme) { themedAccentColor(selectedTheme, isDarkTheme) }

    var summaries by remember { mutableStateOf<List<WeeklySummaryEntity>>(emptyList()) }
    var summariesLoadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val dao = analysisDao ?: return@LaunchedEffect
        try {
            dao.getAllSummaries()
                .catch { e ->
                    android.util.Log.e("AnalysisScreen", "DB query failed", e)
                    summariesLoadFailed = true
                    emit(emptyList())
                }
                .collect { list ->
                    summaries = list
                    summariesLoadFailed = false
                }
        } catch (e: Exception) {
            android.util.Log.e("AnalysisScreen", "Flow collection failed", e)
            summariesLoadFailed = true
        }
    }
    val scope = rememberCoroutineScope()

    var showConsentDialog by remember { mutableStateOf(false) }
    var generatingWeek by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var autoGenerateAfterConsent by remember { mutableStateOf(false) }

    // Shared generation logic — used by buttons and auto-generation
    suspend fun generateAllWeeks(dao: AnalysisDao) {
        val pastWeeks = getPastWeeks(12)
        for (monday in pastWeeks) {
            val weekStartStr = monday.toString()
            val existing = dao.getSummary(weekStartStr)
            if (existing != null && existing.status == "generated") continue

            val sunday = getWeekSunday(monday)
            val weekRecords = records.filter { r ->
                try {
                    val d = LocalDate.parse(r.date)
                    !d.isBefore(monday) && !d.isAfter(sunday)
                } catch (_: Exception) { false }
            }
            val dietDays = weekRecords.count { it.totalIntake > 0 }
            val exerciseDays = weekRecords.count { it.totalBurned > 0 }
            if (dietDays + exerciseDays < 2) continue

            generatingWeek = weekStartStr
            val dataText = buildWeeklyDataText(weekStartStr, records, allItems, userProfile)
            val result = aiService.generateWeeklyAnalysis(dataText)
            dao.insertSummary(
                WeeklySummaryEntity(
                    weekStartDate = weekStartStr,
                    weekEndDate = sunday.toString(),
                    summaryText = result,
                    recommendations = "",
                    dietDays = dietDays,
                    exerciseDays = exerciseDays,
                    generatedAt = System.currentTimeMillis(),
                    status = "generated"
                )
            )
        }
    }

    // Check if first visit - show consent dialog
    val consentKey = "analysis_consent_given"
    val prefs = remember { context.getSharedPreferences("analysis_prefs", android.content.Context.MODE_PRIVATE) }
    val consentGiven = remember { prefs.getBoolean(consentKey, false) }

    LaunchedEffect(Unit) {
        if (!consentGiven) {
            showConsentDialog = true
        }
    }

    // Auto-generate after user consents
    LaunchedEffect(autoGenerateAfterConsent) {
        if (!autoGenerateAfterConsent) return@LaunchedEffect
        val dao = analysisDao ?: return@LaunchedEffect
        try {
            generateAllWeeks(dao)
        } catch (e: Exception) {
            errorMessage = "生成失败：${e.localizedMessage}"
        } finally {
            generatingWeek = null
            autoGenerateAfterConsent = false
        }
    }

    // Consent dialog
    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = {
                Text("📊 开启每周数据分析？", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "分析板块会读取你每周的饮食、运动、睡眠、饮水和体重数据，" +
                    "通过 AI 为你生成详细的周报分析，包括健康评估、饮食建议和运动计划。\n\n" +
                    "同意后，每周一系统会自动为你生成上周的总结。你也可以随时手动生成。\n\n" +
                    "是否开启？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean(consentKey, true).apply()
                    showConsentDialog = false
                    autoGenerateAfterConsent = true
                }) {
                    Text("开始分析", color = accentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConsentDialog = false
                }) {
                    Text("暂不开启")
                }
            },
            containerColor = cardColor,
            titleContentColor = onCardColor,
            textContentColor = onCardColor.copy(alpha = 0.8f)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "📊 每周分析",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = onCardColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "AI 综合分析你的健康数据，提供个性化建议",
            style = MaterialTheme.typography.bodySmall,
            color = onCardColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Generating banner
        if (generatingWeek != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "⏳ 正在生成周报...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Text(
                            "请勿离开此页面，否则当前周可能生成失败",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // --- Main content: gated by if-else (no early returns to avoid Compose slot corruption) ---

        if (analysisDao == null) {
            // Database error state
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚠️ 数据库初始化失败", color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("请尝试重启应用或清除应用数据", color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        } else if (summariesLoadFailed) {
            // Data load failed state
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚠️ 数据加载失败", color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("周报数据暂时无法加载，请检查数据库是否正常", color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        } else if (!consentGiven && summaries.isEmpty()) {
            // Empty state (no consent)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📊", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "尚未开启每周分析",
                        style = MaterialTheme.typography.titleMedium,
                        color = onCardColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "点击上方按钮开启 AI 周报分析",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onCardColor.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showConsentDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                    ) {
                        Text("开启分析")
                    }
                }
            }
        } else {
            // --- Normal content: consent given, DB ok, load succeeded ---

            // "生成未总结的周" — prominent button
            Button(
                onClick = {
                    scope.launch {
                        val dao = analysisDao ?: return@launch
                        try {
                            generateAllWeeks(dao)
                        } catch (e: Exception) {
                            errorMessage = "生成失败：${e.localizedMessage}"
                        } finally {
                            generatingWeek = null
                        }
                    }
                },
                enabled = generatingWeek == null,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (generatingWeek != null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (generatingWeek != null) "生成中..." else "生成未总结的周",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Error message
            errorMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (summaries.isEmpty()) {
                // Empty summaries: show generate button card
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "还没有周报",
                            style = MaterialTheme.typography.titleMedium,
                            color = onCardColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "条件：当周至少有2天饮食或运动记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = onCardColor.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val dao = analysisDao ?: return@launch
                                    try {
                                        generateAllWeeks(dao)
                                    } catch (e: Exception) {
                                        errorMessage = "生成失败：${e.localizedMessage}"
                                    } finally {
                                        generatingWeek = null
                                    }
                                }
                            },
                            enabled = generatingWeek == null,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (generatingWeek != null) "生成中..." else "生成周报")
                        }
                    }
                }
            } else {
                // Weekly summary cards
                summaries.forEach { summary ->
                    WeeklySummaryCard(
                        summary = summary,
                        accentColor = accentColor,
                        cardColor = cardColor,
                        onCardColor = onCardColor,
                        onClick = { onNavigateToDetail(summary.weekStartDate) },
                        onDelete = {
                            scope.launch {
                                val dao = analysisDao ?: return@launch
                                try {
                                    dao.deleteSummary(summary.weekStartDate)
                                } catch (e: Exception) {
                                    errorMessage = "删除失败：${e.localizedMessage}"
                                }
                            }
                        },
                        onRetry = {
                            scope.launch {
                                val dao = analysisDao ?: return@launch
                                try {
                                    generatingWeek = summary.weekStartDate
                                    val dataText = buildWeeklyDataText(summary.weekStartDate, records, allItems, userProfile)
                                    val result = aiService.generateWeeklyAnalysis(dataText)
                                    dao.insertSummary(
                                        summary.copy(
                                            summaryText = result,
                                            generatedAt = System.currentTimeMillis(),
                                            status = "generated"
                                        )
                                    )
                                    generatingWeek = null
                                } catch (e: Exception) {
                                    generatingWeek = null
                                    errorMessage = "重试失败：${e.localizedMessage}"
                                }
                            }
                        },
                        onReanalyze = {
                            scope.launch {
                                val dao = analysisDao ?: return@launch
                                try {
                                    generatingWeek = summary.weekStartDate
                                    val dataText = buildWeeklyDataText(summary.weekStartDate, records, allItems, userProfile)
                                    val result = aiService.generateWeeklyAnalysis(dataText)
                                    dao.insertSummary(
                                        summary.copy(
                                            summaryText = result,
                                            generatedAt = System.currentTimeMillis(),
                                            status = "generated"
                                        )
                                    )
                                    generatingWeek = null
                                } catch (e: Exception) {
                                    generatingWeek = null
                                    errorMessage = "重新分析失败：${e.localizedMessage}"
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklySummaryCard(
    summary: WeeklySummaryEntity,
    accentColor: Color,
    cardColor: Color,
    onCardColor: Color,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onReanalyze: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReanalyzeConfirm by remember { mutableStateOf(false) }

    // Re-analyze confirmation dialog
    if (showReanalyzeConfirm) {
        AlertDialog(
            onDismissRequest = { showReanalyzeConfirm = false },
            title = { Text("重新分析", color = onCardColor) },
            text = { Text("确定要重新分析 ${getWeekLabel(summary.weekStartDate)} 的周报吗？将调用 AI 重新生成分析内容。", color = onCardColor.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    showReanalyzeConfirm = false
                    onReanalyze()
                }) { Text("重新分析", color = accentColor) }
            },
            dismissButton = {
                TextButton(onClick = { showReanalyzeConfirm = false }) { Text("取消", color = onCardColor.copy(alpha = 0.6f)) }
            },
            containerColor = cardColor,
            titleContentColor = onCardColor,
            textContentColor = onCardColor.copy(alpha = 0.8f)
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除周报", color = onCardColor) },
            text = { Text("确定要删除 ${getWeekLabel(summary.weekStartDate)} 的周报吗？删除后可以重新生成。", color = onCardColor.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消", color = onCardColor.copy(alpha = 0.6f)) }
            },
            containerColor = cardColor,
            titleContentColor = onCardColor,
            textContentColor = onCardColor.copy(alpha = 0.8f)
        )
    }

    val dismissState = rememberDismissState(
        confirmValueChange = { value ->
            when (value) {
                DismissValue.DismissedToStart -> {
                    showDeleteConfirm = true
                    false // don't actually dismiss, show dialog instead
                }
                DismissValue.DismissedToEnd -> {
                    showReanalyzeConfirm = true
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismiss(
        state = dismissState,
        background = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                // Left side: re-analyze (revealed on right-swipe)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(accentColor.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "重新分析",
                        tint = Color.White,
                        modifier = Modifier.padding(start = 24.dp)
                    )
                }
                // Right side: delete (revealed on left-swipe)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 24.dp)
                    )
                }
            }
        },
        dismissContent = {
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 1f)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable { if (summary.status == "generated") onClick() }
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "📅 ${getWeekLabel(summary.weekStartDate)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = onCardColor
                    )
                    if (summary.status == "failed") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "生成失败",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                if (summary.status == "generated") {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = onCardColor.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "🍽 饮食 ${summary.dietDays}天",
                    style = MaterialTheme.typography.labelSmall,
                    color = onCardColor.copy(alpha = 0.6f)
                )
                Text(
                    "🏃 运动 ${summary.exerciseDays}天",
                    style = MaterialTheme.typography.labelSmall,
                    color = onCardColor.copy(alpha = 0.6f)
                )
            }

            if (summary.status == "generated") {
                Spacer(modifier = Modifier.height(4.dp))
                // Show first few lines of summary as preview with markdown
                val preview = summary.summaryText.trim().take(80)
                MarkdownText(
                    text = preview + if (summary.summaryText.length > 80) "..." else "",
                    baseColor = onCardColor.copy(alpha = 0.7f),
                    baseFontSize = 11f,
                    lineHeight = 15f,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (summary.status == "failed") {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                ) {
                    Text("重新生成", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
    }) // SwipeToDismiss
}
