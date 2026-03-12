package com.example.calorietracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.data.CalorieItemEntity
import com.example.calorietracker.util.CalorieUtils
import java.text.SimpleDateFormat
import java.util.*

// Enum for Tabs
enum class StatsTab(val label: String) {
    WEEK("周"),
    MONTH("月"),
    YEAR("年"),
    TOTAL("总")
}

enum class ChartMetric(val label: String) {
    DURATION("时长"),
    CALORIES("热量")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    allItems: List<CalorieItemEntity>
) {
    var selectedTab by remember { mutableStateOf(StatsTab.MONTH) }
    var chartMetric by remember { mutableStateOf(ChartMetric.DURATION) }
    
    // Date navigation state
    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    
    // Filter items based on tab and date
    val filteredItems = remember(allItems, selectedTab, currentCalendar.timeInMillis) {
        filterItems(allItems, selectedTab, currentCalendar)
    }

    val durationMap = remember(filteredItems) {
        filteredItems.associate { it.id to CalorieUtils.parseDuration(it.notes) }
    }
    
    val totalDuration = durationMap.values.sum()
    val totalSessions = filteredItems.size
    val totalCalories = filteredItems.sumOf { it.calories }
    
    val uniqueDays = filteredItems.map { it.date }.distinct().size
    
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            ) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "运动统计", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                
                // Custom Tab Row
                StatsTabRow(selectedTab, onTabSelected = { selectedTab = it })
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DateNavigator(selectedTab, currentCalendar) { dir ->
                    val newCal = currentCalendar.clone() as Calendar
                    when (selectedTab) {
                        StatsTab.WEEK -> newCal.add(Calendar.WEEK_OF_YEAR, dir)
                        StatsTab.MONTH -> newCal.add(Calendar.MONTH, dir)
                        StatsTab.YEAR -> newCal.add(Calendar.YEAR, dir)
                        else -> {}
                    }
                    currentCalendar = newCal
                }
            }

            item {
                // Bar Chart Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (chartMetric == ChartMetric.DURATION) "运动时长趋势" else "运动热量趋势",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // Metric Toggle
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .padding(2.dp)
                            ) {
                                ChartMetric.values().forEach { metric ->
                                    val selected = chartMetric == metric
                                    val bgColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(bgColor)
                                            .clickable { chartMetric = metric }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
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
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        StatsBarChart(
                            items = filteredItems,
                            tab = selectedTab,
                            currentDate = currentCalendar,
                            metric = chartMetric,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }
            
            item {
                // Summary Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = buildString {
                                    append("共运动 ")
                                    append(uniqueDays)
                                    append(" 天")
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "保持运动习惯，继续加油！",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            item {
                // Detailed Grid
                StatsGrid(
                    totalDuration = totalDuration,
                    totalSessions = totalSessions,
                    totalCalories = totalCalories,
                    avgDuration = if (uniqueDays > 0) totalDuration / uniqueDays else 0
                )
            }
            
            item {
                Text(
                    "详细记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(filteredItems.sortedByDescending { it.date + it.time }) { item ->
                ExerciseHistoryItem(item)
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StatsTabRow(selectedTab: StatsTab, onTabSelected: (StatsTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatsTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            val bgColor = if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent
            val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            val shadowElevation = if (isSelected) 2.dp else 0.dp
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .shadow(shadowElevation, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun DateNavigator(tab: StatsTab, calendar: Calendar, onNavigate: (Int) -> Unit) {
    if (tab == StatsTab.TOTAL) return
    
    val format = when (tab) {
        StatsTab.WEEK -> "'第'w'周' yyyy"
        StatsTab.MONTH -> "yyyy'年'MM'月'"
        StatsTab.YEAR -> "yyyy'年'"
        else -> ""
    }
    val dateText = SimpleDateFormat(format, Locale.getDefault()).format(calendar.time)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onNavigate(-1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.ChevronLeft, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(
            onClick = { onNavigate(1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatsGrid(totalDuration: Int, totalSessions: Int, totalCalories: Int, avgDuration: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatsCardItem(
                modifier = Modifier.weight(1f),
                label = "总时长",
                value = formatDuration(totalDuration),
                icon = Icons.Default.Timer,
                color = MaterialTheme.colorScheme.primary
            )
            StatsCardItem(
                modifier = Modifier.weight(1f),
                label = "完成次数",
                value = "$totalSessions 次",
                icon = Icons.Default.Repeat,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatsCardItem(
                modifier = Modifier.weight(1f),
                label = "消耗热量",
                value = "$totalCalories kcal",
                icon = Icons.Default.Star,
                color = MaterialTheme.colorScheme.tertiary
            )
            StatsCardItem(
                modifier = Modifier.weight(1f),
                label = "日均时长",
                value = formatDuration(avgDuration),
                icon = Icons.Default.AccessTime,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StatsCardItem(
    modifier: Modifier = Modifier, 
    label: String, 
    value: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExerciseHistoryItem(item: CalorieItemEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                 Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.date} ${item.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${item.calories} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (item.notes?.contains("时长") == true) {
                    val duration = CalorieUtils.parseDuration(item.notes)
                    if (duration > 0) {
                         Text(
                            text = formatDuration(duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsBarChart(
    items: List<CalorieItemEntity>,
    tab: StatsTab,
    currentDate: Calendar,
    metric: ChartMetric,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    
    // Generate data based on tab
    val data = remember(items, tab, currentDate.timeInMillis, metric) {
        generateChartData(items, tab, currentDate, metric)
    }
    
    val maxVal = data.maxOfOrNull { it.second } ?: 1
    
    Canvas(modifier = modifier) {
        val barCount = data.size
        // Dynamic bar width based on count
        val barWidth = size.width / (barCount * 2f + 1f)
        val space = barWidth
        
        // Reduced corner radius
        val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        
        data.forEachIndexed { index, (label, value) ->
            val x = space + index * (barWidth + space)
            
            // Draw Track (Full Height) - Darker Gray
            drawRoundRect(
                color = Color.Gray.copy(alpha = 0.2f), // Darker than previous 0.3 surfaceVariant
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height - 24.dp.toPx()),
                cornerRadius = cornerRadius
            )
            
            // Draw Value Bar
            val maxBarHeight = size.height - 24.dp.toPx()
            val barHeight = (value / maxVal.toFloat()) * maxBarHeight
            
            if (barHeight > 0) {
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(x, maxBarHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            }
            
            // Label
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x + barWidth / 2,
                size.height,
                android.graphics.Paint().apply {
                    color = labelColor
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}

// Reuse helper functions or redefine if moved
fun filterItems(items: List<CalorieItemEntity>, tab: StatsTab, calendar: Calendar): List<CalorieItemEntity> {
    val exerciseItems = items.filter { it.type == "exercise" }
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    return when (tab) {
        StatsTab.WEEK -> {
            val c = calendar.clone() as Calendar
            c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
            val start = c.time
            c.add(Calendar.DAY_OF_WEEK, 6)
            val end = c.time
            
            val startStr = sdf.format(start)
            val endStr = sdf.format(end)
            exerciseItems.filter { it.date in startStr..endStr }
        }
        StatsTab.MONTH -> {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val prefix = String.format("%04d-%02d", year, month)
            exerciseItems.filter { it.date.startsWith(prefix) }
        }
        StatsTab.YEAR -> {
            val year = calendar.get(Calendar.YEAR)
            val prefix = String.format("%04d", year)
            exerciseItems.filter { it.date.startsWith(prefix) }
        }
        StatsTab.TOTAL -> exerciseItems
    }
}

fun generateChartData(
    items: List<CalorieItemEntity>, 
    tab: StatsTab, 
    calendar: Calendar,
    metric: ChartMetric
): List<Pair<String, Int>> {
    val durationMap = items.associate { it.id to CalorieUtils.parseDuration(it.notes) }
    
    fun getValue(item: CalorieItemEntity): Int {
        return if (metric == ChartMetric.DURATION) {
            durationMap[item.id] ?: 0
        } else {
            item.calories
        }
    }
    
    return when (tab) {
        StatsTab.WEEK -> {
            val c = calendar.clone() as Calendar
            c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
            (0..6).map { 
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.time)
                val dayLabel = SimpleDateFormat("E", Locale.getDefault()).format(c.time) // Mon, Tue...
                val total = items.filter { it.date == dateStr }.sumOf { getValue(it) }
                c.add(Calendar.DAY_OF_MONTH, 1)
                dayLabel to total
            }
        }
        StatsTab.MONTH -> {
            val c = calendar.clone() as Calendar
            c.set(Calendar.DAY_OF_MONTH, 1)
            val maxDays = c.getActualMaximum(Calendar.DAY_OF_MONTH)
            (1..maxDays).map { day ->
                val dateStr = String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, day)
                val total = items.filter { it.date == dateStr }.sumOf { getValue(it) }
                (if (day % 5 == 0 || day == 1) "$day" else "") to total // Sparse labels
            }
        }
        StatsTab.YEAR -> {
            (0..11).map { month ->
                val year = calendar.get(Calendar.YEAR)
                val prefix = String.format("%04d-%02d", year, month + 1)
                val total = items.filter { it.date.startsWith(prefix) }.sumOf { getValue(it) }
                "${month + 1}" to total
            }
        }
        StatsTab.TOTAL -> {
            // Show current year and past 4 years (total 5 years)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val startYear = currentYear - 4
            
            (startYear..currentYear).map { year ->
                val yearStr = year.toString()
                val total = items.filter { it.date.startsWith(yearStr) }.sumOf { getValue(it) }
                yearStr to total
            }
        }
    }
}

// parseDuration removed - using CalorieUtils.parseDuration

fun formatDuration(minutes: Int): String {
    if (minutes < 60) return "${minutes}分钟"
    val h = minutes / 60
    val m = minutes % 60
    return if (m > 0) "${h}小时${m}分" else "${h}小时"
}

fun getDateOffset(dateStr: String, offset: Int): String {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: Date()
        val c = Calendar.getInstance()
        c.time = date
        c.add(Calendar.DAY_OF_YEAR, offset)
        return sdf.format(c.time)
    } catch (e: Exception) {
        return dateStr
    }
}

fun calculateStreak(items: List<CalorieItemEntity>): Int {
    // Simplified streak logic from all exercise items
    val dates = items.filter { it.type == "exercise" }.map { it.date }.distinct().sortedDescending()
    if (dates.isEmpty()) return 0
    
    var streak = 1
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Check if streak is active (last date is today or yesterday)
    val today = sdf.format(Date())
    val yesterday = getDateOffset(today, -1)
    
    if (dates[0] != today && dates[0] != yesterday) return 0
    
    var currentDate = dates[0]
    for (i in 1 until dates.size) {
        if (dates[i] == getDateOffset(currentDate, -1)) {
            streak++
            currentDate = dates[i]
        } else {
            break
        }
    }
    return streak
}
