package com.example.calorietracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.calorietracker.data.CalorieItemEntity
import com.example.calorietracker.data.DailyRecordEntity
import com.example.calorietracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

import com.example.calorietracker.data.UserProfileEntity
import com.example.calorietracker.util.CalorieUtils
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.View
import androidx.compose.ui.platform.ComposeView
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast

enum class HeatmapMetric(val label: String) {
    Sleep("睡眠"),
    Water("饮水"),
    Net("热量缺口"),
    Intake("饮食"),
    Burned("运动"),
    Weight("体重")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    records: List<DailyRecordEntity>,
    allItems: List<CalorieItemEntity>,
    userProfile: UserProfileEntity?,
    onAddRecord: (String) -> Unit,
    onUpdateWeight: (Float, String) -> Unit,
    onUpdateWater: (Int, String) -> Unit,
    onUpdateSleep: (Int, String) -> Unit,
    detailDate: String?,
    detailItems: List<CalorieItemEntity>,
    onDetailDateChange: (String?) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val selectedThemeIndex = userProfile?.selectedTodayThemeIndex ?: 0
    val selectedTheme = remember(selectedThemeIndex) { getTodayVisualTheme(selectedThemeIndex) }
    val backgroundSeed = remember(selectedThemeIndex) { (selectedThemeIndex + 1) * 1031 }
    val cardColor = remember(selectedTheme, isDarkTheme) { themedDashboardCardColor(selectedTheme, isDarkTheme) }
    val onCardColor = if (isDarkTheme) Color.White else if (calculatePerceivedLuminance(cardColor) > 0.5f) Color(0xFF1E1E1E) else Color(0xFFF4F4F4)
    val accentColor = remember(selectedTheme, isDarkTheme) { themedAccentColor(selectedTheme, isDarkTheme) }

    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    // Default to current month
    var selectedMonth by remember { mutableStateOf<Int?>(Calendar.getInstance().get(Calendar.MONTH)) }
    
    var heatmapMetric by remember { mutableStateOf(HeatmapMetric.Net) }
    var previewCalendarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val recordMap = remember(records) { records.associateBy { it.date } }
    val context = LocalContext.current

    if (detailDate != null) {
        val record = recordMap[detailDate]
        DayDetailDialog(
            date = detailDate,
            record = record,
            records = records,
            items = detailItems,
            userProfile = userProfile,
            metric = heatmapMetric,
            accentColor = accentColor,
            cardColor = cardColor,
            onCardColor = onCardColor,
            onDismiss = { onDetailDateChange(null) },
            onAddRecord = { onAddRecord(detailDate); onDetailDateChange(null) },
            onUpdateWeight = { w -> onUpdateWeight(w, detailDate) },
            onUpdateWater = { w -> onUpdateWater(w, detailDate) },
            onUpdateSleep = { s -> onUpdateSleep(s, detailDate) }
        )
    }

    if (previewCalendarBitmap != null) {
        Dialog(onDismissRequest = { previewCalendarBitmap = null }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Image(
                        bitmap = previewCalendarBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp, max = 520.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { previewCalendarBitmap = null }) {
                            Text("取消")
                        }
                        TextButton(onClick = {
                            saveCalendarToGallery(context, previewCalendarBitmap!!)
                            previewCalendarBitmap = null
                        }) {
                            Text("保存到相册")
                        }
                        TextButton(onClick = {
                            shareCalendarImage(context, previewCalendarBitmap!!)
                            previewCalendarBitmap = null
                        }) {
                            Text("分享给朋友")
                        }
                    }
                }
            }
        }
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
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    .padding(16.dp)
            ) {
            OverviewTopBar(
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                onYearChange = { selectedYear = it },
                onBackToYear = { selectedMonth = null },
                onBackToMonth = {
                    val cal = Calendar.getInstance()
                    selectedYear = cal.get(Calendar.YEAR)
                    selectedMonth = cal.get(Calendar.MONTH)
                },
                onShare = {
                    previewCalendarBitmap = generateCalendarBitmap(
                        context = context,
                        year = selectedYear,
                        month = selectedMonth,
                        records = records,
                        allItems = allItems,
                        metric = heatmapMetric,
                        userProfile = userProfile
                    )
                },
                containerColor = Color.Transparent,
                titleContentColor = onCardColor,
                accentColor = accentColor,
                modifier = Modifier
            )

            // Heatmap Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedMonth == null) "统计" else "${selectedMonth!! + 1}月",
                            style = MaterialTheme.typography.titleMedium,
                            color = onCardColor.copy(alpha = 0.8f)
                        )
                        
                        // Metric Toggle
                        Row(
                            modifier = Modifier
                                .background(onCardColor.copy(alpha = 0.12f), CircleShape)
                                .padding(2.dp)
                        ) {
                            HeatmapMetric.values().forEach { metric ->
                                val selected = heatmapMetric == metric
                                val bgColor = if (selected) accentColor else Color.Transparent
                                val contentColor = if (selected) Color.White else onCardColor.copy(alpha = 0.8f)
                                
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(bgColor)
                                        .clickable { heatmapMetric = metric }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = metric.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (selectedMonth == null) {
                        YearHeatmapView(
                            year = selectedYear,
                            records = records,
                            allItems = allItems,
                            recordMap = recordMap,
                            userProfile = userProfile,
                            metric = heatmapMetric,
                            onMonthClick = { selectedMonth = it },
                            onDayClick = { onDetailDateChange(it) }
                        )
                    } else {
                        MonthHeatmapView(
                            year = selectedYear,
                            month = selectedMonth!!,
                            records = records,
                            allItems = allItems,
                            recordMap = recordMap,
                            userProfile = userProfile,
                            metric = heatmapMetric,
                            onDayClick = { onDetailDateChange(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Chart Card
            AnimatedVisibility(visible = selectedMonth != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "体重趋势",
                            style = MaterialTheme.typography.titleMedium,
                            color = onCardColor.copy(alpha = 0.86f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        SmoothWeightChart(
                            year = selectedYear,
                            month = selectedMonth,
                            records = records,
                            accentColor = accentColor,
                            contentColor = onCardColor
                        )
                    }
                }
            }
            
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewTopBar(
    selectedYear: Int,
    selectedMonth: Int?,
    onYearChange: (Int) -> Unit,
    onBackToYear: () -> Unit,
    onBackToMonth: () -> Unit,
    onShare: () -> Unit,
    containerColor: Color,
    titleContentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = {
            if (selectedMonth != null) {
                Text("${selectedYear}年 ${selectedMonth + 1}月", fontWeight = FontWeight.Bold)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onYearChange(selectedYear - 1) }) {
                        Icon(Icons.Default.ChevronLeft, "Prev Year", tint = accentColor)
                    }
                    Text("$selectedYear", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { onYearChange(selectedYear + 1) }) {
                        Icon(Icons.Default.ChevronRight, "Next Year", tint = accentColor)
                    }
                }
            }
        },
        navigationIcon = {
            if (selectedMonth != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToYear) {
                        Icon(Icons.Default.ChevronLeft, "Back", tint = accentColor)
                    }
                    Text(
                        text = "年视图",
                        style = MaterialTheme.typography.bodySmall,
                        color = titleContentColor.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onBackToYear() }
                    )
                }
            } else {
                // Year View: Show Back to Month View
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToMonth) {
                        Icon(Icons.Default.ChevronLeft, "Back", tint = accentColor)
                    }
                    Text(
                        text = "月视图",
                        style = MaterialTheme.typography.bodySmall,
                        color = titleContentColor.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onBackToMonth() }
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, "Share", tint = accentColor)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = containerColor,
            titleContentColor = titleContentColor
        )
    )
}

fun generateCalendarBitmap(
    context: android.content.Context,
    year: Int,
    month: Int?,
    records: List<DailyRecordEntity>,
    allItems: List<CalorieItemEntity>,
    metric: HeatmapMetric,
    userProfile: UserProfileEntity?
): Bitmap {
    // 1. Calculate stats
    fun getTarget(date: String): Int {
        return if (userProfile != null) {
            val effectiveWeight = CalorieUtils.getEffectiveWeight(date, records, userProfile)
            CalorieUtils.calculateDailyTarget(
                gender = userProfile.gender,
                weight = effectiveWeight,
                height = userProfile.height,
                age = userProfile.age,
                activityLevel = userProfile.activityLevel,
                goal = userProfile.goal
            )
        } else 2000
    }

    val relevantRecords = records.filter { record ->
        val parts = record.date.split("-")
        if (parts.size == 3) {
            val rYear = parts[0].toInt()
            val rMonth = parts[1].toInt() - 1
            if (month == null) rYear == year else rYear == year && rMonth == month
        } else false
    }

    // Weight Stats
    val validWeights = records.filter { 
        val parts = it.date.split("-")
        if (parts.size == 3) {
            parts[0].toInt() == year
        } else false
    }.mapNotNull { it.weight }.filter { it > 0 }
    val minWeight = if (validWeights.isNotEmpty()) validWeights.minOrNull()!! else 0f
    val maxWeight = if (validWeights.isNotEmpty()) validWeights.maxOrNull()!! else 100f
    val weightRange = if (maxWeight > minWeight) maxWeight - minWeight else 1f

    val title = "我的${metric.label}日历"
    val periodStr = if (month == null) "${year}年" else "${year}年${month + 1}月"
    
    val subtitle = when (metric) {
        HeatmapMetric.Sleep -> {
            val goalMins = (userProfile?.sleepGoal ?: 7.5f) * 60
            val qualifiedCount = relevantRecords.count { it.sleepDuration >= goalMins }
            "💤 哇！${periodStr}有 ${qualifiedCount} 天睡饱啦！✨"
        }
        HeatmapMetric.Net -> {
            val validRecords = relevantRecords.filter { it.totalIntake > 0 || it.totalBurned > 0 }
            val deficitCount = validRecords.count { (it.totalIntake - (getTarget(it.date) + it.totalBurned)) <= 0 }
            val surplusCount = validRecords.count { (it.totalIntake - (getTarget(it.date) + it.totalBurned)) > 0 }
            // Force split into 3 lines for better readability
            "🔥 燃脂大作战！\n成功制造缺口 ${deficitCount} 天，只有 ${surplusCount} 天稍微放纵了一下哦~🍰"
        }
        HeatmapMetric.Intake -> {
            val count = relevantRecords.count { it.totalIntake > 0 }
            // Ensure periodStr and count are on the second line
            "🥑 认真吃饭的时光！\n${periodStr}记录了 ${count} 天的美味！"
        }
        HeatmapMetric.Weight -> {
            val count = relevantRecords.count { (it.weight ?: 0f) > 0 }
             "⚖️ 记录体重，见证蜕变！\n${periodStr}坚持记录${count} 天，遇见更好的自己！"
        }
        else -> {
             val count = relevantRecords.count { 
                 when(metric) {
                     HeatmapMetric.Water -> it.totalWater > 0
                     HeatmapMetric.Burned -> it.totalBurned > 0
                     else -> false
                 }
             }
             "${periodStr}坚持${metric.label} ${count} 天，继续加油！"
        }
    }

    // 2. Configuration & Dimensions
    val width = 1080
    val padding = 60f
    val headerHeight = 450f // Adjusted header height for closer spacing
    val footerHeight = 350f // Footer for branding + legend
    
    // Layout Calculation
    // Year View: 2 columns, 6 rows
    val cols = 2
    val rows = 6
    val colGap = 60f
    val rowGap = 48f // Reduced by 40% (from 80f)
    val cellGap = 14f // Increased gap slightly
    
    // Calculate month width for Year View
    val yearViewMonthWidth = (width - padding * 2 - (cols - 1) * colGap) / cols
    val yearViewDaySize = (yearViewMonthWidth - 6 * cellGap) / 7
    
    // Calculate month width for Month View (Full width)
    val monthViewWidth = width - padding * 2
    val monthViewDaySize = (monthViewWidth - 6 * cellGap) / 7
    
    // Calculate content height
    val contentHeight = if (month == null) {
        // Year View Height
        // Each row has: Month Title + Grid
        // Grid height = 6 rows of days (max) * (daySize + cellGap)
        val monthGridHeight = 6 * (yearViewDaySize + cellGap)
        val monthTitleHeight = 60f
        val singleRowHeight = monthTitleHeight + 20f + monthGridHeight // 20f spacing between title and grid
        
        singleRowHeight * rows + (rows - 1) * rowGap
    } else {
        // Month View Height
        // Week Header + Grid
        val weekHeaderHeight = 60f
        val gridHeight = 6 * (monthViewDaySize + cellGap)
        weekHeaderHeight + 40f + gridHeight
    }
    
    val totalHeight = headerHeight + contentHeight + footerHeight
    
    // 3. Create Bitmap
    val bitmap = Bitmap.createBitmap(width, totalHeight.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    
    // Theme Configuration
    data class CalendarTheme(
        val bgColor: Int,
        val bgPatternEmoji: List<String>,
        val primaryColor: Int, // For title pill, etc.
        val slogan: String
    )
    
    val theme = when(metric) {
        HeatmapMetric.Intake -> CalendarTheme(
            bgColor = android.graphics.Color.parseColor("#FFFDE7"), // Light Yellow
            bgPatternEmoji = listOf("🍎", "🥗", "🍇", "🥑"),
            primaryColor = android.graphics.Color.parseColor("#FFF59D"),
            slogan = "吃饱饱，没烦恼~"
        )
        HeatmapMetric.Burned -> CalendarTheme(
            bgColor = android.graphics.Color.parseColor("#FFEBEE"), // Light Pink
            bgPatternEmoji = listOf("🔥", "💪", "🏃", "✨"),
            primaryColor = android.graphics.Color.parseColor("#FFCDD2"),
            slogan = "燃烧我的卡路里！"
        )
        HeatmapMetric.Water -> CalendarTheme(
            bgColor = android.graphics.Color.parseColor("#E1F5FE"), // Light Blue
            bgPatternEmoji = listOf("💧", "🌊", "🧊", "💙"),
            primaryColor = android.graphics.Color.parseColor("#B3E5FC"),
            slogan = "多喝水，皮肤好~"
        )
        HeatmapMetric.Sleep -> CalendarTheme(
            bgColor = android.graphics.Color.parseColor("#F3E5F5"), // Light Purple
            bgPatternEmoji = listOf("💤", "🌙", "⭐", "🛌"),
            primaryColor = android.graphics.Color.parseColor("#E1BEE7"),
            slogan = "早睡早起身体好~"
        )
        HeatmapMetric.Net -> CalendarTheme(
            bgColor = android.graphics.Color.parseColor("#E8F5E9"), // Light Green
            bgPatternEmoji = listOf("🐱", "🐾", "🌿", "🍀"),
            primaryColor = android.graphics.Color.parseColor("#C8E6C9"),
            slogan = "今天也是元气满满的一天！"
        )
        HeatmapMetric.Weight -> CalendarTheme(
            bgColor = android.graphics.Color.parseColor("#E0F7FA"), // Cyan-ish
            bgPatternEmoji = listOf("⚖️", "✨", "📉", "💪"),
            primaryColor = android.graphics.Color.parseColor("#B2EBF2"),
            slogan = "轻盈生活，从记录开始~"
        )
    }

    // Draw Background Color
    canvas.drawColor(theme.bgColor)
    
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
    }
    
    // Draw Background Pattern (Semi-transparent emojis)
    paint.textSize = 100f
    paint.alpha = 30 // Very low alpha (0-255)
    paint.textAlign = android.graphics.Paint.Align.CENTER
    
    val patternCols = 5
    val patternRows = (totalHeight / 300).toInt() + 1
    val patternGapX = width / patternCols
    val patternGapY = 300f
    
    val random = java.util.Random(year.toLong()) // Consistent random seed based on year
    
    for (r in 0 until patternRows) {
        for (c in 0 until patternCols) {
            val emoji = theme.bgPatternEmoji[random.nextInt(theme.bgPatternEmoji.size)]
            val x = c * patternGapX + patternGapX / 2 + (random.nextFloat() - 0.5f) * 50f
            val y = r * patternGapY + patternGapY / 2 + (random.nextFloat() - 0.5f) * 50f
            val rotation = (random.nextFloat() - 0.5f) * 60f // -30 to 30 degrees
            
            canvas.save()
            canvas.rotate(rotation, x, y)
            canvas.drawText(emoji, x, y, paint)
            canvas.restore()
        }
    }
    
    paint.alpha = 255 // Reset alpha
    
    // --- Draw Header ---
    
    // Emoji & Title Logic
    val emoji = when(metric) {
        HeatmapMetric.Intake -> "🍎"
        HeatmapMetric.Burned -> "🔥"
        HeatmapMetric.Water -> "💧"
        HeatmapMetric.Sleep -> "🌙"
        HeatmapMetric.Net -> "⚖️"
        HeatmapMetric.Weight -> "⚖️"
    }
    
    val displayTitle = "我的${metric.label}日历"
    val yearTag = "${year}"
    
    // Draw Title Background Pill (Themed highlight style)
    val titleTextSize = 80f
    paint.textSize = titleTextSize
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    val titleWidth = paint.measureText(displayTitle)
    
    val pillPaddingX = 40f
    val pillPaddingY = 20f
    val pillRect = android.graphics.RectF(
        width / 2f - titleWidth / 2f - pillPaddingX,
        180f - titleTextSize + 10f - pillPaddingY,
        width / 2f + titleWidth / 2f + pillPaddingX,
        180f + pillPaddingY
    )
    
    // Theme Highlight for title
    paint.color = theme.primaryColor
    // Add shadow
    paint.setShadowLayer(10f, 0f, 5f, android.graphics.Color.parseColor("#20000000"))
    canvas.drawRoundRect(pillRect, 40f, 40f, paint)
    paint.clearShadowLayer()
    
    // Draw Title Text
    paint.color = android.graphics.Color.parseColor("#333333")
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText(displayTitle, width / 2f, 180f, paint)
    
    // Draw Year Badge next to title (Top Right of pill)
    val badgeSize = 50f
    val badgeX = pillRect.right - 10f
    val badgeY = pillRect.top - 10f
    val badgeRect = android.graphics.RectF(badgeX, badgeY, badgeX + 120f, badgeY + 60f)
    
    paint.color = android.graphics.Color.parseColor("#5C6BC0") // Indigo
    canvas.drawRoundRect(badgeRect, 30f, 30f, paint)
    
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 36f
    paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
    // Center text in badge
    val badgeFontMetrics = paint.fontMetrics
    val badgeTextHeight = badgeFontMetrics.descent - badgeFontMetrics.ascent
    val badgeTextOffset = (badgeTextHeight / 2) - badgeFontMetrics.descent
    canvas.drawText(yearTag, badgeRect.centerX(), badgeRect.centerY() + badgeTextOffset, paint)

    // Subtitle with Emoji
    val trimmedSubtitle = subtitle.trimStart()
    val firstCodePoint = trimmedSubtitle.firstOrNull()?.let { trimmedSubtitle.codePointAt(0) } ?: -1
    val hasLeadingEmoji = firstCodePoint != -1 && (
        firstCodePoint in 0x1F000..0x1FAFF ||
        firstCodePoint in 0x2600..0x27BF ||
        Character.getType(firstCodePoint) == Character.OTHER_SYMBOL.toInt()
    )
    val fullSubtitle = if (hasLeadingEmoji) subtitle else "$emoji $subtitle"
    // Adjust text size based on length to prevent clipping
    paint.textSize = if (fullSubtitle.length > 25) 40f else 48f
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD) // Use Serif for "cute" look if possible, or stick to Default Bold
    paint.color = android.graphics.Color.parseColor("#555555")

    // Use StaticLayout for multiline support
    val textPaint = android.text.TextPaint(paint)
    // Force Left alignment for TextPaint, so StaticLayout can handle centering correctly
    textPaint.textAlign = android.graphics.Paint.Align.LEFT
    val textWidth = (width - padding * 2).toInt()
    val alignment = android.text.Layout.Alignment.ALIGN_CENTER

    val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        android.text.StaticLayout.Builder.obtain(fullSubtitle, 0, fullSubtitle.length, textPaint, textWidth)
            .setAlignment(alignment)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(false)
            .build()
    } else {
        @Suppress("DEPRECATION")
        android.text.StaticLayout(fullSubtitle, textPaint, textWidth, alignment, 1.2f, 0f, false)
    }

    canvas.save()
    // Move Y up slightly to accommodate multiple lines, centered roughly where the single line was
    canvas.translate(padding, 260f)
    staticLayout.draw(canvas)
    canvas.restore()
    
    // Helper for text color based on brightness
    fun getContrastColor(color: Int): Int {
        if (color == android.graphics.Color.TRANSPARENT) return android.graphics.Color.parseColor("#333333") // Dark gray for empty cells
        
        val r = android.graphics.Color.red(color)
        val g = android.graphics.Color.green(color)
        val b = android.graphics.Color.blue(color)
        val sum = r + g + b
        return if (sum > 382) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }

    fun getColor(value: Int, m: HeatmapMetric): Int {
        if (value == 0) {
            // Semi-transparent white for empty cells to blend with background but stay readable
            return android.graphics.Color.argb(150, 255, 255, 255)
        }
        
        // Helper to lerp colors
        fun lerp(start: Int, end: Int, fraction: Float): Int {
            val f = fraction.coerceIn(0f, 1f)
            val a = (android.graphics.Color.alpha(start) + (android.graphics.Color.alpha(end) - android.graphics.Color.alpha(start)) * f).toInt()
            val r = (android.graphics.Color.red(start) + (android.graphics.Color.red(end) - android.graphics.Color.red(start)) * f).toInt()
            val g = (android.graphics.Color.green(start) + (android.graphics.Color.green(end) - android.graphics.Color.green(start)) * f).toInt()
            val b = (android.graphics.Color.blue(start) + (android.graphics.Color.blue(end) - android.graphics.Color.blue(start)) * f).toInt()
            return android.graphics.Color.argb(a, r, g, b)
        }

        // Use slightly more vibrant/pastel colors for "Calendar" look
        return when (m) {
            HeatmapMetric.Net -> {
                if (value > 0) lerp(android.graphics.Color.parseColor("#FFCDD2"), android.graphics.Color.parseColor("#C62828"), (value/1000f))
                else lerp(android.graphics.Color.parseColor("#C8E6C9"), android.graphics.Color.parseColor("#2E7D32"), (Math.abs(value)/1000f))
            }
            HeatmapMetric.Sleep -> {
                val durationHours = value / 60f
                
                if (durationHours < 3f) {
                    android.graphics.Color.parseColor("#E57373") // Red
                } else if (durationHours > 12f) {
                    android.graphics.Color.parseColor("#90A4AE") // Blue Grey
                } else {
                     // Linear interpolation between 3h and 12h
                     val progress = ((durationHours - 3f) / (12f - 3f)).coerceIn(0f, 1f)
                     lerp(
                         android.graphics.Color.parseColor("#F3E5F5"), // Light Purple
                         android.graphics.Color.parseColor("#4A148C"), // Dark Deep Purple
                         progress
                     )
                }
            }
            HeatmapMetric.Water -> lerp(android.graphics.Color.parseColor("#B3E5FC"), android.graphics.Color.parseColor("#0277BD"), value/2500f)
            HeatmapMetric.Intake -> lerp(android.graphics.Color.parseColor("#FFF8E1"), android.graphics.Color.parseColor("#F57F17"), value/3000f)
            HeatmapMetric.Burned -> lerp(android.graphics.Color.parseColor("#FCE4EC"), android.graphics.Color.parseColor("#AD1457"), value/3000f)
            HeatmapMetric.Weight -> {
                // value is encoded weight * 10 or just raw weight cast to Int?
                // Actually getColor takes Int. We'll pass weight * 10
                val weight = value / 10f
                val progress = if (weightRange > 0) (weight - minWeight) / weightRange else 0.5f
                lerp(android.graphics.Color.parseColor("#E0F7FA"), android.graphics.Color.parseColor("#006064"), progress)
            }
        }
    }

    fun drawHeatCell(rect: android.graphics.RectF, radius: Float, color: Int) {
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = color
        paint.setShadowLayer(8f, 0f, 2f, android.graphics.Color.argb(70, 0, 0, 0))
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.clearShadowLayer()
    }

    fun drawStar(cx: Float, cy: Float, size: Float) {
        val path = android.graphics.Path()
        val outerRadius = size / 2
        val innerRadius = outerRadius * 0.5f // Fatter star
        
        for (i in 0 until 5) {
            val angleOuter = -Math.PI / 2 + i * 2 * Math.PI / 5
            val xOuter = cx + Math.cos(angleOuter).toFloat() * outerRadius
            val yOuter = cy + Math.sin(angleOuter).toFloat() * outerRadius
            
            if (i == 0) path.moveTo(xOuter, yOuter) else path.lineTo(xOuter, yOuter)
            
            val angleInner = angleOuter + Math.PI / 5
            val xInner = cx + Math.cos(angleInner).toFloat() * innerRadius
            val yInner = cy + Math.sin(angleInner).toFloat() * innerRadius
            path.lineTo(xInner, yInner)
        }
        path.close()
        
        // Add corner path effect for rounded corners
        val cornerPathEffect = android.graphics.CornerPathEffect(size * 0.1f)
        val originalEffect = paint.pathEffect
        paint.pathEffect = cornerPathEffect
        
        val originalColor = paint.color
        paint.color = android.graphics.Color.parseColor("#FFD700") // Gold
        paint.alpha = 150 // Semi-transparent (~0.6)
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawPath(path, paint)
        
        paint.color = originalColor // Restore
        paint.alpha = 255
        paint.pathEffect = originalEffect // Restore
    }

    val recordMap = records.associateBy { it.date }

    
    // --- Draw Content ---
    var currentY = headerHeight
    
    if (month == null) {
        // --- Year View ---
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val mIndex = row * 2 + col
                val startX = padding + col * (yearViewMonthWidth + colGap)
                val startY = currentY
                
                // Draw Month Title
                paint.textSize = 42f
                paint.color = android.graphics.Color.parseColor("#444444")
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
                paint.textAlign = android.graphics.Paint.Align.LEFT
                canvas.drawText("${mIndex + 1}月", startX + 10f, startY, paint) // Indent slightly
                
                // Draw Days
                val cal = Calendar.getInstance()
                cal.set(year, mIndex, 1)
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                
                val gridOffsetY = startY + 40f // Space between month title and grid
                
                for (day in 1..daysInMonth) {
                    val offsetDay = day + (firstDayOfWeek - 1) - 1
                    val r = offsetDay / 7
                    val c = offsetDay % 7
                    
                    val x = startX + c * (yearViewDaySize + cellGap)
                    val y = gridOffsetY + r * (yearViewDaySize + cellGap)
                    
                    val dateStr = String.format("%04d-%02d-%02d", year, mIndex + 1, day)
                    val record = recordMap[dateStr]
                    
                    val value = when (metric) {
                        HeatmapMetric.Sleep -> record?.sleepDuration ?: 0
                        HeatmapMetric.Water -> record?.totalWater ?: 0
                        HeatmapMetric.Intake -> record?.totalIntake ?: 0
                        HeatmapMetric.Burned -> {
                            // Calculate excluded burned
                             val excludedList = userProfile?.excludedExercises?.split(",")?.map { it.trim() } ?: emptyList()
                             val dayItems = allItems.filter { it.date == dateStr && it.type == "exercise" }
                             val validItems = dayItems.filter { !excludedList.contains(it.name) }
                             validItems.sumOf { it.calories }
                        }
                        HeatmapMetric.Net -> {
                            val intake = record?.totalIntake ?: 0
                            val burned = record?.totalBurned ?: 0
                            val target = getTarget(dateStr)
                            intake - (target + burned)
                        }
                        HeatmapMetric.Weight -> {
                            val w = record?.weight ?: 0f
                            (w * 10).toInt()
                        }
                    }
                    
                    val hasDataForMetric = when(metric) {
                        HeatmapMetric.Sleep -> (record?.sleepDuration ?: 0) > 0
                        HeatmapMetric.Water -> (record?.totalWater ?: 0) > 0
                        HeatmapMetric.Intake -> (record?.totalIntake ?: 0) > 0
                        HeatmapMetric.Burned -> value > 0 // Use calculated value
                        HeatmapMetric.Net -> (record?.totalIntake ?: 0) > 0 || (record?.totalBurned ?: 0) > 0
                        HeatmapMetric.Weight -> (record?.weight ?: 0f) > 0f
                    }
                    
                    val color = if (!hasDataForMetric) getColor(0, metric) else getColor(value, metric)
                    
                    val rect = android.graphics.RectF(x, y, x + yearViewDaySize, y + yearViewDaySize)
                    if (hasDataForMetric) {
                        drawHeatCell(rect, 12f, color)
                    }
                    
                    if (metric == HeatmapMetric.Sleep && hasDataForMetric) {
                         val durationHours = value / 60f
                         val goal = userProfile?.sleepGoal ?: 7.5f
                         if (durationHours in 3f..12f && durationHours >= goal) {
                             drawStar(x + yearViewDaySize/2, y + yearViewDaySize/2, yearViewDaySize * 0.8f)
                         }
                    }
                    
                    // Draw Day Number
                    paint.color = if (hasDataForMetric) getContrastColor(color) else android.graphics.Color.parseColor("#666666")
                    paint.textSize = 34f // Larger font size for year view days
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    paint.textAlign = android.graphics.Paint.Align.CENTER
                    
                    // Center text
                    val fontMetrics = paint.fontMetrics
                    val textHeight = fontMetrics.descent - fontMetrics.ascent
                    val textOffset = (textHeight / 2) - fontMetrics.descent
                    canvas.drawText("$day", x + yearViewDaySize/2, y + yearViewDaySize/2 + textOffset, paint)
                }
            }
            // Move Y down for next row
            // Grid height = 6 rows * (size + gap)
            val monthHeight = 6 * (yearViewDaySize + cellGap) + 40f + 40f // Title + Grid
            currentY += monthHeight + rowGap
        }
    } else {
        // --- Month View ---
        // Draw Week Headers
        val weeks = listOf("日", "一", "二", "三", "四", "五", "六")
        paint.textSize = 40f
        paint.color = android.graphics.Color.GRAY
        paint.typeface = android.graphics.Typeface.SERIF
        paint.textAlign = android.graphics.Paint.Align.CENTER
        
        for (i in 0..6) {
            val x = padding + i * (monthViewDaySize + cellGap) + monthViewDaySize / 2
            canvas.drawText(weeks[i], x, currentY, paint)
        }
        
        currentY += 80f // Space after week header
        
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        
        paint.textSize = 36f
        
        for (day in 1..daysInMonth) {
            val offsetDay = day + (firstDayOfWeek - 1) - 1
            val r = offsetDay / 7
            val c = offsetDay % 7
            
            val x = padding + c * (monthViewDaySize + cellGap)
            val y = currentY + r * (monthViewDaySize + cellGap)
            
            val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
            val record = recordMap[dateStr]
            
            val value = when (metric) {
                HeatmapMetric.Sleep -> record?.sleepDuration ?: 0
                HeatmapMetric.Water -> record?.totalWater ?: 0
                HeatmapMetric.Intake -> record?.totalIntake ?: 0
                HeatmapMetric.Burned -> {
                    // Calculate excluded burned
                     val excludedList = userProfile?.excludedExercises?.split(",")?.map { it.trim() } ?: emptyList()
                     val dayItems = allItems.filter { it.date == dateStr && it.type == "exercise" }
                     val validItems = dayItems.filter { !excludedList.contains(it.name) }
                     validItems.sumOf { it.calories }
                }
                HeatmapMetric.Net -> {
                    val intake = record?.totalIntake ?: 0
                    val burned = record?.totalBurned ?: 0
                    val target = getTarget(dateStr)
                    intake - (target + burned)
                }
                HeatmapMetric.Weight -> {
                    val w = record?.weight ?: 0f
                    (w * 10).toInt()
                }
            }
            
            val hasDataForMetric = when(metric) {
                HeatmapMetric.Sleep -> (record?.sleepDuration ?: 0) > 0
                HeatmapMetric.Water -> (record?.totalWater ?: 0) > 0
                HeatmapMetric.Intake -> (record?.totalIntake ?: 0) > 0
                HeatmapMetric.Burned -> value > 0 // Use calculated value
                HeatmapMetric.Net -> (record?.totalIntake ?: 0) > 0 || (record?.totalBurned ?: 0) > 0
                HeatmapMetric.Weight -> (record?.weight ?: 0f) > 0f
            }
            
            val color = if (!hasDataForMetric) getColor(0, metric) else getColor(value, metric)
            
            val rect = android.graphics.RectF(x, y, x + monthViewDaySize, y + monthViewDaySize)
            if (hasDataForMetric) {
                drawHeatCell(rect, 16f, color)
            } else {
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = color
                paint.clearShadowLayer()
                canvas.drawRoundRect(rect, 16f, 16f, paint)
            }
            
            if (metric == HeatmapMetric.Sleep && hasDataForMetric) {
                 val durationHours = value / 60f
                 val goal = userProfile?.sleepGoal ?: 7.5f
                 if (durationHours in 3f..12f && durationHours >= goal) {
                     drawStar(x + monthViewDaySize/2, y + monthViewDaySize/2, monthViewDaySize * 0.8f)
                 }
            }
            
            // Draw Day Number
            paint.color = getContrastColor(color)
            paint.textAlign = android.graphics.Paint.Align.CENTER
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            
            if (hasDataForMetric) {
                 // Draw day number slightly higher
                canvas.drawText("$day", x + monthViewDaySize/2, y + monthViewDaySize/2 - 10f, paint)
                
                // Draw Value
                paint.textSize = 28f
                paint.typeface = android.graphics.Typeface.SANS_SERIF
                val displayValue = if (metric == HeatmapMetric.Sleep) {
                    val h = value / 60
                    val m = value % 60
                    "${h}h${m}m"
                } else if (metric == HeatmapMetric.Net) {
                    if (value > 0) "+$value" else "$value"
                } else if (metric == HeatmapMetric.Weight) {
                    "${value/10f}"
                } else {
                    "$value"
                }
                // Draw value below center
                canvas.drawText(displayValue, x + monthViewDaySize/2, y + monthViewDaySize/2 + 30f, paint)
                paint.textSize = 36f // Reset
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            } else {
                // Center day number
                val fontMetrics = paint.fontMetrics
                val textHeight = fontMetrics.descent - fontMetrics.ascent
                val textOffset = (textHeight / 2) - fontMetrics.descent
                canvas.drawText("$day", x + monthViewDaySize/2, y + monthViewDaySize/2 + textOffset, paint)
            }
        }
    }
    
    // --- Draw Footer ---
    val footerY = totalHeight - footerHeight
    
    // 1. Draw Legend
    val legendY = footerY + 40f
    val legendSize = 50f
    val legendGap = 15f
    
    // Generate legend colors based on metric
    val legendColors = mutableListOf<Int>()
    val legendText = when(metric) {
        HeatmapMetric.Intake -> "摄入热量越高，颜色越深"
        HeatmapMetric.Burned -> "运动消耗越多，颜色越深"
        HeatmapMetric.Water -> "饮水量越多，颜色越深"
        HeatmapMetric.Sleep -> "睡眠时间越长，颜色越深"
        HeatmapMetric.Net -> "绿色代表热量缺口，红色代表热量盈余"
        HeatmapMetric.Weight -> "颜色越深代表体重越接近今年最大值"
    }
    
    if (metric == HeatmapMetric.Net) {
        // Special legend for Net: Green -> Light -> Red
        legendColors.add(android.graphics.Color.parseColor("#2E7D32"))
        legendColors.add(android.graphics.Color.parseColor("#C8E6C9"))
        legendColors.add(android.graphics.Color.parseColor("#FFCDD2"))
        legendColors.add(android.graphics.Color.parseColor("#C62828"))
    } else if (metric == HeatmapMetric.Sleep) {
        // Correct legend for Sleep (using minutes)
        // Min: 3h = 180, Max: 12h = 720
        legendColors.add(getColor(180, metric)) // 3h (Light)
        legendColors.add(getColor(360, metric)) // 6h
        legendColors.add(getColor(540, metric)) // 9h
        legendColors.add(getColor(720, metric)) // 12h (Dark)
    } else if (metric == HeatmapMetric.Weight) {
         // Weight legend
         // We pass normalized values scaled by 10 (since we use int * 10)
         // minWeight and weightRange are calculated at top of function
         // We want 4 steps: Min, Min+33%, Min+66%, Max
         val step = weightRange / 3
         legendColors.add(getColor(((minWeight) * 10).toInt(), metric))
         legendColors.add(getColor(((minWeight + step) * 10).toInt(), metric))
         legendColors.add(getColor(((minWeight + step * 2) * 10).toInt(), metric))
         legendColors.add(getColor(((minWeight + weightRange) * 10).toInt(), metric))
    } else {
        // Gradient for others
        val baseColor = getColor(3000, metric) // Max value approx
        val lightColor = getColor(500, metric)
        val lighterColor = getColor(100, metric)
        val lightestColor = getColor(10, metric)
        legendColors.add(lightestColor)
        legendColors.add(lighterColor)
        legendColors.add(lightColor)
        legendColors.add(baseColor)
    }
    
    var legendX = padding
    
    for (color in legendColors) {
        paint.color = color
        val rect = android.graphics.RectF(legendX, legendY, legendX + legendSize, legendY + legendSize)
        canvas.drawRoundRect(rect, 10f, 10f, paint)
        legendX += legendSize + legendGap
    }
    
    // Legend Text
    paint.color = android.graphics.Color.GRAY
    paint.textSize = 36f
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
    paint.textAlign = android.graphics.Paint.Align.LEFT
    
    val fontMetricsLegend = paint.fontMetrics
    val textHeightLegend = fontMetricsLegend.descent - fontMetricsLegend.ascent
    val textOffsetLegend = (textHeightLegend / 2) - fontMetricsLegend.descent
    
    canvas.drawText(legendText, legendX + 20f, legendY + legendSize/2 + textOffsetLegend, paint)
    
    
    // 2. Divider & App Branding
    val brandingY = legendY + legendSize + 60f
    paint.color = android.graphics.Color.LTGRAY
    paint.strokeWidth = 2f
    canvas.drawLine(padding, brandingY, width - padding, brandingY, paint)
    
    val iconY = brandingY + 50f
    val iconSize = 100f
    
    // Load App Icon
    val iconId = context.resources.getIdentifier("app_icon", "drawable", context.packageName)
    if (iconId != 0) {
        val iconBitmap = android.graphics.BitmapFactory.decodeResource(context.resources, iconId)
        if (iconBitmap != null) {
            val scaledIcon = Bitmap.createScaledBitmap(iconBitmap, iconSize.toInt(), iconSize.toInt(), true)
            canvas.drawBitmap(scaledIcon, padding, iconY, null)
        }
    } else {
        // Fallback Icon
        paint.color = theme.primaryColor // Use theme color
        canvas.drawRoundRect(android.graphics.RectF(padding, iconY, padding + iconSize, iconY + iconSize), 20f, 20f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 50f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("猫", padding + iconSize/2, iconY + iconSize/2 + 15f, paint)
    }
    
    // App Name
    paint.color = android.graphics.Color.parseColor("#333333")
    paint.textSize = 48f
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
    paint.textAlign = android.graphics.Paint.Align.LEFT
    canvas.drawText("猫猫要健康！", padding + iconSize + 30f, iconY + iconSize/2 + 5f, paint)
    
    // Tagline/Link - Use cute slogan from theme
    paint.color = android.graphics.Color.GRAY
    paint.textSize = 32f
    paint.typeface = android.graphics.Typeface.DEFAULT
    canvas.drawText(theme.slogan, padding + iconSize + 30f, iconY + iconSize/2 + 50f, paint)
    
    return bitmap
}

fun saveCalendarToGallery(context: android.content.Context, bitmap: Bitmap) {
    val filename = "CalorieTracker_${System.currentTimeMillis()}.png"
    var fos: java.io.OutputStream? = null
    var uri: Uri? = null

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CalorieTracker")
            }
            val resolver = context.contentResolver
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                fos = resolver.openOutputStream(uri)
            }
        } else {
            // For older versions, we might need WRITE_EXTERNAL_STORAGE.
            // Assuming permission is granted or not strictly enforced for some paths on older APIs
            // or we fall back to cache if this fails.
            // Simplified for this context: Try to use MediaStore even on older APIs (often works for external)
            // or use legacy file path.
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(imagesDir, "CalorieTracker")
            if (!appDir.exists()) appDir.mkdirs()
            val image = File(appDir, filename)
            fos = FileOutputStream(image)
            
            // Trigger media scan
            // MediaScannerConnection.scanFile(context, arrayOf(image.toString()), null, null)
        }

        if (fos != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
            Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "保存出错: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareCalendarImage(context: android.content.Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "share_calendar.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享我的日历"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun getHeatmapColor(
    value: Int, 
    metric: HeatmapMetric, 
    userProfile: UserProfileEntity? = null,
    min: Float = 0f,
    max: Float = 100f
): Color {
    if (value == 0) return when(metric) {
        HeatmapMetric.Intake -> Color(0xFFFFF8E1)
        HeatmapMetric.Burned -> Color(0xFFFCE4EC)
        HeatmapMetric.Water -> Color(0xFFE1F5FE) // Very light light-blue
        HeatmapMetric.Sleep -> Color(0xFFEDE7F6) // Very light deep-purple
        HeatmapMetric.Weight -> Color(0xFFE0F7FA) // Very light cyan
        else -> Color.Transparent.copy(alpha = 0.05f)
    }

    return when (metric) {
        HeatmapMetric.Net -> {
            // Net: Positive (Surplus) = Red/Bad, Negative (Deficit) = Green/Good
            if (value > 0) {
                // Surplus - Red
                val alpha = (value / 1000f).coerceIn(0.3f, 1.0f)
                MaterialTheme.colorScheme.error.copy(alpha = alpha)
            } else {
                // Deficit (value is negative) - Green
                val absValue = Math.abs(value)
                val alpha = (absValue / 1000f).coerceIn(0.3f, 1.0f)
                Color(0xFF4CAF50).copy(alpha = alpha)
            }
        }
        HeatmapMetric.Sleep -> {
            val durationHours = value / 60f
            val goal = userProfile?.sleepGoal ?: 7.5f
            
            if (durationHours < 3f) {
                // Less than 3 hours: Warning Red
                Color(0xFFE57373)
            } else if (durationHours > 12f) {
                // More than 12 hours: Warning Blue Grey
                Color(0xFF90A4AE)
            } else {
                // Between 3h and 12h
                // 3h -> Lightest Purple (0xFFF3E5F5)
                // 12h -> Darkest Purple (0xFF4A148C)
                // Linear interpolation across the whole valid range [3, 12]
                val progress = ((durationHours - 3f) / (12f - 3f)).coerceIn(0f, 1f)
                androidx.compose.ui.graphics.lerp(
                    Color(0xFFF3E5F5), // Light Purple
                    Color(0xFF4A148C), // Dark Deep Purple
                    progress
                )
            }
        }
        HeatmapMetric.Water -> {
            // Gradient from very light blue to very deep blue
            // Target roughly 2000ml
            val fraction = (value / 2500f).coerceIn(0f, 1f)
            androidx.compose.ui.graphics.lerp(
                Color(0xFFE1F5FE),
                Color(0xFF0288D1),
                fraction
            )
        }
        HeatmapMetric.Intake -> {
            val fraction = (value / 3000f).coerceIn(0f, 1f)
            androidx.compose.ui.graphics.lerp(
                Color(0xFFFFF8E1),
                Color(0xFFF57F17),
                fraction
            )
        }
        HeatmapMetric.Burned -> {
            val fraction = (value / 3000f).coerceIn(0f, 1f)
            androidx.compose.ui.graphics.lerp(
                Color(0xFFFCE4EC),
                Color(0xFFAD1457),
                fraction
            )
        }
        HeatmapMetric.Weight -> {
             val weight = value / 10f
             val range = if (max > min) max - min else 1f
             val progress = if (range > 0) (weight - min) / range else 0.5f
             androidx.compose.ui.graphics.lerp(
                 Color(0xFFE0F7FA), // Light Cyan
                 Color(0xFF006064), // Deep Cyan
                 progress
             )
        }
    }
}

@Composable
fun getContentColorForBackground(bgColor: Color): Color {
    if (bgColor == Color.Transparent) return MaterialTheme.colorScheme.onSurface
    
    val surfaceColor = MaterialTheme.colorScheme.surface
    // Composite the semi-transparent background color over the surface color
    // to get the actual visible color
    val actualColor = bgColor.compositeOver(surfaceColor)
    
    // Calculate sum of RGB components (0.0 - 1.0 range)
    // 255+255+255 in 8-bit is 1.0+1.0+1.0 = 3.0 in float
    // Half is 1.5
    val sum = actualColor.red + actualColor.green + actualColor.blue
    
    return if (sum > 1.5f) Color.Black else Color.White
}

@Composable
fun YearHeatmapView(
    year: Int,
    records: List<DailyRecordEntity>,
    allItems: List<CalorieItemEntity>,
    recordMap: Map<String, DailyRecordEntity>,
    userProfile: UserProfileEntity?,
    metric: HeatmapMetric,
    onMonthClick: (Int) -> Unit,
    onDayClick: (String) -> Unit
) {
    // Calculate year-based min/max for weight
    val weights = records.filter { it.date.startsWith("$year-") && it.weight != null && it.weight > 0 }.mapNotNull { it.weight }
    val minWeight = if (weights.isNotEmpty()) weights.minOrNull()!! else 0f
    val maxWeight = if (weights.isNotEmpty()) weights.maxOrNull()!! else 100f

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val m1 = row * 2
                val m2 = row * 2 + 1
                Box(modifier = Modifier.weight(1f)) {
                    MonthCalendarMini(year, m1, records, allItems, recordMap, userProfile, metric, minWeight, maxWeight, onMonthClick, onDayClick)
                }
                Box(modifier = Modifier.weight(1f)) {
                    MonthCalendarMini(year, m2, records, allItems, recordMap, userProfile, metric, minWeight, maxWeight, onMonthClick, onDayClick)
                }
            }
        }
    }
}

@Composable
fun MonthCalendarMini(
    year: Int,
    month: Int,
    records: List<DailyRecordEntity>,
    allItems: List<CalorieItemEntity>,
    recordMap: Map<String, DailyRecordEntity>,
    userProfile: UserProfileEntity?,
    metric: HeatmapMetric,
    minWeight: Float,
    maxWeight: Float,
    onHeaderClick: (Int) -> Unit,
    onDayClick: (String) -> Unit
) {
    Column {
        Text(
            text = "${month + 1}月",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 6.dp)
                .clickable { onHeaderClick(month) }
        )
        
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun
        
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val totalCells = daysInMonth + (firstDayOfWeek - 1)
            val rows = (totalCells + 6) / 7
            
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (c in 0 until 7) {
                        val index = r * 7 + c
                        val day = index - (firstDayOfWeek - 1) + 1
                        
                        if (day in 1..daysInMonth) {
                            val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
                            val record = recordMap[dateStr]
                            
                            val value = when (metric) {
                                HeatmapMetric.Sleep -> record?.sleepDuration ?: 0
                                HeatmapMetric.Water -> record?.totalWater ?: 0
                                HeatmapMetric.Intake -> record?.totalIntake ?: 0
                                HeatmapMetric.Burned -> {
                                    // Calculate excluded burned
                                     val excludedList = userProfile?.excludedExercises?.split(",")?.map { it.trim() } ?: emptyList()
                                     val dayItems = allItems.filter { it.date == dateStr && it.type == "exercise" }
                                     val validItems = dayItems.filter { !excludedList.contains(it.name) }
                                     validItems.sumOf { it.calories }
                                }
                                HeatmapMetric.Net -> {
                                    // Calculate dynamic net
                                    val target = if (userProfile != null) {
                                        val effectiveWeight = CalorieUtils.getEffectiveWeight(dateStr, records, userProfile)
                                        CalorieUtils.calculateDailyTarget(
                                            gender = userProfile.gender,
                                            weight = effectiveWeight,
                                            height = userProfile.height,
                                            age = userProfile.age,
                                            activityLevel = userProfile.activityLevel,
                                            goal = userProfile.goal
                                        )
                                    } else 2000
                                    
                                    val intake = record?.totalIntake ?: 0
                                    val burned = record?.totalBurned ?: 0
                                    intake - (target + burned)
                                }
                                HeatmapMetric.Weight -> {
                                    val w = record?.weight ?: 0f
                                    (w * 10).toInt()
                                }
                            }
                            
                            val hasDataForMetric = when(metric) {
                                HeatmapMetric.Sleep -> (record?.sleepDuration ?: 0) > 0
                                HeatmapMetric.Water -> (record?.totalWater ?: 0) > 0
                                HeatmapMetric.Intake -> (record?.totalIntake ?: 0) > 0
                                HeatmapMetric.Burned -> value > 0 // Use calculated value
                                HeatmapMetric.Net -> (record?.totalIntake ?: 0) > 0 || (record?.totalBurned ?: 0) > 0
                                HeatmapMetric.Weight -> (record?.weight ?: 0f) > 0f
                            }
                            
                            // Use raw value for color calculation now
                            val bgColor = if (!hasDataForMetric) Color.Transparent else getHeatmapColor(value, metric, userProfile, minWeight, maxWeight)
                            val contentColor = getContentColorForBackground(bgColor)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(bgColor, RoundedCornerShape(2.dp))
                                    .clickable { onDayClick(dateStr) },
                                contentAlignment = Alignment.Center
                            ) {
                                // Star Indicator for Sleep Goal Met
                                if (metric == HeatmapMetric.Sleep && hasDataForMetric) {
                                    val durationHours = value / 60f
                                    val goal = userProfile?.sleepGoal ?: 7.5f
                                    if (durationHours in 3f..12f && durationHours >= goal) {
                                        // Draw fat rounded star
                                        Canvas(modifier = Modifier.fillMaxSize(0.8f).alpha(0.6f)) {
                                            val cx = size.width / 2
                                            val cy = size.height / 2
                                            val starSize = size.width
                                            val outerRadius = starSize / 2
                                            val innerRadius = outerRadius * 0.5f // Fatter star
                                            
                                            val path = Path()
                                            for (i in 0 until 5) {
                                                val angleOuter = -Math.PI / 2 + i * 2 * Math.PI / 5
                                                val xOuter = cx + Math.cos(angleOuter).toFloat() * outerRadius
                                                val yOuter = cy + Math.sin(angleOuter).toFloat() * outerRadius
                                                
                                                if (i == 0) path.moveTo(xOuter, yOuter) else path.lineTo(xOuter, yOuter)
                                                
                                                val angleInner = angleOuter + Math.PI / 5
                                                val xInner = cx + Math.cos(angleInner).toFloat() * innerRadius
                                                val yInner = cy + Math.sin(angleInner).toFloat() * innerRadius
                                                path.lineTo(xInner, yInner)
                                            }
                                            path.close()
                                            
                                            drawContext.canvas.nativeCanvas.apply {
                                                val paint = android.graphics.Paint().apply {
                                                    color = android.graphics.Color.parseColor("#FFD700")
                                                    style = android.graphics.Paint.Style.FILL
                                                    isAntiAlias = true
                                                    pathEffect = android.graphics.CornerPathEffect(starSize * 0.1f)
                                                }
                                                drawPath(path.asAndroidPath(), paint)
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "$day",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                    color = contentColor,
                                    fontWeight = if (hasDataForMetric) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthHeatmapView(
    year: Int,
    month: Int,
    records: List<DailyRecordEntity>,
    allItems: List<CalorieItemEntity>,
    recordMap: Map<String, DailyRecordEntity>,
    userProfile: UserProfileEntity?,
    metric: HeatmapMetric,
    onDayClick: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    
    // Calculate year-based min/max for weight for consistent coloring
    val weights = records.filter { it.date.startsWith("$year-") && it.weight != null && it.weight > 0 }.mapNotNull { it.weight }
    val minWeight = if (weights.isNotEmpty()) weights.minOrNull()!! else 0f
    val maxWeight = if (weights.isNotEmpty()) weights.maxOrNull()!! else 100f
    
    val weeks = listOf("日", "一", "二", "三", "四", "五", "六")
    
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            weeks.forEach { 
                Text(
                    text = it, 
                    modifier = Modifier.weight(1f), 
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        val totalCells = daysInMonth + (firstDayOfWeek - 1)
        val rows = (totalCells + 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in 0 until 7) {
                        val index = r * 7 + c
                        val day = index - (firstDayOfWeek - 1) + 1
                        
                        if (day in 1..daysInMonth) {
                            val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
                            val record = recordMap[dateStr]
                            
                            val value = when (metric) {
                                HeatmapMetric.Sleep -> record?.sleepDuration ?: 0
                                HeatmapMetric.Water -> record?.totalWater ?: 0
                                HeatmapMetric.Intake -> record?.totalIntake ?: 0
                                HeatmapMetric.Burned -> {
                                    // Calculate excluded burned
                                     val excludedList = userProfile?.excludedExercises?.split(",")?.map { it.trim() } ?: emptyList()
                                     val dayItems = allItems.filter { it.date == dateStr && it.type == "exercise" }
                                     val validItems = dayItems.filter { !excludedList.contains(it.name) }
                                     validItems.sumOf { it.calories }
                                }
                                HeatmapMetric.Net -> {
                                    // Calculate dynamic net
                                    val target = if (userProfile != null) {
                                        val effectiveWeight = CalorieUtils.getEffectiveWeight(dateStr, records, userProfile)
                                        CalorieUtils.calculateDailyTarget(
                                            gender = userProfile.gender,
                                            weight = effectiveWeight,
                                            height = userProfile.height,
                                            age = userProfile.age,
                                            activityLevel = userProfile.activityLevel,
                                            goal = userProfile.goal
                                        )
                                    } else 2000
                                    
                                    val intake = record?.totalIntake ?: 0
                                    val burned = record?.totalBurned ?: 0
                                    intake - (target + burned)
                                }
                                HeatmapMetric.Weight -> {
                                    val w = record?.weight ?: 0f
                                    (w * 10).toInt()
                                }
                            }
                            
                            val hasDataForMetric = when(metric) {
                                HeatmapMetric.Sleep -> (record?.sleepDuration ?: 0) > 0
                                HeatmapMetric.Water -> (record?.totalWater ?: 0) > 0
                                HeatmapMetric.Intake -> (record?.totalIntake ?: 0) > 0
                                HeatmapMetric.Burned -> value > 0 // Use calculated value
                                HeatmapMetric.Net -> (record?.totalIntake ?: 0) > 0 || (record?.totalBurned ?: 0) > 0
                                HeatmapMetric.Weight -> (record?.weight ?: 0f) > 0f
                            }
                            
                            // Use raw value for color
                            val bgColor = if (!hasDataForMetric) Color.Transparent else getHeatmapColor(value, metric, userProfile, minWeight, maxWeight)
                            val contentColor = getContentColorForBackground(bgColor)
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(bgColor, RoundedCornerShape(4.dp))
                                    .clickable { onDayClick(dateStr) },
                                contentAlignment = Alignment.Center
                            ) {
                                // Star Indicator for Sleep Goal Met
                                if (metric == HeatmapMetric.Sleep && hasDataForMetric) {
                                    val durationHours = value / 60f
                                    val goal = userProfile?.sleepGoal ?: 7.5f
                                    if (durationHours in 3f..12f && durationHours >= goal) {
                                        // Draw fat rounded star
                                        Canvas(modifier = Modifier.fillMaxSize(0.8f).alpha(0.6f)) {
                                            val cx = size.width / 2
                                            val cy = size.height / 2
                                            val starSize = size.width
                                            val outerRadius = starSize / 2
                                            val innerRadius = outerRadius * 0.5f // Fatter star
                                            
                                            val path = Path()
                                            for (i in 0 until 5) {
                                                val angleOuter = -Math.PI / 2 + i * 2 * Math.PI / 5
                                                val xOuter = cx + Math.cos(angleOuter).toFloat() * outerRadius
                                                val yOuter = cy + Math.sin(angleOuter).toFloat() * outerRadius
                                                
                                                if (i == 0) path.moveTo(xOuter, yOuter) else path.lineTo(xOuter, yOuter)
                                                
                                                val angleInner = angleOuter + Math.PI / 5
                                                val xInner = cx + Math.cos(angleInner).toFloat() * innerRadius
                                                val yInner = cy + Math.sin(angleInner).toFloat() * innerRadius
                                                path.lineTo(xInner, yInner)
                                            }
                                            path.close()
                                            
                                            drawContext.canvas.nativeCanvas.apply {
                                                val paint = android.graphics.Paint().apply {
                                                    color = android.graphics.Color.parseColor("#FFD700")
                                                    style = android.graphics.Paint.Style.FILL
                                                    isAntiAlias = true
                                                    pathEffect = android.graphics.CornerPathEffect(starSize * 0.1f)
                                                }
                                                drawPath(path.asAndroidPath(), paint)
                                            }
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                    text = "$day",
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                                    color = contentColor,
                                    fontWeight = FontWeight.Bold
                                )
                                if (hasDataForMetric) {
                                    // Show signed value for Net, absolute for others
                                val displayValue = when (metric) {
                                    HeatmapMetric.Net -> if (value > 0) "+$value" else "$value"
                                    HeatmapMetric.Sleep -> {
                                        val h = value / 60
                                        val m = value % 60
                                        "${h}h${m}m"
                                    }
                                    HeatmapMetric.Weight -> "${value/10f}"
                                    else -> "${Math.abs(value)}"
                                }
                                
                                Text(
                                    text = displayValue,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = contentColor.copy(alpha = 0.9f)
                                )
                                }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailRecordSectionHeader(title: String, calories: Int, accentColor: Color, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = accentColor,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${calories} kcal",
            style = MaterialTheme.typography.labelLarge,
            color = textColor.copy(alpha = 0.78f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DetailRecordRow(item: CalorieItemEntity, textColor: Color) {
    val isFood = item.type == "food"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isFood) Icons.Default.Restaurant else Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = if (isFood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(item.name, style = MaterialTheme.typography.bodyMedium, color = textColor, modifier = Modifier.weight(1f))
        Text(
            "${if (isFood) "+" else "-"}${item.calories}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isFood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun DayDetailDialog(
    date: String,
    record: DailyRecordEntity?,
    records: List<DailyRecordEntity>, // Added records for effective weight calculation
    items: List<CalorieItemEntity>,
    userProfile: UserProfileEntity?,
    metric: HeatmapMetric,
    accentColor: Color,
    cardColor: Color,
    onCardColor: Color,
    onDismiss: () -> Unit,
    onAddRecord: () -> Unit,
    onUpdateWeight: (Float) -> Unit,
    onUpdateWater: (Int) -> Unit,
    onUpdateSleep: (Int) -> Unit
) {
    var showWeightEdit by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf(record?.weight?.toString() ?: "") }
    
    // States for Water/Sleep editing
    var showWaterEdit by remember { mutableStateOf(false) }
    var waterInput by remember { mutableStateOf(record?.totalWater?.toString() ?: "0") }
    
    var showSleepEdit by remember { mutableStateOf(false) }
    var sleepHoursInput by remember { mutableStateOf((record?.sleepDuration?.div(60) ?: 0).toString()) }
    var sleepMinutesInput by remember { mutableStateOf((record?.sleepDuration?.rem(60) ?: 0).toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = date, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = onCardColor)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = onCardColor.copy(alpha = 0.72f))
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                when (metric) {
                    HeatmapMetric.Water -> {
                        // Water View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("当日饮水", style = MaterialTheme.typography.labelMedium, color = onCardColor.copy(alpha = 0.72f))
                                if (showWaterEdit) {
                                    OutlinedTextField(
                                        value = waterInput,
                                        onValueChange = { waterInput = it },
                                        modifier = Modifier.width(100.dp),
                                        singleLine = true,
                                        label = { Text("ml") },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                    )
                                } else {
                                    Text(
                                        text = "${record?.totalWater ?: 0} ml",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                            }
                            
                            TextButton(onClick = {
                                if (showWaterEdit) {
                                    val w = waterInput.toIntOrNull()
                                    if (w != null && w >= 0) {
                                        onUpdateWater(w)
                                        showWaterEdit = false
                                    }
                                } else {
                                    showWaterEdit = true
                                }
                            }) {
                                Text(if (showWaterEdit) "保存" else "修改", color = accentColor)
                            }
                        }
                    }
                    HeatmapMetric.Sleep -> {
                        // Sleep View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("当日睡眠", style = MaterialTheme.typography.labelMedium, color = onCardColor.copy(alpha = 0.72f))
                                if (showSleepEdit) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = sleepHoursInput,
                                            onValueChange = { sleepHoursInput = it },
                                            modifier = Modifier.width(60.dp),
                                            singleLine = true,
                                            label = { Text("h") },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                        )
                                        OutlinedTextField(
                                            value = sleepMinutesInput,
                                            onValueChange = { sleepMinutesInput = it },
                                            modifier = Modifier.width(60.dp),
                                            singleLine = true,
                                            label = { Text("m") },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                        )
                                    }
                                } else {
                                    val duration = record?.sleepDuration ?: 0
                                    val h = duration / 60
                                    val m = duration % 60
                                    Text(
                                        text = "${h}h ${m}m",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                            }
                            
                            TextButton(onClick = {
                                if (showSleepEdit) {
                                    val h = sleepHoursInput.toIntOrNull() ?: 0
                                    val m = sleepMinutesInput.toIntOrNull() ?: 0
                                    val total = h * 60 + m
                                    if (total >= 0) {
                                        onUpdateSleep(total)
                                        showSleepEdit = false
                                    }
                                } else {
                                    showSleepEdit = true
                                }
                            }) {
                                Text(if (showSleepEdit) "保存" else "修改", color = accentColor)
                            }
                        }
                    }
                    else -> {
                        // Original Calorie/Weight View
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatBox("饮食", "${record?.totalIntake ?: 0}", MaterialTheme.colorScheme.primary)
                        StatBox("运动", "${record?.totalBurned ?: 0}", MaterialTheme.colorScheme.secondary)
                            
                            // Net = Intake - (Target + Burned)
                            // Calculate target dynamically using effective weight
                            val target = if (userProfile != null) {
                                val effectiveWeight = CalorieUtils.getEffectiveWeight(date, records, userProfile)
                                CalorieUtils.calculateDailyTarget(
                                    gender = userProfile.gender,
                                    weight = effectiveWeight,
                                    height = userProfile.height,
                                    age = userProfile.age,
                                    activityLevel = userProfile.activityLevel,
                                    goal = userProfile.goal
                                )
                            } else {
                                2000
                            }
                            
                            val intake = record?.totalIntake ?: 0
                            val burned = record?.totalBurned ?: 0
                            val net = intake - (target + burned)
                            
                            // Display signed value
                            val displayNet = if (net > 0) "+$net" else "$net"
                            StatBox("热量缺口", displayNet, if (net <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        
                        // Weight Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("当日体重", style = MaterialTheme.typography.labelMedium, color = onCardColor.copy(alpha = 0.72f))
                                if (showWeightEdit) {
                                    OutlinedTextField(
                                        value = weightInput,
                                        onValueChange = { weightInput = it },
                                        modifier = Modifier.width(100.dp),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                } else {
                                    Text(
                                        text = if (record?.weight != null) "${record.weight} kg" else "未记录",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = onCardColor
                                    )
                                }
                            }
                            
                            TextButton(onClick = {
                                if (showWeightEdit) {
                                    val w = weightInput.toFloatOrNull()
                                    if (w != null && w > 0) {
                                        onUpdateWeight(w)
                                        showWeightEdit = false
                                    }
                                } else {
                                    showWeightEdit = true
                                }
                            }) {
                                Text(if (showWeightEdit) "保存" else if (record?.weight != null) "修改" else "补录", color = accentColor)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Items List
                        Text("记录详情", style = MaterialTheme.typography.labelMedium, color = onCardColor.copy(alpha = 0.72f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val breakfastItems = remember(items) {
                            items
                                .filter { it.type == "food" && CalorieUtils.getMealCategoryByTime(it.time) == CalorieUtils.MealCategory.BREAKFAST }
                                .sortedBy { it.time }
                        }
                        val lunchItems = remember(items) {
                            items
                                .filter { it.type == "food" && CalorieUtils.getMealCategoryByTime(it.time) == CalorieUtils.MealCategory.LUNCH }
                                .sortedBy { it.time }
                        }
                        val dinnerItems = remember(items) {
                            items
                                .filter { it.type == "food" && CalorieUtils.getMealCategoryByTime(it.time) == CalorieUtils.MealCategory.DINNER }
                                .sortedBy { it.time }
                        }
                        val nightSnackItems = remember(items) {
                            items
                                .filter { it.type == "food" && CalorieUtils.getMealCategoryByTime(it.time) == CalorieUtils.MealCategory.NIGHT_SNACK }
                                .sortedBy { it.time }
                        }
                        val exerciseItems = remember(items) {
                            items
                                .filter { it.type == "exercise" }
                                .sortedByDescending { it.time }
                        }

                        if (items.isEmpty()) {
                            Text("无饮食运动记录", style = MaterialTheme.typography.bodySmall, color = onCardColor.copy(alpha = 0.68f), modifier = Modifier.padding(bottom = 16.dp))
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                if (breakfastItems.isNotEmpty()) {
                                    item {
                                        DetailRecordSectionHeader(
                                            title = "早餐",
                                            calories = breakfastItems.sumOf { it.calories },
                                            accentColor = MaterialTheme.colorScheme.primary,
                                            textColor = onCardColor
                                        )
                                    }
                                    items(breakfastItems) { item ->
                                        DetailRecordRow(item, textColor = onCardColor)
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                                if (lunchItems.isNotEmpty()) {
                                    item {
                                        DetailRecordSectionHeader(
                                            title = "午餐",
                                            calories = lunchItems.sumOf { it.calories },
                                            accentColor = Color(0xFF26A69A),
                                            textColor = onCardColor
                                        )
                                    }
                                    items(lunchItems) { item ->
                                        DetailRecordRow(item, textColor = onCardColor)
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                                if (dinnerItems.isNotEmpty()) {
                                    item {
                                        DetailRecordSectionHeader(
                                            title = "晚餐",
                                            calories = dinnerItems.sumOf { it.calories },
                                            accentColor = Color(0xFFFF7043),
                                            textColor = onCardColor
                                        )
                                    }
                                    items(dinnerItems) { item ->
                                        DetailRecordRow(item, textColor = onCardColor)
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                                if (exerciseItems.isNotEmpty()) {
                                    item {
                                        DetailRecordSectionHeader(
                                            title = "运动",
                                            calories = exerciseItems.sumOf { it.calories },
                                            accentColor = MaterialTheme.colorScheme.secondary,
                                            textColor = onCardColor
                                        )
                                    }
                                    items(exerciseItems) { item ->
                                        DetailRecordRow(item, textColor = onCardColor)
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                                if (nightSnackItems.isNotEmpty()) {
                                    item {
                                        DetailRecordSectionHeader(
                                            title = "宵夜",
                                            calories = nightSnackItems.sumOf { it.calories },
                                            accentColor = Color(0xFF7E57C2),
                                            textColor = onCardColor
                                        )
                                    }
                                    items(nightSnackItems) { item ->
                                        DetailRecordRow(item, textColor = onCardColor)
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onAddRecord,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                        ) {
                            Text("补录饮食/运动")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmoothWeightChart(
    year: Int,
    month: Int?,
    records: List<DailyRecordEntity>,
    accentColor: Color,
    contentColor: Color
) {
    var viewMode by remember { mutableStateOf("Month") } // Month, Quarter, Year
    
    Column {
        // View Mode Selector
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            listOf("Month" to "月", "Quarter" to "季", "Year" to "年").forEach { (mode, label) ->
                TextButton(
                    onClick = { viewMode = mode },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (viewMode == mode) accentColor else contentColor.copy(alpha = 0.72f)
                    )
                ) {
                    Text(label)
                }
            }
        }
        
        val filteredRecords = remember(records, year, month, viewMode) {
            records.filter { 
                val cal = Calendar.getInstance()
                val parts = it.date.split("-")
                if (parts.size == 3) {
                    val rYear = parts[0].toInt()
                    val rMonth = parts[1].toInt() - 1 
                    
                    if (viewMode == "Year") {
                        rYear == year
                    } else if (viewMode == "Quarter") {
                        val currentMonth = month ?: 0 // Default to Jan if no month selected? Or better logic
                        // If month is selected, show that quarter. If not, show whole year?
                        // Let's assume month is always selected for Month view.
                        // For Quarter, we need a reference month.
                        if (month != null) {
                            val qStart = (month / 3) * 3
                            rYear == year && rMonth in qStart..(qStart + 2)
                        } else {
                            rYear == year // Fallback
                        }
                    } else { // Month
                        rYear == year && (month == null || rMonth == month)
                    }
                } else false
            }.filter { it.weight != null && it.weight > 0 }.sortedBy { it.date }
        }

        val lineColor = accentColor
        val dotColor = Color.White
        val gridColor = contentColor.copy(alpha = 0.24f)
        val textColor = contentColor.copy(alpha = 0.72f)

        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        val minWeight = filteredRecords.minOf { it.weight!! } - 2f
        val maxWeight = filteredRecords.maxOf { it.weight!! } + 2f
        val weightRange = maxWeight - minWeight

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(start = 32.dp, end = 16.dp, top = 16.dp, bottom = 32.dp) // Extra padding for labels
        ) {
            val width = size.width
            val height = size.height
            
            // Draw Grid & Y-Axis Labels
            val lines = 5
            val textPaint = android.graphics.Paint().apply {
                color = textColor.toArgb()
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            
            for (i in 0..lines) {
                val y = height * (i.toFloat() / lines)
                val weightVal = maxWeight - (weightRange * (i.toFloat() / lines))
                
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
                
                drawContext.canvas.nativeCanvas.drawText(
                    "%.1f".format(weightVal),
                    -8.dp.toPx(),
                    y + 4.dp.toPx(),
                    textPaint
                )
            }

            if (filteredRecords.size < 2) {
                val cx = width / 2
                val cy = height / 2
                drawCircle(lineColor, 4.dp.toPx(), Offset(cx, cy))
                return@Canvas
            }

            val path = Path()
            val points = mutableListOf<Offset>()
            
            val xStep = width / (filteredRecords.size - 1)
            filteredRecords.forEachIndexed { index, record ->
                val x = index * xStep
                val normalizedW = (record.weight!! - minWeight) / weightRange
                val y = height - (normalizedW * height)
                points.add(Offset(x, y))
                
                // X-Axis Labels (Date) - Draw sparsely
                if (filteredRecords.size < 10 || index % (filteredRecords.size / 5) == 0) {
                    val dateLabel = record.date.substring(5) // MM-DD
                    textPaint.textAlign = android.graphics.Paint.Align.CENTER
                    drawContext.canvas.nativeCanvas.drawText(
                        dateLabel,
                        x,
                        height + 20.dp.toPx(),
                        textPaint
                    )
                }
            }

            path.moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2, p0.y)
                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2, p1.y)
                path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
            }
            
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            
            points.forEach { 
                drawCircle(lineColor, 5.dp.toPx(), it)
                drawCircle(dotColor, 3.dp.toPx(), it)
            }
        }
    }
}
