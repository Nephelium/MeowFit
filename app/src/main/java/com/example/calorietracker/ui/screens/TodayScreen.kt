package com.example.calorietracker.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import com.example.calorietracker.data.CalorieItemEntity
import com.example.calorietracker.data.DailyRecordEntity
import com.example.calorietracker.data.UserProfileEntity
import com.example.calorietracker.util.CalorieUtils
import com.example.calorietracker.util.ImageStorageUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ArrowDropDown

// ... existing imports ...

import androidx.compose.material.icons.filled.WaterDrop

import androidx.compose.material.icons.filled.Bedtime

data class TodayVisualTheme(
    val id: Int,
    val name: String,
    val lightBgColor: Int,
    val lightTopGradientColor: Int,
    val darkBgColor: Int,
    val darkTopGradientColor: Int,
    val patternEmoji: List<String>
)

val todayVisualThemePool = listOf(
    TodayVisualTheme(0, "清晨果园", "#FFFDE7".toColorInt(), "#FFF59D".toColorInt(), "#1B1A14".toColorInt(), "#2D2A1D".toColorInt(), listOf("🍎", "🥗", "🍇", "🥑")),
    TodayVisualTheme(1, "燃脂能量", "#FFEBEE".toColorInt(), "#FFCDD2".toColorInt(), "#1F1618".toColorInt(), "#3A1E24".toColorInt(), listOf("🔥", "💪", "🏃", "✨")),
    TodayVisualTheme(2, "海盐清蓝", "#E1F5FE".toColorInt(), "#B3E5FC".toColorInt(), "#111B22".toColorInt(), "#153448".toColorInt(), listOf("💧", "🌊", "🧊", "💙")),
    TodayVisualTheme(3, "夜眠薰衣", "#F3E5F5".toColorInt(), "#E1BEE7".toColorInt(), "#1A1520".toColorInt(), "#30203A".toColorInt(), listOf("💤", "🌙", "⭐", "🛌")),
    TodayVisualTheme(4, "猫系森绿", "#E8F5E9".toColorInt(), "#C8E6C9".toColorInt(), "#121A14".toColorInt(), "#1E3121".toColorInt(), listOf("🐱", "🐾", "🌿", "🍀")),
    TodayVisualTheme(5, "暖阳蔬果", "#FFF3E0".toColorInt(), "#FFCC80".toColorInt(), "#20170F".toColorInt(), "#3C2A16".toColorInt(), listOf("🥕", "🍊", "🌞", "🌻"))
)

fun getTodayVisualTheme(index: Int): TodayVisualTheme {
    val safeIndex = index.coerceIn(0, todayVisualThemePool.lastIndex)
    return todayVisualThemePool[safeIndex]
}

fun calculatePerceivedLuminance(color: Color): Float {
    return 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
}

@Composable
fun TodayBackground(theme: TodayVisualTheme, seed: Int, isDarkTheme: Boolean, modifier: Modifier = Modifier) {
    val bgColor = if (isDarkTheme) Color(theme.darkBgColor) else Color(theme.lightBgColor)
    val gradientColor = if (isDarkTheme) Color(theme.darkTopGradientColor) else Color(theme.lightTopGradientColor)
    Canvas(modifier = modifier) {
        drawRect(color = bgColor)
        val gradientEndY = minOf(size.height, 520.dp.toPx())
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    gradientColor.copy(alpha = if (isDarkTheme) 0.92f else 0.82f),
                    bgColor.copy(alpha = 0f)
                ),
                startY = 0f,
                endY = gradientEndY
            )
        )
        drawIntoCanvas { canvas ->
            val widthScale = size.width / 1080f
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 100f * widthScale
                color = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                alpha = if (isDarkTheme) 34 else 24
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val cols = 5
            val gapX = size.width / cols
            val gapY = 300f * widthScale
            val rows = (size.height / gapY).toInt() + 2
            val random = java.util.Random(seed.toLong())
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val emoji = theme.patternEmoji[random.nextInt(theme.patternEmoji.size)]
                    val x = c * gapX + gapX / 2f + (random.nextFloat() - 0.5f) * (50f * widthScale)
                    val y = r * gapY + gapY / 2f + (random.nextFloat() - 0.5f) * (50f * widthScale)
                    val rotation = (random.nextFloat() - 0.5f) * 60f
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.rotate(rotation, x, y)
                    canvas.nativeCanvas.drawText(emoji, x, y, paint)
                    canvas.nativeCanvas.restore()
                }
            }
        }
    }
}

fun themedDashboardCardColor(theme: TodayVisualTheme, isDarkTheme: Boolean): Color {
    val baseCardColor = if (isDarkTheme) Color(theme.darkBgColor) else Color(theme.lightBgColor)
    return if (isDarkTheme) {
        val liftedFromBackground = lerp(baseCardColor, Color.White, 0.16f)
        lerp(liftedFromBackground, Color(0xFF303236), 0.12f).copy(alpha = 0.97f)
    } else {
        lerp(baseCardColor, Color.White, 0.72f).copy(alpha = 0.96f)
    }
}

fun themedAccentColor(theme: TodayVisualTheme, isDarkTheme: Boolean): Color {
    val source = if (isDarkTheme) Color(theme.darkTopGradientColor) else Color(theme.lightTopGradientColor)
    return if (isDarkTheme) lerp(source, Color.White, 0.40f) else lerp(source, Color(0xFF2B2B2B), 0.12f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    userProfile: UserProfileEntity?,
    dailyRecord: DailyRecordEntity?,
    allRecords: List<DailyRecordEntity>,
    items: List<CalorieItemEntity>,
    selectedDate: String,
    onDateChange: (String) -> Unit,
    onAddClick: (String) -> Unit,
    onDeleteItem: (CalorieItemEntity) -> Unit,
    onUpdateItem: (CalorieItemEntity) -> Unit,
    onUpdateWeight: (Float) -> Unit,
    onSaveExercise: (String, Int, String, String) -> Unit, // name, calories, startTime, endTime
    onUpdateWater: (Int) -> Unit,
    onUpdateSleep: (Int) -> Unit // minutes
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val selectedThemeIndex = userProfile?.selectedTodayThemeIndex ?: 0
    val selectedVisualTheme = remember(selectedThemeIndex) { getTodayVisualTheme(selectedThemeIndex) }
    val backgroundSeed = remember(selectedThemeIndex) { (selectedThemeIndex + 1) * 1031 }
    val dashboardCardColor = remember(selectedVisualTheme, isDarkTheme) {
        themedDashboardCardColor(selectedVisualTheme, isDarkTheme)
    }
    val dashboardOnCardColor = if (isDarkTheme) Color.White else if (calculatePerceivedLuminance(dashboardCardColor) > 0.5f) Color(0xFF1E1E1E) else Color(0xFFF4F4F4)
    val dialogAccentColor = remember(selectedVisualTheme, isDarkTheme) {
        themedAccentColor(selectedVisualTheme, isDarkTheme)
    }
    val timerCardColor = if (isDarkTheme) {
        lerp(dashboardCardColor, dialogAccentColor, 0.26f)
    } else {
        lerp(dashboardCardColor, dialogAccentColor, 0.18f)
    }
    val timerOnCardColor = if (isDarkTheme) Color.White else if (calculatePerceivedLuminance(timerCardColor) > 0.5f) Color(0xFF1E1E1E) else Color(0xFFF4F4F4)
    var showWeightDialog by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerStartTime by remember { mutableStateOf<Long?>(null) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var exerciseName by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<CalorieItemEntity?>(null) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }
    var previewSavedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewShareBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareShowNotes by remember { mutableStateOf(true) }
    var shareMaskWeight by remember { mutableStateOf(false) }

    // Calculate effective weight for today (or selected date)
    // If dailyRecord.weight is set, use it. Otherwise find previous.
    val effectiveWeight = remember(dailyRecord, allRecords, selectedDate, userProfile) {
        CalorieUtils.getEffectiveWeight(selectedDate, allRecords, userProfile)
    }
    val breakfastItems = remember(items) { items.filter { it.type == "food" && CalorieUtils.getMealCategoryByTime(it.time) == CalorieUtils.MealCategory.BREAKFAST }.sortedBy { it.time } }
    val lunchItems = remember(items) { items.filter { it.type == "food" && CalorieUtils.getMealCategoryByTime(it.time) == CalorieUtils.MealCategory.LUNCH }.sortedBy { it.time } }
    val dinnerItems = remember(items) { items.filter { it.type == "food" && CalorieUtils.getMealCategoryByTime(it.time) == CalorieUtils.MealCategory.DINNER }.sortedBy { it.time } }
    val nightSnackItems = remember(items) { items.filter { it.type == "food" && CalorieUtils.getMealCategoryByTime(it.time) == CalorieUtils.MealCategory.NIGHT_SNACK }.sortedBy { it.time } }
    val exerciseItems = remember(items) { items.filter { it.type == "exercise" }.sortedByDescending { it.time } }

    if (!previewImagePath.isNullOrBlank()) {
        Dialog(onDismissRequest = { previewImagePath = null }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                AsyncImage(
                    model = File(previewImagePath!!),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp, max = 520.dp)
                        .padding(12.dp)
                )
            }
        }
    }

    if (previewSavedBitmap != null) {
        Dialog(onDismissRequest = { previewSavedBitmap = null }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Image(
                    bitmap = previewSavedBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp, max = 520.dp)
                        .padding(12.dp)
                )
            }
        }
    }

    if (previewShareBitmap != null) {
        Dialog(onDismissRequest = { previewShareBitmap = null }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        bitmap = previewShareBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp, max = 520.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { previewShareBitmap = null }) {
                            Text("取消")
                        }
                        TextButton(onClick = {
                            try {
                                shareTodayBitmap(context, previewShareBitmap!!)
                            } catch (e: Exception) {
                                Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            previewShareBitmap = null
                        }) {
                            Text("分享给朋友")
                        }
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("分享今日记录") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("可保存为图片，或先预览后分享给好友")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("显示备注")
                        Switch(
                            checked = shareShowNotes,
                            onCheckedChange = { shareShowNotes = it }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("体重打码")
                        Switch(
                            checked = shareMaskWeight,
                            onCheckedChange = { shareMaskWeight = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val bitmap = generateTodayLongScreenshot(
                            context = context,
                            userProfile = userProfile,
                            dailyRecord = dailyRecord,
                            allRecords = allRecords,
                            items = items,
                            selectedDate = selectedDate,
                            selectedThemeIndex = selectedThemeIndex,
                            showNotes = shareShowNotes,
                            maskWeight = shareMaskWeight
                        )
                        val saved = saveTodayBitmap(context, bitmap)
                        if (saved) previewSavedBitmap = bitmap
                    } catch (e: Exception) {
                        Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    showShareDialog = false
                }) {
                    Text("保存图片")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        try {
                            val bitmap = generateTodayLongScreenshot(
                                context = context,
                                userProfile = userProfile,
                                dailyRecord = dailyRecord,
                                allRecords = allRecords,
                                items = items,
                                selectedDate = selectedDate,
                                selectedThemeIndex = selectedThemeIndex,
                                showNotes = shareShowNotes,
                                maskWeight = shareMaskWeight
                            )
                            previewShareBitmap = bitmap
                        } catch (e: Exception) {
                            Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        showShareDialog = false
                    }) {
                        Text("分享")
                    }
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("取消")
                    }
                }
            },
            containerColor = dashboardCardColor,
            titleContentColor = dashboardOnCardColor,
            textContentColor = dashboardOnCardColor
        )
    }

    if (editingItem != null) {
        EditRecordDialog(
            item = editingItem!!,
            onDismiss = { editingItem = null },
            onConfirm = { updatedItem ->
                onUpdateItem(updatedItem)
                editingItem = null
            },
            containerColor = dashboardCardColor,
            onContainerColor = dashboardOnCardColor,
            accentColor = dialogAccentColor
        )
    }

    if (showWeightDialog) {
        WeightDialog(
            currentWeight = dailyRecord?.weight ?: effectiveWeight,
            onDismiss = { showWeightDialog = false },
            onConfirm = { 
                onUpdateWeight(it)
                showWeightDialog = false
            },
            containerColor = dashboardCardColor,
            onContainerColor = dashboardOnCardColor,
            accentColor = dialogAccentColor
        )
    }

    if (showWaterDialog) {
        WaterDialog(
            currentWater = dailyRecord?.totalWater ?: 0,
            onDismiss = { showWaterDialog = false },
            onConfirm = { 
                onUpdateWater(it)
                showWaterDialog = false
            },
            containerColor = dashboardCardColor,
            onContainerColor = dashboardOnCardColor,
            accentColor = dialogAccentColor
        )
    }

    if (showSleepDialog) {
        SleepDialog(
            currentDuration = dailyRecord?.sleepDuration ?: 0,
            onDismiss = { showSleepDialog = false },
            onConfirm = { 
                onUpdateSleep(it)
                showSleepDialog = false
            },
            containerColor = dashboardCardColor,
            onContainerColor = dashboardOnCardColor,
            accentColor = dialogAccentColor
        )
    }

    if (showTimerDialog) {
        ExerciseTimerDialog(
            initialName = exerciseName,
            startTime = timerStartTime,
            onDismiss = { 
                showTimerDialog = false 
            },
            onSave = { name, calories, start, end ->
                onSaveExercise(name, calories, start, end)
                showTimerDialog = false
                isTimerRunning = false
                timerStartTime = null
                exerciseName = ""
            },
            onDiscard = {
                showTimerDialog = false
                isTimerRunning = false
                timerStartTime = null
                exerciseName = ""
            },
            containerColor = dashboardCardColor,
            onContainerColor = dashboardOnCardColor,
            accentColor = dialogAccentColor
        )
    }
    
    // Initial Timer Setup Dialog
    if (isTimerRunning && timerStartTime == null) {
        // Just started, show dialog to pick exercise
        AlertDialog(
            onDismissRequest = { isTimerRunning = false },
            title = { Text("开始运动") },
            text = {
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = { Text("运动名称 (如: 跑步)") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exerciseName.isNotBlank()) {
                            timerStartTime = System.currentTimeMillis()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = dialogAccentColor,
                        contentColor = Color.White
                    )
                ) {
                    Text("开始计时")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { isTimerRunning = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = dialogAccentColor)
                ) {
                    Text("取消")
                }
            },
            containerColor = dashboardCardColor,
            titleContentColor = dashboardOnCardColor,
            textContentColor = dashboardOnCardColor
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            calendar.time = sdf.parse(selectedDate)!!
        } catch (e: Exception) {}
        
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = calendar.timeInMillis
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(
                containerColor = dashboardCardColor,
                titleContentColor = dashboardOnCardColor,
                headlineContentColor = dialogAccentColor,
                weekdayContentColor = dashboardOnCardColor.copy(alpha = 0.75f),
                subheadContentColor = dashboardOnCardColor.copy(alpha = 0.75f),
                yearContentColor = dashboardOnCardColor,
                currentYearContentColor = dialogAccentColor,
                selectedYearContentColor = dashboardOnCardColor,
                selectedYearContainerColor = dialogAccentColor.copy(alpha = 0.24f),
                dayContentColor = dashboardOnCardColor,
                selectedDayContentColor = dashboardOnCardColor,
                selectedDayContainerColor = dialogAccentColor,
                todayContentColor = dialogAccentColor,
                todayDateBorderColor = dialogAccentColor
            ),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            calendar.timeInMillis = millis
                            onDateChange(sdf.format(calendar.time))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定", color = dialogAccentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消", color = dashboardOnCardColor.copy(alpha = 0.82f))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Water FAB removed as per user request

                // Sleep FAB removed as per user request

                // Timer FAB
                FloatingActionButton(
                    onClick = {
                        if (isTimerRunning && timerStartTime != null) {
                            // Stop Timer
                            showTimerDialog = true
                        } else {
                            // Start Timer
                            isTimerRunning = true
                        }
                    },
                    containerColor = if (isTimerRunning) MaterialTheme.colorScheme.error else Color(0xFF2196F3),
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(
                        if (isTimerRunning) Icons.Default.Stop else Icons.Default.Timer, 
                        if (isTimerRunning) "Stop Timer" else "Start Timer"
                    )
                }

                FloatingActionButton(
                    onClick = {
                        onAddClick(selectedDate)
                    },
                    containerColor = dialogAccentColor,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, "Add Item")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            TodayBackground(
                theme = selectedVisualTheme,
                seed = backgroundSeed,
                isDarkTheme = isDarkTheme,
                modifier = Modifier
                    .matchParentSize()
                    .blur(if (isDarkTheme) 22.dp else 10.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                val dateNavColor = if (isDarkTheme) Color.White else dashboardOnCardColor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        try {
                            cal.time = sdf.parse(selectedDate)!!
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                            onDateChange(sdf.format(cal.time))
                        } catch (_: Exception) {}
                    }) {
                        Icon(Icons.Default.ArrowBack, "Previous Day", tint = dateNavColor)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDatePicker = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            CalorieUtils.formatDate(selectedDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = dateNavColor
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Date",
                            modifier = Modifier.size(20.dp),
                            tint = dateNavColor
                        )
                    }
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, "Share Today", tint = dateNavColor)
                    }
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        try {
                            cal.time = sdf.parse(selectedDate)!!
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            onDateChange(sdf.format(cal.time))
                        } catch (_: Exception) {}
                    }) {
                        Icon(Icons.Default.ArrowForward, "Next Day", tint = dateNavColor)
                    }
                }
            }
            // Timer Status Banner
            if (isTimerRunning && timerStartTime != null) {
                item {
                    TimerStatusCard(
                        name = exerciseName,
                        startTime = timerStartTime!!,
                        containerColor = timerCardColor,
                        onContainerColor = timerOnCardColor,
                        isDarkTheme = isDarkTheme,
                        accentColor = dialogAccentColor
                    )
                }
            }

            // Summary Card
            item {
                SummaryCard(userProfile, dailyRecord, effectiveWeight, dashboardCardColor, dashboardOnCardColor, isDarkTheme)
            }
// ...

            // Weight Card
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        WeightCard(
                            weight = dailyRecord?.weight,
                            onEdit = { showWeightDialog = true },
                            containerColor = dashboardCardColor,
                            onContainerColor = dashboardOnCardColor,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WaterCard(
                            water = dailyRecord?.totalWater ?: 0,
                            onEdit = { showWaterDialog = true },
                            containerColor = dashboardCardColor,
                            onContainerColor = dashboardOnCardColor,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SleepCard(
                            duration = dailyRecord?.sleepDuration ?: 0,
                            onEdit = { showSleepDialog = true },
                            containerColor = dashboardCardColor,
                            onContainerColor = dashboardOnCardColor,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }

            // Records List Header
            item {
                Text(
                    "今日记录",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            if (items.isEmpty()) {
                item {
                    EmptyState()
                }
            }
            if (breakfastItems.isNotEmpty()) {
                item { RecordSectionHeader("早餐", breakfastItems.sumOf { it.calories }, MaterialTheme.colorScheme.primary) }
                items(breakfastItems) { item ->
                    RecordItem(item = item, onDelete = onDeleteItem, onEdit = { editingItem = it }, onImagePreview = { previewImagePath = it }, containerColor = dashboardCardColor, onContainerColor = dashboardOnCardColor, isDarkTheme = isDarkTheme)
                }
            }
            if (lunchItems.isNotEmpty()) {
                item { RecordSectionHeader("午餐", lunchItems.sumOf { it.calories }, Color(0xFF26A69A)) }
                items(lunchItems) { item ->
                    RecordItem(item = item, onDelete = onDeleteItem, onEdit = { editingItem = it }, onImagePreview = { previewImagePath = it }, containerColor = dashboardCardColor, onContainerColor = dashboardOnCardColor, isDarkTheme = isDarkTheme)
                }
            }
            if (dinnerItems.isNotEmpty()) {
                item { RecordSectionHeader("晚餐", dinnerItems.sumOf { it.calories }, Color(0xFFFF7043)) }
                items(dinnerItems) { item ->
                    RecordItem(item = item, onDelete = onDeleteItem, onEdit = { editingItem = it }, onImagePreview = { previewImagePath = it }, containerColor = dashboardCardColor, onContainerColor = dashboardOnCardColor, isDarkTheme = isDarkTheme)
                }
            }
            if (nightSnackItems.isNotEmpty()) {
                item { RecordSectionHeader("宵夜", nightSnackItems.sumOf { it.calories }, Color(0xFF7E57C2)) }
                items(nightSnackItems) { item ->
                    RecordItem(item = item, onDelete = onDeleteItem, onEdit = { editingItem = it }, onImagePreview = { previewImagePath = it }, containerColor = dashboardCardColor, onContainerColor = dashboardOnCardColor, isDarkTheme = isDarkTheme)
                }
            }
            if (exerciseItems.isNotEmpty()) {
                item { RecordSectionHeader("运动", exerciseItems.sumOf { it.calories }, MaterialTheme.colorScheme.secondary) }
                items(exerciseItems) { item ->
                    RecordItem(item = item, onDelete = onDeleteItem, onEdit = { editingItem = it }, onImagePreview = { previewImagePath = it }, containerColor = dashboardCardColor, onContainerColor = dashboardOnCardColor, isDarkTheme = isDarkTheme)
                }
            }
            
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun EditRecordDialog(
    item: CalorieItemEntity,
    onDismiss: () -> Unit,
    onConfirm: (CalorieItemEntity) -> Unit,
    containerColor: Color,
    onContainerColor: Color,
    accentColor: Color
) {
    var name by remember { mutableStateOf(item.name) }
    var calories by remember { mutableStateOf(item.calories.toString()) }
    var carbs by remember { mutableStateOf(item.carbs.toString()) }
    var protein by remember { mutableStateOf(item.protein.toString()) }
    var fat by remember { mutableStateOf(item.fat.toString()) }
    var time by remember { mutableStateOf(item.time) }
    var notes by remember { mutableStateOf(item.notes ?: "") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var currentImagePath by remember { mutableStateOf(item.imageUrl) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }
    val context = LocalContext.current
    
    // Check if it's an exercise with duration
    val isExercise = item.type == "exercise"
    // Try to parse start/end time from notes if available or just use 'time' as start
    // Typically exercise items might store "Start" in time, and "Duration" in notes.
    // If we want to edit Start and End, we need to know End.
    // If not available, we default to time + duration.
    
    // Logic: 
    // 1. Parse duration from notes (e.g. "时长: 30分钟")
    // 2. Calculate End Time based on Time + Duration
    // 3. Allow editing Start and End
    // 4. On Save, recalculate Duration and update Notes
    
    var startTimeStr by remember { mutableStateOf(item.time) }
    var endTimeStr by remember { mutableStateOf("") }
    
    LaunchedEffect(item) {
        if (isExercise) {
            // Parse duration
            val durationMinutes = CalorieUtils.parseDuration(item.notes).toLong()
            if (durationMinutes > 0) {
                try {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val start = sdf.parse(item.time)
                    if (start != null) {
                        val endMillis = start.time + durationMinutes * 60 * 1000
                        endTimeStr = sdf.format(java.util.Date(endMillis))
                    }
                } catch (e: Exception) {
                    endTimeStr = item.time // Fallback
                }
            } else {
                endTimeStr = item.time // Fallback
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("编辑记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onContainerColor)
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("卡路里 (kcal)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (!isExercise) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { if (it.all { char -> char.isDigit() }) carbs = it },
                            label = { Text("碳水") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { if (it.all { char -> char.isDigit() }) protein = it },
                            label = { Text("蛋白质") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fat,
                            onValueChange = { if (it.all { char -> char.isDigit() }) fat = it },
                            label = { Text("脂肪") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                if (isExercise) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startTimeStr,
                            onValueChange = { startTimeStr = it },
                            label = { Text("开始时间") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endTimeStr,
                            onValueChange = { endTimeStr = it },
                            label = { Text("结束时间") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("时间") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注") },
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = containerColor.copy(alpha = 0.96f),
                            contentColor = accentColor
                        ),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.56f))
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (selectedImageUri == null && currentImagePath.isNullOrBlank()) "添加备注图片" else "更换图片", color = accentColor)
                    }
                    if (selectedImageUri != null || !currentImagePath.isNullOrBlank()) {
                        TextButton(onClick = {
                            selectedImageUri = null
                            currentImagePath = null
                        }) {
                            Text("移除", color = accentColor)
                        }
                    }
                }

                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else if (!currentImagePath.isNullOrBlank() && File(currentImagePath!!).exists()) {
                    AsyncImage(
                        model = File(currentImagePath!!),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor.copy(alpha = 0.72f),
                            contentColor = Color.White
                        )
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                        val cal = calories.toIntOrNull()
                        val c = carbs.toIntOrNull() ?: 0
                        val p = protein.toIntOrNull() ?: 0
                        val f = fat.toIntOrNull() ?: 0
                        val finalImagePath = selectedImageUri?.let { ImageStorageUtils.compressAndSaveImage(context, it) } ?: currentImagePath
                        
                        if (name.isNotBlank() && cal != null) {
                            if (isExercise) {
                                // Recalculate duration and update notes
                                var newNotes = notes
                                try {
                                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    val start = sdf.parse(startTimeStr)
                                    val end = sdf.parse(endTimeStr)
                                    if (start != null && end != null) {
                                        var diff = end.time - start.time
                                        if (diff < 0) diff += 24 * 60 * 60 * 1000
                                        val minutes = diff / (1000 * 60)
                                        // Update duration in notes. Regex replace or append?
                                        // Simple approach: Replace existing duration string or append
                                        if (newNotes.contains("时长:")) {
                                            newNotes = newNotes.replace(Regex("时长:\\s*\\d+\\s*分钟"), "时长: ${minutes}分钟")
                                        } else {
                                            newNotes = if (newNotes.isBlank()) "时长: ${minutes}分钟" else "$newNotes, 时长: ${minutes}分钟"
                                        }
                                    }
                                } catch (e: Exception) {}
                                
                                onConfirm(item.copy(name = name, calories = cal, carbs = 0, protein = 0, fat = 0, time = startTimeStr, notes = newNotes, imageUrl = finalImagePath))
                            } else {
                                onConfirm(item.copy(name = name, calories = cal, carbs = c, protein = p, fat = f, time = time, notes = notes, imageUrl = finalImagePath))
                            }
                        }
                    },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

// Helper to reuse parseDuration removed - using CalorieUtils.parseDuration

@Composable
fun SummaryCard(
    userProfile: UserProfileEntity?,
    dailyRecord: DailyRecordEntity?,
    effectiveWeight: Float,
    containerColor: Color,
    onContainerColor: Color,
    isDarkTheme: Boolean
) {
    val secondaryTextColor = onContainerColor.copy(alpha = 0.72f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 10.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Target is treated as BMR/Base TDEE, calculated dynamically based on effective weight
            val target = if (userProfile != null) {
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
            val intake = dailyRecord?.totalIntake ?: 0
            val burned = dailyRecord?.totalBurned ?: 0
            
            // Formula: Balance = Intake - (BMR + Burned)
            // Negative is Good (Deficit), Positive is Bad (Surplus)
            val balance = intake - (target + burned)
            
            val isSurplus = balance > 0
            val statusColor = if (isSurplus) MaterialTheme.colorScheme.error else if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32)
            val statusText = if (isSurplus) "今日热量盈余" else "今日热量缺口"
            val balanceAbs = Math.abs(balance)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("今日状态", style = MaterialTheme.typography.labelMedium, color = secondaryTextColor)
                    Text(
                        statusText, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = statusColor
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${if (isSurplus) "+" else "-"}$balanceAbs",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text("kcal", style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Visualization Bar (Center Zero)
            // Let's make a simple bar: [ Intake ] vs [ Target + Burned ]
            // Or just a visual indicator of where we are.
            // Simplified: "Intake" vs "Limit"
            val limit = target + burned
            val progress = (intake.toFloat() / limit.toFloat()).coerceIn(0f, 1.5f) // Allow over 100%
            
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("已摄入 $intake", style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
                    Text("限额 $limit", style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (progress).coerceAtMost(1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = if (progress > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                // Macros (if enabled)
                if (userProfile?.showMacros == true) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val macroTargets = CalorieUtils.calculateMacroTargets(
                        weight = effectiveWeight,
                        goal = userProfile.goal,
                        dailyCalorieTarget = target
                    )
                    
                    val currentCarbs = dailyRecord?.totalCarbs ?: 0
                    val currentProtein = dailyRecord?.totalProtein ?: 0
                    val currentFat = dailyRecord?.totalFat ?: 0
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MacroProgressBar(
                            label = "碳水化合物",
                            current = currentCarbs,
                            target = macroTargets.first,
                            color = Color(0xFF69F0AE), // Cyan/Greenish
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )
                        MacroProgressBar(
                            label = "蛋白质",
                            current = currentProtein,
                            target = macroTargets.second,
                            color = Color(0xFF40C4FF), // Light Blue
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        MacroProgressBar(
                            label = "脂肪",
                            current = currentFat,
                            target = macroTargets.third,
                            color = Color(0xFFFF8A80), // Pink/Red
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("基础消耗", "$target", MaterialTheme.colorScheme.secondary)
                StatItem("运动消耗", "$burned", MaterialTheme.colorScheme.tertiary)
                StatItem("总摄入", "$intake", MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun MacroProgressBar(
    label: String,
    current: Int,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) (current.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$current / ${target}克",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WeightCard(weight: Float?, onEdit: () -> Unit, containerColor: Color, onContainerColor: Color, isDarkTheme: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("今日体重", style = MaterialTheme.typography.bodyMedium, color = onContainerColor.copy(alpha = 0.72f))
                FilledIconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (weight != null) "$weight" else "记录",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = onContainerColor
                )
                if (weight != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "kg",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = onContainerColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WaterCard(water: Int, onEdit: () -> Unit, containerColor: Color, onContainerColor: Color, isDarkTheme: Boolean) {
    val accent = if (isDarkTheme) Color(0xFF81D4FA) else Color(0xFF1E88E5)
    val accentContainer = if (isDarkTheme) Color(0xFF163445) else Color(0xFFE3F2FD)
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("今日饮水", style = MaterialTheme.typography.bodyMedium, color = onContainerColor.copy(alpha = 0.72f))
                FilledIconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = accentContainer)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = accent, modifier = Modifier.size(14.dp))
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$water",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ml",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SleepCard(duration: Int, onEdit: () -> Unit, containerColor: Color, onContainerColor: Color, isDarkTheme: Boolean) {
    val accent = if (isDarkTheme) Color(0xFFB39DDB) else Color(0xFF673AB7)
    val accentContainer = if (isDarkTheme) Color(0xFF2B2242) else Color(0xFFEDE7F6)
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("今日睡眠", style = MaterialTheme.typography.bodyMedium, color = onContainerColor.copy(alpha = 0.72f))
                FilledIconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = accentContainer)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = accent, modifier = Modifier.size(14.dp))
                }
            }
            val hours = duration / 60
            val minutes = duration % 60
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$hours",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "h",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$minutes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "m",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("暂无记录，快去添加吧", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RecordSectionHeader(title: String, calories: Int, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = accentColor,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${calories} kcal",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun loadEmojiKeywordRulesFromFile(context: Context): List<Pair<String, List<String>>> {
    return try {
        context.assets.open("emoji_rules.txt").bufferedReader().useLines { lines ->
            lines.mapNotNull { rawLine ->
                val line = rawLine.trim()
                if (line.isBlank()) return@mapNotNull null
                if (line.startsWith("#")) return@mapNotNull null
                val delimiterIndex = line.indexOf('=').takeIf { it >= 0 } ?: line.indexOf('＝')
                if (delimiterIndex < 0) return@mapNotNull null
                val emoji = line.substring(0, delimiterIndex).trim()
                val keywordBlock = line.substring(delimiterIndex + 1).trim()
                val keywords = keywordBlock
                    .split(Regex("[,，、|]"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                if (emoji.isBlank() || keywords.isEmpty()) return@mapNotNull null
                emoji to keywords
            }.toList()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun resolveDefaultEmoji(context: Context, name: String, type: String): String {
    val locale = Locale.getDefault()
    val primaryText = name.trim().lowercase(locale)
    val rules = loadEmojiKeywordRulesFromFile(context)

    rules.lastOrNull { (_, keywords) ->
        keywords.any { keyword ->
            val normalized = keyword.trim().lowercase(locale)
            normalized.isNotBlank() && primaryText == normalized
        }
    }?.let {
        return it.first
    }

    fun findBestEmoji(text: String): String? {
        var bestEmoji: String? = null
        var bestStart = -1
        var bestLength = -1
        var bestRuleIndex = -1
        rules.forEachIndexed { ruleIndex, (emoji, keywords) ->
            keywords.forEach { keyword ->
                val normalized = keyword.trim().lowercase(locale)
                if (normalized.isBlank()) return@forEach
                val start = text.lastIndexOf(normalized)
                if (start < 0) return@forEach
                val better = start > bestStart ||
                    (start == bestStart && normalized.length > bestLength) ||
                    (start == bestStart && normalized.length == bestLength && ruleIndex > bestRuleIndex)
                if (better) {
                    bestEmoji = emoji
                    bestStart = start
                    bestLength = normalized.length
                    bestRuleIndex = ruleIndex
                }
            }
        }
        return bestEmoji
    }

    findBestEmoji(primaryText)?.let { return it }
    return if (type == "food") "🍽️" else "💪"
}

@Composable
fun RecordItem(
    item: CalorieItemEntity,
    onDelete: (CalorieItemEntity) -> Unit,
    onEdit: (CalorieItemEntity) -> Unit,
    onImagePreview: (String) -> Unit = {},
    containerColor: Color,
    onContainerColor: Color,
    isDarkTheme: Boolean,
    showDeleteButton: Boolean = true
) {
    val context = LocalContext.current
    val notesText = remember(item.notes) { item.notes?.trim().orEmpty() }
    val imagePath = remember(item.imageUrl) {
        item.imageUrl?.trim()?.takeIf { it.isNotBlank() && (it.startsWith("content://") || File(it).exists()) }
    }
    val fallbackEmoji = resolveDefaultEmoji(context, item.name, item.type)
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 6.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(item) }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isFood = item.type == "food"
                val iconBg = if (isFood) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer

                if (!imagePath.isNullOrBlank()) {
                    val imageModel = if (imagePath.startsWith("content://")) Uri.parse(imagePath) else File(imagePath)
                    AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onImagePreview(imagePath)
                            }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = fallbackEmoji, fontSize = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onContainerColor
                    )
                    Text(
                        "${item.time} · ${if(isFood) "食物" else "运动"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainerColor.copy(alpha = 0.72f)
                    )
                    if (notesText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            notesText,
                            style = MaterialTheme.typography.bodySmall,
                            color = onContainerColor.copy(alpha = 0.72f),
                            maxLines = 2
                        )
                    }
                }

                Text(
                    "${if (isFood) "+" else "-"}${item.calories}",
                    modifier = Modifier.width(68.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (showDeleteButton) {
                IconButton(
                    onClick = { onDelete(item) },
                    modifier = Modifier.align(Alignment.TopEnd).size(30.dp).padding(top = 2.dp, end = 2.dp)
                ) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun TimerStatusCard(
    name: String,
    startTime: Long,
    containerColor: Color,
    onContainerColor: Color,
    isDarkTheme: Boolean,
    accentColor: Color
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    
    val durationSeconds = (currentTime - startTime) / 1000
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60
    val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 8.dp),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isDarkTheme) 0.85f else 1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FitnessCenter, null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("正在进行: $name", style = MaterialTheme.typography.labelMedium, color = onContainerColor.copy(alpha = 0.75f))
                Text(timeStr, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onContainerColor)
            }
            // Pulse animation or icon could go here
        }
    }
}

@Composable
fun ExerciseTimerDialog(
    initialName: String,
    startTime: Long?,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, String) -> Unit,
    onDiscard: () -> Unit,
    containerColor: Color,
    onContainerColor: Color,
    accentColor: Color
) {
    var name by remember { mutableStateOf(initialName) }
    var calories by remember { mutableStateOf("") }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("确认放弃") },
            text = { Text("确定要放弃本次运动记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        onDiscard()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                ) { Text("取消") }
            },
            containerColor = containerColor,
            titleContentColor = onContainerColor,
            textContentColor = onContainerColor
        )
    }
    
    // Calculate duration
    val endTime = System.currentTimeMillis()
    val start = startTime ?: endTime
    val durationMillis = endTime - start
    val durationSeconds = durationMillis / 1000
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60
    
    val durationStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    
    // Format times for display/storage
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startStr = sdf.format(java.util.Date(start))
    val endStr = sdf.format(java.util.Date(endTime))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("运动结束", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onContainerColor)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Duration Display
                Text("运动时长", style = MaterialTheme.typography.labelMedium, color = onContainerColor.copy(alpha = 0.75f))
                Text(
                    durationStr,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text("$startStr - $endStr", style = MaterialTheme.typography.bodySmall, color = onContainerColor.copy(alpha = 0.75f))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("运动名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("消耗卡路里 (kcal)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showDiscardConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                    ) {
                        Text("放弃")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cal = calories.toIntOrNull()
                            if (name.isNotBlank() && cal != null && cal > 0) {
                                onSave(name, cal, startStr, endStr)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text("保存记录")
                    }
                }
            }
        }
    }
}

@Composable
fun SleepDialog(
    currentDuration: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    containerColor: Color,
    onContainerColor: Color,
    accentColor: Color
) {
    var hours by remember { mutableStateOf((currentDuration / 60).toString()) }
    var minutes by remember { mutableStateOf((currentDuration % 60).toString()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("记录睡眠", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onContainerColor)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it },
                        label = { Text("小时") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it },
                        label = { Text("分钟") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val h = hours.toIntOrNull() ?: 0
                        val m = minutes.toIntOrNull() ?: 0
                        val total = h * 60 + m
                        if (total >= 0) {
                            onConfirm(total)
                        }
                    }) {
                        Text("保存", color = accentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun WaterDialog(
    currentWater: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    containerColor: Color,
    onContainerColor: Color,
    accentColor: Color
) {
    var waterStr by remember { mutableStateOf(if (currentWater > 0) currentWater.toString() else "") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("记录饮水", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onContainerColor)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = waterStr,
                    onValueChange = { waterStr = it },
                    label = { Text("饮水量 (ml)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick Add Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(200, 350, 500).forEach { amount ->
                        OutlinedButton(
                            onClick = { 
                                val current = waterStr.toIntOrNull() ?: 0
                                waterStr = (current + amount).toString()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+$amount")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val w = waterStr.toIntOrNull()
                        if (w != null && w >= 0) {
                            onConfirm(w)
                        }
                    }) {
                        Text("保存", color = accentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun WeightDialog(
    currentWeight: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
    containerColor: Color,
    onContainerColor: Color,
    accentColor: Color
) {
    var weightStr by remember { mutableStateOf(currentWeight.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("记录今日体重", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onContainerColor)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("体重 (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val w = weightStr.toFloatOrNull()
                        if (w != null && w > 0) {
                            onConfirm(w)
                        }
                    }) {
                        Text("保存", color = accentColor)
                    }
                }
            }
        }
    }
}

fun generateTodayLongScreenshot(
    context: Context,
    userProfile: UserProfileEntity?,
    dailyRecord: DailyRecordEntity?,
    allRecords: List<DailyRecordEntity>,
    items: List<CalorieItemEntity>,
    selectedDate: String,
    selectedThemeIndex: Int = 0,
    showNotes: Boolean = true,
    maskWeight: Boolean = false
): Bitmap {
    val effectiveWeight = CalorieUtils.getEffectiveWeight(selectedDate, allRecords, userProfile)
    val target = if (userProfile != null) {
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
    val intake = dailyRecord?.totalIntake ?: 0
    val burned = dailyRecord?.totalBurned ?: 0
    val balance = intake - (target + burned)

    val breakfastItems = items.filter { it.type == "food" && CalorieUtils.getMealCategoryByTime(it.time) == CalorieUtils.MealCategory.BREAKFAST }.sortedBy { it.time }
    val lunchItems = items.filter { it.type == "food" && CalorieUtils.getMealCategoryByTime(it.time) == CalorieUtils.MealCategory.LUNCH }.sortedBy { it.time }
    val dinnerItems = items.filter { it.type == "food" && CalorieUtils.getMealCategoryByTime(it.time) == CalorieUtils.MealCategory.DINNER }.sortedBy { it.time }
    val nightSnackItems = items.filter { it.type == "food" && CalorieUtils.getMealCategoryByTime(it.time) == CalorieUtils.MealCategory.NIGHT_SNACK }.sortedBy { it.time }
    val exerciseItems = items.filter { it.type == "exercise" }.sortedByDescending { it.time }

    data class ShareSection(val title: String, val color: Int, val list: List<CalorieItemEntity>)
    val sections = listOf(
        ShareSection("早餐", android.graphics.Color.parseColor("#4CAF50"), breakfastItems),
        ShareSection("午餐", android.graphics.Color.parseColor("#26A69A"), lunchItems),
        ShareSection("晚餐", android.graphics.Color.parseColor("#FF7043"), dinnerItems),
        ShareSection("宵夜", android.graphics.Color.parseColor("#7E57C2"), nightSnackItems),
        ShareSection("运动", android.graphics.Color.parseColor("#2196F3"), exerciseItems)
    ).filter { it.list.isNotEmpty() }

    val width = 1080f
    val padding = 52f
    val headerH = 244f
    val showMacros = userProfile?.showMacros == true
    val summaryH = if (showMacros) 668f else 516f
    val metricsH = 192f
    val sectionHeaderH = 64f
    val itemH = 126f
    val footerH = 220f
    val sectionGap = 12f

    var contentH = headerH + summaryH + metricsH + footerH + 64f
    sections.forEach { section ->
        contentH += sectionHeaderH + section.list.size * itemH + sectionGap
    }

    val maxBitmapHeight = 12000f
    val scale = (maxBitmapHeight / contentH).coerceAtMost(1f)
    val bitmap = Bitmap.createBitmap((width * scale).toInt(), (contentH * scale).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    if (scale < 1f) canvas.scale(scale, scale)

    val paint = android.graphics.Paint().apply { isAntiAlias = true }
    val textPaint = android.text.TextPaint().apply { isAntiAlias = true }
    val selectedTheme = getTodayVisualTheme(selectedThemeIndex)
    canvas.drawColor(selectedTheme.lightBgColor)

    val bgPaint = android.graphics.Paint().apply {
        shader = android.graphics.LinearGradient(
            0f, 0f, 0f, 420f,
            selectedTheme.lightTopGradientColor,
            selectedTheme.lightBgColor,
            android.graphics.Shader.TileMode.CLAMP
        )
        isAntiAlias = true
    }
    canvas.drawRect(0f, 0f, width, 520f, bgPaint)
    paint.textSize = 100f
    paint.alpha = 24
    paint.textAlign = android.graphics.Paint.Align.CENTER
    val patternCols = 5
    val patternRows = (contentH / 300f).toInt() + 1
    val patternGapX = width / patternCols
    val patternGapY = 300f
    val bgRandom = java.util.Random()
    for (r in 0 until patternRows) {
        for (c in 0 until patternCols) {
            val emoji = selectedTheme.patternEmoji[bgRandom.nextInt(selectedTheme.patternEmoji.size)]
            val x = c * patternGapX + patternGapX / 2 + (bgRandom.nextFloat() - 0.5f) * 50f
            val yPattern = r * patternGapY + patternGapY / 2 + (bgRandom.nextFloat() - 0.5f) * 50f
            val rotation = (bgRandom.nextFloat() - 0.5f) * 60f
            canvas.save()
            canvas.rotate(rotation, x, yPattern)
            canvas.drawText(emoji, x, yPattern, paint)
            canvas.restore()
        }
    }
    paint.alpha = 255

    var y = 120f
    textPaint.color = android.graphics.Color.parseColor("#1F2A24")
    textPaint.textSize = 58f
    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    textPaint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText("今日记录", width / 2f, y, textPaint)

    y += 66f
    textPaint.textSize = 34f
    textPaint.typeface = android.graphics.Typeface.DEFAULT
    textPaint.color = android.graphics.Color.parseColor("#4F5B56")
    canvas.drawText(selectedDate, width / 2f, y, textPaint)
    textPaint.textAlign = android.graphics.Paint.Align.LEFT

    y += 54f

    val summaryTop = headerH - 22f
    paint.color = android.graphics.Color.argb(200, 255, 255, 255)
    paint.setShadowLayer(8f, 0f, 3f, android.graphics.Color.parseColor("#1A000000"))
    val summaryRect = android.graphics.RectF(padding, summaryTop, width - padding, summaryTop + summaryH)
    canvas.drawRoundRect(summaryRect, 44f, 44f, paint)
    paint.clearShadowLayer()

    val statusText = if (balance > 0) "今日热量盈余" else "今日热量缺口"
    val statusColor = if (balance > 0) android.graphics.Color.parseColor("#D32F2F") else android.graphics.Color.parseColor("#4CAF50")
    val balanceAbs = kotlin.math.abs(balance)
    val limit = target + burned
    val progressRaw = if (limit > 0) intake.toFloat() / limit.toFloat() else 0f
    val progress = progressRaw.coerceIn(0f, 1f)

    val leftX = padding + 44f
    textPaint.color = android.graphics.Color.parseColor("#67736E")
    textPaint.textSize = 24f
    textPaint.typeface = android.graphics.Typeface.DEFAULT
    canvas.drawText("今日状态", leftX, summaryTop + 64f, textPaint)
    textPaint.color = statusColor
    textPaint.textSize = 54f
    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    canvas.drawText(statusText, leftX, summaryTop + 138f, textPaint)

    val rightX = width - padding - 50f
    val balanceText = "${if (balance > 0) "+" else "-"}$balanceAbs"
    textPaint.textAlign = android.graphics.Paint.Align.RIGHT
    textPaint.textSize = 68f
    canvas.drawText(balanceText, rightX, summaryTop + 126f, textPaint)
    textPaint.textSize = 40f
    textPaint.typeface = android.graphics.Typeface.DEFAULT
    textPaint.color = android.graphics.Color.parseColor("#4F5B56")
    canvas.drawText("kcal", rightX, summaryTop + 180f, textPaint)
    textPaint.textAlign = android.graphics.Paint.Align.LEFT

    textPaint.color = android.graphics.Color.parseColor("#4F5B56")
    textPaint.textSize = 38f
    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    canvas.drawText("已摄入 $intake", leftX, summaryTop + 248f, textPaint)
    textPaint.textAlign = android.graphics.Paint.Align.RIGHT
    canvas.drawText("限额 $limit", rightX, summaryTop + 248f, textPaint)
    textPaint.textAlign = android.graphics.Paint.Align.LEFT

    val barLeft = leftX
    val barRight = rightX
    val barTop = summaryTop + 282f
    paint.color = android.graphics.Color.parseColor("#F1F4F1")
    val trackRect = android.graphics.RectF(barLeft, barTop, barRight, barTop + 34f)
    canvas.drawRoundRect(trackRect, 17f, 17f, paint)
    val fillRight = barLeft + (barRight - barLeft) * progress
    if (fillRight > barLeft + 2f) {
        paint.color = if (progressRaw > 1f) android.graphics.Color.parseColor("#D32F2F") else android.graphics.Color.parseColor("#4CAF50")
        canvas.drawRoundRect(android.graphics.RectF(barLeft, barTop, fillRight, barTop + 34f), 17f, 17f, paint)
    }

    var blockBottom = barTop + 34f
    if (showMacros) {
        val macroTargets = CalorieUtils.calculateMacroTargets(
            weight = effectiveWeight,
            goal = userProfile?.goal ?: "maintain",
            dailyCalorieTarget = target
        )
        val macroData = listOf(
            Triple("碳水化合物", Pair(dailyRecord?.totalCarbs ?: 0, macroTargets.first), android.graphics.Color.parseColor("#69F0AE")),
            Triple("蛋白质", Pair(dailyRecord?.totalProtein ?: 0, macroTargets.second), android.graphics.Color.parseColor("#40C4FF")),
            Triple("脂肪", Pair(dailyRecord?.totalFat ?: 0, macroTargets.third), android.graphics.Color.parseColor("#FF8A80"))
        )
        val macroTop = blockBottom + 32f
        val gap = 20f
        val macroWidth = (barRight - barLeft - gap * 2f) / 3f
        macroData.forEachIndexed { idx, macro ->
            val x = barLeft + idx * (macroWidth + gap)
            textPaint.color = android.graphics.Color.parseColor("#6A7570")
            textPaint.textSize = 30f
            textPaint.typeface = android.graphics.Typeface.DEFAULT
            canvas.drawText(macro.first, x, macroTop + 40f, textPaint)

            paint.color = android.graphics.Color.parseColor("#EEF3EE")
            canvas.drawRoundRect(android.graphics.RectF(x, macroTop + 54f, x + macroWidth, macroTop + 74f), 10f, 10f, paint)
            val current = macro.second.first
            val targetMacro = macro.second.second.coerceAtLeast(1)
            val p = (current.toFloat() / targetMacro.toFloat()).coerceIn(0f, 1f)
            paint.color = macro.third
            canvas.drawRoundRect(android.graphics.RectF(x, macroTop + 54f, x + macroWidth * p, macroTop + 74f), 10f, 10f, paint)

            textPaint.color = android.graphics.Color.parseColor("#4E5B56")
            textPaint.textSize = 34f
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("$current / ${macro.second.second}克", x, macroTop + 132f, textPaint)
        }
        blockBottom = macroTop + 146f
    }

    val statsTop = blockBottom + 18f
    val statWidth = (barRight - barLeft) / 3f
    val statData = listOf(
        Triple("基础消耗", target.toString(), android.graphics.Color.parseColor("#F9A825")),
        Triple("运动消耗", burned.toString(), android.graphics.Color.parseColor("#2196F3")),
        Triple("总摄入", intake.toString(), android.graphics.Color.parseColor("#43A047"))
    )
    statData.forEachIndexed { idx, stat ->
        val x = barLeft + idx * statWidth
        textPaint.color = android.graphics.Color.parseColor("#5F6C67")
        textPaint.textSize = 34f
        textPaint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText(stat.first, x, statsTop + 34f, textPaint)
        textPaint.color = stat.third
        textPaint.textSize = 48f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText(stat.second, x, statsTop + 102f, textPaint)
    }

    val metricsTop = summaryTop + summaryH + 24f
    val metricGap = 20f
    val metricWidth = (width - padding * 2 - metricGap * 2) / 3f
    val weightText = if (maskWeight) "****" else dailyRecord?.weight?.let { String.format(Locale.getDefault(), "%.1f kg", it) } ?: "记录"
    val waterText = "${dailyRecord?.totalWater ?: 0} ml"
    val sleepHour = (dailyRecord?.sleepDuration ?: 0) / 60
    val sleepMinute = (dailyRecord?.sleepDuration ?: 0) % 60
    val metricList = listOf(
        listOf("今日体重", weightText, "", android.graphics.Color.parseColor("#1E1E1E"), android.graphics.Color.parseColor("#FFF3CD"), android.graphics.Color.parseColor("#C28B00")),
        listOf("今日饮水", waterText, "", android.graphics.Color.parseColor("#2196F3"), android.graphics.Color.parseColor("#E3F2FD"), android.graphics.Color.parseColor("#2196F3")),
        listOf("今日睡眠", "${sleepHour}h ${sleepMinute}m", "", android.graphics.Color.parseColor("#673AB7"), android.graphics.Color.parseColor("#EDE7F6"), android.graphics.Color.parseColor("#673AB7"))
    )
    metricList.forEachIndexed { idx, pair ->
        val left = padding + idx * (metricWidth + metricGap)
        val rect = android.graphics.RectF(left, metricsTop, left + metricWidth, metricsTop + metricsH)
        paint.color = android.graphics.Color.argb(200, 255, 255, 255)
        paint.setShadowLayer(6f, 0f, 2f, android.graphics.Color.parseColor("#18000000"))
        canvas.drawRoundRect(rect, 30f, 30f, paint)
        paint.clearShadowLayer()

        paint.color = pair[4] as Int
        canvas.drawCircle(left + metricWidth - 40f, metricsTop + 42f, 24f, paint)
        textPaint.color = pair[5] as Int
        textPaint.textSize = 20f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("✎", left + metricWidth - 50f, metricsTop + 50f, textPaint)

        textPaint.color = android.graphics.Color.parseColor("#5E6A65")
        textPaint.textSize = 34f
        textPaint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText(pair[0] as String, left + 22f, metricsTop + 58f, textPaint)

        textPaint.color = pair[3] as Int
        textPaint.textSize = 50f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        val value = pair[1] as String
        canvas.drawText(value, left + 22f, metricsTop + 146f, textPaint)
        val unit = pair[2] as String
        if (unit.isNotBlank() && value != "记录") {
            textPaint.textSize = 34f
            textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            val vWidth = textPaint.measureText(value)
            canvas.drawText(unit, left + 26f + vWidth, metricsTop + 146f, textPaint)
        }
    }

    var currentY = metricsTop + metricsH + 34f
    val caloriesRightX = width - padding - 12f
    fun drawSectionTitle(title: String, calories: Int, color: Int) {
        textPaint.color = color
        textPaint.textSize = 40f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText(title, padding, currentY + 46f, textPaint)
        textPaint.color = android.graphics.Color.parseColor("#6A7570")
        textPaint.textSize = 36f
        textPaint.typeface = android.graphics.Typeface.DEFAULT
        textPaint.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText("${calories} kcal", caloriesRightX, currentY + 46f, textPaint)
        textPaint.textAlign = android.graphics.Paint.Align.LEFT
        currentY += sectionHeaderH
    }

    fun decodeThumbnail(path: String): Bitmap? {
        return try {
            if (path.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(path))?.use { BitmapFactory.decodeStream(it) }
            } else {
                BitmapFactory.decodeFile(path)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun resolveImagePath(item: CalorieItemEntity): String? {
        val directPath = item.imageUrl?.trim().orEmpty()
        if (directPath.isNotBlank()) return directPath
        val notes = item.notes.orEmpty()
        val marker = "|img:"
        val markerIndex = notes.indexOf(marker)
        if (markerIndex < 0) return null
        val start = markerIndex + marker.length
        if (start >= notes.length) return null
        val end = notes.indexOf('|', start).takeIf { it >= 0 } ?: notes.length
        return notes.substring(start, end).trim().takeIf { it.isNotBlank() }
    }

    fun ellipsizeSingleLine(text: String, maxWidth: Float, paint: android.graphics.Paint): String {
        if (text.isEmpty()) return text
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        if (paint.measureText(ellipsis) >= maxWidth) return ellipsis
        var low = 0
        var high = text.length
        var best = 0
        while (low <= high) {
            val mid = (low + high) / 2
            val candidate = text.substring(0, mid) + ellipsis
            if (paint.measureText(candidate) <= maxWidth) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return text.substring(0, best) + ellipsis
    }

    fun resolveDisplayNotes(item: CalorieItemEntity): String {
        val notes = item.notes.orEmpty()
        if (notes.isBlank()) return ""
        val marker = "|img:"
        val markerIndex = notes.indexOf(marker)
        val cleaned = if (markerIndex >= 0) {
            val pathStart = markerIndex + marker.length
            val pathEnd = notes.indexOf('|', pathStart).takeIf { it >= 0 } ?: notes.length
            val removeEnd = if (pathEnd < notes.length) pathEnd + 1 else pathEnd
            notes.removeRange(markerIndex, removeEnd)
        } else {
            notes
        }
        return cleaned.replace("\r", " ").replace("\n", " ").replace(Regex("\\s+"), " ").trim()
    }

    fun drawItemRow(item: CalorieItemEntity) {
        val rowGap = 14f
        val iconR = (itemH - rowGap) / 2f - 2f
        val iconCx = padding + iconR
        val iconCy = currentY + (itemH - rowGap) / 2f
        val rectLeft = iconCx + iconR + rowGap
        val rect = android.graphics.RectF(rectLeft, currentY, width - padding, currentY + itemH - rowGap)
        paint.color = android.graphics.Color.argb(150, 255, 255, 255)
        paint.setShadowLayer(5f, 0f, 2f, android.graphics.Color.parseColor("#16000000"))
        canvas.drawRoundRect(rect, 24f, 24f, paint)
        paint.clearShadowLayer()

        val isFood = item.type == "food"
        val iconBg = if (isFood) android.graphics.Color.parseColor("#DDF7D8") else android.graphics.Color.parseColor("#D9ECFF")
        paint.color = iconBg
        paint.setShadowLayer(5f, 0f, 2f, android.graphics.Color.parseColor("#16000000"))
        canvas.drawCircle(iconCx, iconCy, iconR, paint)
        paint.clearShadowLayer()
        val imagePath = resolveImagePath(item)
        var thumbnailDrawn = false
        if (!imagePath.isNullOrBlank()) {
            val srcBitmap = decodeThumbnail(imagePath)
            if (srcBitmap != null) {
                val srcSide = kotlin.math.min(srcBitmap.width, srcBitmap.height)
                val srcLeft = (srcBitmap.width - srcSide) / 2
                val srcTop = (srcBitmap.height - srcSide) / 2
                val srcRect = android.graphics.Rect(srcLeft, srcTop, srcLeft + srcSide, srcTop + srcSide)
                val dstRect = android.graphics.RectF(iconCx - iconR, iconCy - iconR, iconCx + iconR, iconCy + iconR)
                val clipPath = android.graphics.Path().apply { addCircle(iconCx, iconCy, iconR, android.graphics.Path.Direction.CW) }
                paint.color = iconBg
                canvas.drawCircle(iconCx, iconCy, iconR, paint)
                canvas.save()
                canvas.clipPath(clipPath)
                canvas.drawBitmap(srcBitmap, srcRect, dstRect, null)
                canvas.restore()
                thumbnailDrawn = true
            }
        }
        if (!thumbnailDrawn) {
            paint.alpha = 255
            paint.color = iconBg
            canvas.drawCircle(iconCx, iconCy, iconR, paint)
            textPaint.textSize = iconR * 1.15f
            textPaint.color = if (isFood) android.graphics.Color.parseColor("#2E7D32") else android.graphics.Color.parseColor("#1565C0")
            textPaint.textAlign = android.graphics.Paint.Align.CENTER
            val fm = textPaint.fontMetrics
            val emojiY = iconCy - (fm.ascent + fm.descent) / 2f
            canvas.drawText(resolveDefaultEmoji(context, item.name, item.type), iconCx, emojiY, textPaint)
            textPaint.textAlign = android.graphics.Paint.Align.LEFT
        }

        textPaint.textSize = if (showNotes) 36f else 40f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textPaint.color = android.graphics.Color.parseColor("#222C27")
        val nameLeft = rectLeft + 28f
        val nameRightLimit = if (showNotes) width - padding - 154f else width - padding - 190f
        val maxNameWidth = (nameRightLimit - nameLeft).coerceAtLeast(80f)
        val name = ellipsizeSingleLine(item.name, maxNameWidth, textPaint)
        val titleY = if (showNotes) {
            currentY + 56f
        } else {
            val fm = textPaint.fontMetrics
            val rectCenterY = currentY + (itemH - 14f) / 2f
            rectCenterY - (fm.ascent + fm.descent) / 2f
        }
        canvas.drawText(name, nameLeft, titleY, textPaint)

        if (showNotes) {
            textPaint.typeface = android.graphics.Typeface.DEFAULT
            textPaint.textSize = 29f
            textPaint.color = android.graphics.Color.parseColor("#7B8681")
            val metaLeft = rectLeft + 28f
            val noteRightLimit = padding + (width - padding * 2f) * (5f / 6f)
            val maxMetaWidth = (noteRightLimit - metaLeft).coerceAtLeast(80f)
            val displayNotes = resolveDisplayNotes(item)
            val metaRaw = if (displayNotes.isNotBlank()) "${item.time} · $displayNotes" else item.time
            val metaText = ellipsizeSingleLine(metaRaw, maxMetaWidth, textPaint)
            canvas.drawText(metaText, metaLeft, currentY + 92f, textPaint)

        }
        textPaint.textSize = if (showNotes) 37f else 39f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textPaint.color = if (isFood) android.graphics.Color.parseColor("#2E7D32") else android.graphics.Color.parseColor("#1565C0")
        val calText = "${if (isFood) "+" else "-"}${item.calories}"
        textPaint.textAlign = android.graphics.Paint.Align.RIGHT
        val calY = if (showNotes) {
            currentY + 76f
        } else {
            val fm = textPaint.fontMetrics
            val rectCenterY = currentY + (itemH - 14f) / 2f
            rectCenterY - (fm.ascent + fm.descent) / 2f
        }
        canvas.drawText(calText, caloriesRightX, calY, textPaint)
        textPaint.textAlign = android.graphics.Paint.Align.LEFT
        textPaint.typeface = android.graphics.Typeface.DEFAULT

        currentY += itemH
    }

    if (sections.isEmpty()) {
        textPaint.color = android.graphics.Color.parseColor("#7C8883")
        textPaint.textSize = 28f
        canvas.drawText("今天还没有记录，去添加第一条吧～", padding, currentY + 44f, textPaint)
        currentY += 72f
    } else {
        sections.forEach { section ->
            drawSectionTitle(section.title, section.list.sumOf { it.calories }, section.color)
            section.list.forEach { drawItemRow(it) }
            currentY += sectionGap
        }
    }

    val footerTop = contentH - footerH
    paint.color = android.graphics.Color.parseColor("#CCD6D1")
    paint.strokeWidth = 2f
    canvas.drawLine(padding, footerTop + 28f, width - padding, footerTop + 16f, paint)

    val iconSize = 86f
    val iconX = width / 2f - 250f
    val iconY = footerTop + 66f
    val iconId = context.resources.getIdentifier("app_icon", "drawable", context.packageName)
    if (iconId != 0) {
        val iconBitmap = BitmapFactory.decodeResource(context.resources, iconId)
        if (iconBitmap != null) {
            val scaled = Bitmap.createScaledBitmap(iconBitmap, iconSize.toInt(), iconSize.toInt(), true)
            canvas.drawBitmap(scaled, iconX, iconY, null)
        }
    } else {
        paint.color = selectedTheme.lightTopGradientColor
        val iconRect = android.graphics.RectF(iconX, iconY, iconX + iconSize, iconY + iconSize)
        canvas.drawRoundRect(iconRect, 18f, 18f, paint)
        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 40f
        textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("猫", iconX + 24f, iconY + 56f, textPaint)
    }

    textPaint.color = android.graphics.Color.parseColor("#2F3934")
    textPaint.textSize = 46f
    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
    canvas.drawText("猫猫要健康！", iconX + iconSize + 24f, iconY + 40f, textPaint)
    textPaint.color = android.graphics.Color.parseColor("#6E7A75")
    textPaint.textSize = 28f
    textPaint.typeface = android.graphics.Typeface.DEFAULT
    canvas.drawText("记录每一份努力", iconX + iconSize + 24f, iconY + 82f, textPaint)

    return bitmap
}

fun saveTodayBitmap(context: Context, bitmap: Bitmap): Boolean {
    val filename = "MeowFit_Today_${System.currentTimeMillis()}.png"
    var outputStream: java.io.OutputStream? = null
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MeowFit")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                outputStream = context.contentResolver.openOutputStream(uri)
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "MeowFit")
            if (!appDir.exists()) appDir.mkdirs()
            val imageFile = File(appDir, filename)
            outputStream = FileOutputStream(imageFile)
        }

        if (outputStream != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
            return true
        } else {
            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
            return false
        }
    } catch (e: Exception) {
        Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        return false
    }
}

fun shareTodayBitmap(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "share_today.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享今日记录"))
    } catch (e: Exception) {
        Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
