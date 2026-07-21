package com.example.calorietracker.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape as RoundedCornerShape
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import com.example.calorietracker.data.CalorieItemEntity
import com.example.calorietracker.data.DailyRecordEntity
import com.example.calorietracker.data.UserProfileEntity
import com.example.calorietracker.ui.components.PixelCatStatusCard
import com.example.calorietracker.ui.components.compactInput
import com.example.calorietracker.ui.components.resolvePixelCatStatus
import com.example.calorietracker.ui.theme.InfoBlue
import com.example.calorietracker.ui.theme.MacroCarb
import com.example.calorietracker.ui.theme.MacroFat
import com.example.calorietracker.ui.theme.MacroProtein
import com.example.calorietracker.ui.theme.PixelInk
import com.example.calorietracker.ui.theme.deficitColor
import com.example.calorietracker.ui.theme.mealCategoryColors
import com.example.calorietracker.util.BitmapUtils
import com.example.calorietracker.util.CalorieUtils
import com.example.calorietracker.util.ImageStorageUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    TodayVisualTheme(5, "暖阳蔬果", "#FFF3E0".toColorInt(), "#FFCC80".toColorInt(), "#20170F".toColorInt(), "#3C2A16".toColorInt(), listOf("🥕", "🍊", "🌞", "🌻")),
    TodayVisualTheme(6, "纯净素白", "#FAFAFA".toColorInt(), "#EEEEEE".toColorInt(), "#121212".toColorInt(), "#1E1E1E".toColorInt(), listOf("☁️", "❄️", "🕊️", "🤍"))
)

fun getTodayVisualTheme(index: Int): TodayVisualTheme {
    val safeIndex = index.coerceIn(0, todayVisualThemePool.lastIndex)
    return todayVisualThemePool[safeIndex]
}

fun calculatePerceivedLuminance(color: Color): Float {
    return 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
}

private fun createTempCameraUri(context: Context): Uri? {
    return runCatching {
        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File.createTempFile("meowfit_edit_", ".jpg", cameraDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

private fun allowDecimalInput(text: String): Boolean {
    if (text.isEmpty()) return true
    return text.matches(Regex("\\d*(\\.\\d{0,2})?"))
}

private fun toDateString(year: Int, month: Int, day: Int): String {
    return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)
}

private fun parseDateSafe(date: String): Triple<Int, Int, Int>? {
    return try {
        val parts = date.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = (parts[1].toIntOrNull() ?: return null) - 1
        val d = parts[2].toIntOrNull() ?: return null
        Triple(y, m, d)
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun ThemedCalendarPickerDialog(
    initialDate: String,
    recordedDates: Set<String>,
    weekStartDay: Int,
    containerColor: Color,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parsed = parseDateSafe(initialDate) ?: run {
        val now = Calendar.getInstance()
        Triple(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
    }
    var displayedYear by remember(initialDate) { mutableIntStateOf(parsed.first) }
    var displayedMonth by remember(initialDate) { mutableIntStateOf(parsed.second) }
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }

    val today = remember {
        val c = Calendar.getInstance()
        toDateString(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    }
    val weeks = if (weekStartDay == Calendar.MONDAY) {
        listOf("一", "二", "三", "四", "五", "六", "日")
    } else {
        listOf("日", "一", "二", "三", "四", "五", "六")
    }

    val monthCalendar = remember(displayedYear, displayedMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val firstDayOfWeek = monthCalendar.get(Calendar.DAY_OF_WEEK)
    val startOffset = (firstDayOfWeek - weekStartDay + 7) % 7
    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("选择日期", style = MaterialTheme.typography.titleMedium, color = textColor)
                Text(
                    text = runCatching {
                        CalorieUtils.formatDate(selectedDate).replace("月", "月").replace("日", "日")
                    }.getOrDefault(selectedDate),
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Divider(color = textColor.copy(alpha = 0.16f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${displayedYear}年${displayedMonth + 1}月",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (displayedMonth == 0) {
                            displayedMonth = 11
                            displayedYear -= 1
                        } else {
                            displayedMonth -= 1
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, null, tint = textColor)
                    }
                    IconButton(onClick = {
                        if (displayedMonth == 11) {
                            displayedMonth = 0
                            displayedYear += 1
                        } else {
                            displayedMonth += 1
                        }
                    }) {
                        Icon(Icons.Default.ArrowForward, null, tint = textColor)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    weeks.forEach { label ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = textColor.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(rows) { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            repeat(7) { col ->
                                val index = row * 7 + col
                                val day = index - startOffset + 1
                                if (day !in 1..daysInMonth) {
                                    Box(modifier = Modifier.weight(1f).height(42.dp))
                                } else {
                                    val dateStr = toDateString(displayedYear, displayedMonth, day)
                                    val isSelected = selectedDate == dateStr
                                    val isToday = today == dateStr
                                    val hasData = dateStr in recordedDates
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .padding(horizontal = 2.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) accentColor else Color.Transparent)
                                            .clickable { selectedDate = dateStr },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = day.toString(),
                                                color = when {
                                                    isSelected -> PixelInk
                                                    else -> textColor
                                                },
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (hasData) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) PixelInk else accentColor)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.height(7.dp))
                                            }
                                        }
                                        if (isToday && !isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .padding(1.dp)
                                                    .border(1.dp, accentColor.copy(alpha = 0.8f), CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = textColor.copy(alpha = 0.82f))
                    }
                    TextButton(onClick = { onConfirm(selectedDate) }) {
                        Text("确定", color = accentColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageSourceDialog(
    containerColor: Color,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onPickAlbum: () -> Unit,
    onTakePhoto: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        titleContentColor = textColor,
        textContentColor = textColor,
        title = { Text("选择图片来源") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPickAlbum,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = containerColor.copy(alpha = 0.96f),
                        contentColor = accentColor
                    ),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.56f))
                ) {
                    Text("从相册选择", color = accentColor)
                }
                OutlinedButton(
                    onClick = onTakePhoto,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = containerColor.copy(alpha = 0.96f),
                        contentColor = accentColor
                    ),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.56f))
                ) {
                    Text("拍照", color = accentColor)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
            ) {
                Text("取消", color = accentColor)
            }
        }
    )
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
    allItems: List<CalorieItemEntity>,
    selectedDate: String,
    onDateChange: (String) -> Unit,
    onAddClick: (String) -> Unit,
    onDeleteItem: (CalorieItemEntity) -> Unit,
    onUpdateItem: (CalorieItemEntity) -> Unit,
    onUpdateWeight: (Float) -> Unit,
    onSaveExercise: (String, Double, String, String) -> Unit, // name, calories, startTime, endTime
    onUpdateWater: (Int) -> Unit,
    onUpdateSleep: (Int) -> Unit, // minutes
    onUpdateMedicationTaken: (String) -> Unit = {}
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
    var showMedicationDialog by remember { mutableStateOf(false) }
    var isTimerRunning by rememberSaveable { mutableStateOf(false) }
    var timerStartTime by rememberSaveable { mutableStateOf<Long?>(null) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var exerciseName by rememberSaveable { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<CalorieItemEntity?>(null) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }
    var previewSavedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewShareBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareShowNotes by remember { mutableStateOf(true) }
    var shareMaskWeight by remember { mutableStateOf(false) }
    var shareShowMeds by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }
    var pendingSearchItemId by remember { mutableStateOf<String?>(null) }
    var isShareWorking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val searchResults = remember(searchKeyword, allItems) {
        val keyword = searchKeyword.trim()
        if (keyword.isBlank()) {
            emptyList()
        } else {
            allItems.filter {
                it.name.contains(keyword, ignoreCase = true) ||
                    it.notes.orEmpty().contains(keyword, ignoreCase = true) ||
                    it.time.contains(keyword, ignoreCase = true) ||
                    it.date.contains(keyword, ignoreCase = true) ||
                    it.mealCategory.orEmpty().contains(keyword, ignoreCase = true)
            }.sortedWith(compareByDescending<CalorieItemEntity> { it.date }.thenByDescending { it.time }).take(120)
        }
    }

    // Calculate effective weight for today (or selected date)
    // If dailyRecord.weight is set, use it. Otherwise find previous.
    val effectiveWeight = remember(dailyRecord, allRecords, selectedDate, userProfile) {
        CalorieUtils.getEffectiveWeight(selectedDate, allRecords, userProfile)
    }
    val foodSections = mealCategoryColors().map { (category, title, color) ->
        Triple(
            title,
            color,
            items.filter {
                it.type == "food" && CalorieUtils.resolveMealCategory(it.mealCategory, it.time) == category
            }.sortedBy { it.time }
        )
    }.filter { it.third.isNotEmpty() }
    val exerciseItems = remember(items) { items.filter { it.type == "exercise" }.sortedByDescending { it.time } }

    LaunchedEffect(items, pendingSearchItemId) {
        val targetId = pendingSearchItemId ?: return@LaunchedEffect
        val target = items.firstOrNull { it.id == targetId }
        if (target == null) {
            // Target no longer exists; clear to avoid re-running on every items change
            if (items.isNotEmpty()) pendingSearchItemId = null
            return@LaunchedEffect
        }
        editingItem = target
        pendingSearchItemId = null
    }

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
        Dialog(onDismissRequest = {
            previewSavedBitmap?.recycle()
            previewSavedBitmap = null
        }) {
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
        Dialog(onDismissRequest = {
            previewShareBitmap?.recycle()
            previewShareBitmap = null
        }) {
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
                        TextButton(onClick = {
                            previewShareBitmap?.recycle()
                            previewShareBitmap = null
                        }) {
                            Text("取消")
                        }
                        TextButton(onClick = {
                            val bitmapToShare = previewShareBitmap
                            if (bitmapToShare != null) {
                                scope.launch {
                                    try {
                                        shareTodayBitmap(context, bitmapToShare)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    // File already written & share intent fired; safe to release the big bitmap
                                    bitmapToShare.recycle()
                                    previewShareBitmap = null
                                }
                            } else {
                                previewShareBitmap = null
                            }
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

                    if (userProfile?.medicationEnabled == true && userProfile.medications.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("包含服药信息")
                            Switch(
                                checked = shareShowMeds,
                                onCheckedChange = { shareShowMeds = it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onClick@{
                        if (isShareWorking) return@onClick
                        isShareWorking = true
                        scope.launch {
                            try {
                                val bitmap = withContext(Dispatchers.IO) {
                                    generateTodayLongScreenshot(
                                        context = context,
                                        userProfile = userProfile,
                                        dailyRecord = dailyRecord,
                                        allRecords = allRecords,
                                        items = items,
                                        selectedDate = selectedDate,
                                        selectedThemeIndex = selectedThemeIndex,
                                        showNotes = shareShowNotes,
                                        maskWeight = shareMaskWeight,
                                        showMeds = shareShowMeds
                                    )
                                }
                                val saved = withContext(Dispatchers.IO) {
                                    saveTodayBitmap(context, bitmap)
                                }
                                if (saved) {
                                    Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                                    previewSavedBitmap = bitmap
                                } else {
                                    bitmap.recycle()
                                    Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            isShareWorking = false
                            showShareDialog = false
                        }
                    },
                    enabled = !isShareWorking
                ) {
                    Text(if (isShareWorking) "生成中..." else "保存图片")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = onClick@{
                            if (isShareWorking) return@onClick
                            isShareWorking = true
                            scope.launch {
                                try {
                                    val bitmap = withContext(Dispatchers.IO) {
                                        generateTodayLongScreenshot(
                                            context = context,
                                            userProfile = userProfile,
                                            dailyRecord = dailyRecord,
                                            allRecords = allRecords,
                                            items = items,
                                            selectedDate = selectedDate,
                                            selectedThemeIndex = selectedThemeIndex,
                                            showNotes = shareShowNotes,
                                            maskWeight = shareMaskWeight,
                                            showMeds = shareShowMeds
                                        )
                                    }
                                    previewShareBitmap = bitmap
                                } catch (e: Exception) {
                                    Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                                isShareWorking = false
                                showShareDialog = false
                            }
                        },
                        enabled = !isShareWorking
                    ) {
                        Text(if (isShareWorking) "生成中..." else "分享")
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

    if (showMedicationDialog && userProfile != null) {
        val meds = if (userProfile.medications.isBlank()) emptyList() else userProfile.medications.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val taken = if (dailyRecord?.medicationTaken.isNullOrBlank()) mutableListOf() else dailyRecord!!.medicationTaken.split(",").map { it.trim() == "1" }.toMutableList()
        MedicationCheckDialog(
            medications = meds,
            taken = taken,
            onDismiss = { showMedicationDialog = false },
            onConfirm = { newTaken ->
                onUpdateMedicationTaken(newTaken.joinToString(","))
                showMedicationDialog = false
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
                        contentColor = PixelInk
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
    val weekStartDay = if (userProfile?.weekStartDay == Calendar.MONDAY) Calendar.MONDAY else Calendar.SUNDAY
    val recordedDates = remember(allRecords, allItems) {
        val fromRecords = allRecords.filter { record ->
            (record.weight ?: 0f) > 0f ||
                record.totalWater > 0 ||
                record.sleepDuration > 0 ||
                record.totalIntake > 0 ||
                record.totalBurned > 0 ||
                record.netCalories != 0 ||
                record.totalCarbs > 0 ||
                record.totalProtein > 0 ||
                record.totalFat > 0
        }.map { it.date }.toSet()
        val fromItems = allItems.map { it.date }.toSet()
        fromRecords + fromItems
    }

    if (showDatePicker) {
        ThemedCalendarPickerDialog(
            initialDate = selectedDate,
            recordedDates = recordedDates,
            weekStartDay = weekStartDay,
            containerColor = dashboardCardColor,
            textColor = dashboardOnCardColor,
            accentColor = dialogAccentColor,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onDateChange(it)
                showDatePicker = false
            }
        )
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
                    containerColor = if (isTimerRunning) MaterialTheme.colorScheme.error else InfoBlue,
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
                    contentColor = PixelInk,
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
                    .blur(if (isDarkTheme) 35.dp else 18.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        val parsedDate = try { sdf.parse(selectedDate) } catch (_: Exception) { null }
                        if (parsedDate != null) {
                            cal.time = parsedDate
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                            onDateChange(sdf.format(cal.time))
                        } else {
                            Toast.makeText(context, "日期格式错误，无法切换", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Previous Day", tint = dateNavColor)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDatePicker = true }
                            .padding(vertical = 4.dp),
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
                        showSearchDialog = true
                    }) {
                        Icon(Icons.Default.Search, "Search Records", tint = dateNavColor)
                    }
                    IconButton(onClick = {
                        onDateChange(CalorieUtils.getTodayString())
                    }) {
                        Icon(Icons.Default.Home, "Back To Today", tint = dateNavColor)
                    }
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val parsedDate = try { sdf.parse(selectedDate) } catch (_: Exception) { null }
                        if (parsedDate != null) {
                            cal.time = parsedDate
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            onDateChange(sdf.format(cal.time))
                        } else {
                            Toast.makeText(context, "日期格式错误，无法切换", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.ArrowForward, "Next Day", tint = dateNavColor)
                    }
                }
            }
            item {
                PixelCatStatusCard(
                    status = resolvePixelCatStatus(
                        intake = dailyRecord?.totalIntake ?: 0,
                        burned = dailyRecord?.totalBurned ?: 0,
                        target = userProfile?.dailyCalorieTarget ?: 0,
                        water = dailyRecord?.totalWater ?: 0,
                        sleepMinutes = dailyRecord?.sleepDuration ?: 0
                    ),
                    containerColor = dashboardCardColor,
                    modifier = Modifier.fillMaxWidth()
                )
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // Medication Card
            if (userProfile?.medicationEnabled == true && userProfile.medications.isNotBlank()) {
                item {
                    val meds = userProfile.medications.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val medTimes = if (userProfile.medicationTimes.isBlank()) emptyList<String>() else userProfile.medicationTimes.split(",").map { it.trim() }
                    val taken = if (dailyRecord?.medicationTaken.isNullOrBlank()) emptyList<String>() else dailyRecord!!.medicationTaken.split(",").map { it.trim() }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        meds.chunked(3).forEachIndexed { chunkIndex, rowMeds ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowMeds.forEachIndexed { rowIndex, med ->
                                    val globalIndex = chunkIndex * 3 + rowIndex
                                    val isTaken = globalIndex < taken.size && taken[globalIndex] == "1"
                                    val medTime = if (globalIndex < medTimes.size) medTimes[globalIndex] else ""
                                    Box(modifier = Modifier.weight(1f)) {
                                        MedicationCard(
                                            name = med,
                                            time = medTime,
                                            isTaken = isTaken,
                                            onToggle = {
                                                val newTaken = MutableList(meds.size) { i ->
                                                    if (i < taken.size) taken[i] else "0"
                                                }
                                                newTaken[globalIndex] = if (isTaken) "0" else "1"
                                                onUpdateMedicationTaken(newTaken.joinToString(","))
                                            },
                                            containerColor = dashboardCardColor,
                                            onContainerColor = dashboardOnCardColor,
                                            isDarkTheme = isDarkTheme
                                        )
                                    }
                                }
                                // Fill remaining space if fewer than 3 in this row
                                repeat(3 - rowMeds.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
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
            foodSections.forEach { section ->
                item { RecordSectionHeader(section.first, section.third.sumOf { it.calories }, section.second) }
                items(section.third, key = { it.id }) { item ->
                    RecordItem(item = item, onDelete = onDeleteItem, onEdit = { editingItem = it }, onImagePreview = { previewImagePath = it }, containerColor = dashboardCardColor, onContainerColor = dashboardOnCardColor, isDarkTheme = isDarkTheme)
                }
            }
            if (exerciseItems.isNotEmpty()) {
                item { RecordSectionHeader("运动", exerciseItems.sumOf { it.calories }, MaterialTheme.colorScheme.secondary) }
                items(exerciseItems, key = { it.id }) { item ->
                    RecordItem(item = item, onDelete = onDeleteItem, onEdit = { editingItem = it }, onImagePreview = { previewImagePath = it }, containerColor = dashboardCardColor, onContainerColor = dashboardOnCardColor, isDarkTheme = isDarkTheme)
                }
            }
            
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showSearchDialog) {
        Dialog(onDismissRequest = { showSearchDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = dashboardCardColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("搜索记录", style = MaterialTheme.typography.titleMedium, color = dashboardOnCardColor)
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        label = { Text("输入关键词", color = dashboardOnCardColor.copy(alpha = 0.75f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = dashboardCardColor.copy(alpha = 0.96f),
                            unfocusedContainerColor = dashboardCardColor.copy(alpha = 0.9f),
                            focusedBorderColor = dialogAccentColor,
                            unfocusedBorderColor = dashboardOnCardColor.copy(alpha = 0.26f),
                            focusedTextColor = dashboardOnCardColor,
                            unfocusedTextColor = dashboardOnCardColor,
                            focusedLabelColor = dialogAccentColor,
                            unfocusedLabelColor = dashboardOnCardColor.copy(alpha = 0.75f),
                            cursorColor = dialogAccentColor
                        )
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults) { item ->
                            Surface(
                                onClick = {
                                    showSearchDialog = false
                                    onDateChange(item.date)
                                    pendingSearchItemId = item.id
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = dashboardCardColor.copy(alpha = 0.86f),
                                border = BorderStroke(1.dp, dashboardOnCardColor.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(item.name, color = dashboardOnCardColor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text("${item.date} ${item.time} · ${if (item.type == "food") "食物" else "运动"} · ${CalorieUtils.formatNumber(item.calories)} kcal", color = dashboardOnCardColor.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (searchKeyword.isNotBlank() && searchResults.isEmpty()) {
                            item {
                                Text(
                                    text = "没有找到相关记录",
                                    color = dashboardOnCardColor.copy(alpha = 0.62f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = { showSearchDialog = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = dialogAccentColor)
                        ) {
                            Text("关闭", color = dialogAccentColor)
                        }
                    }
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
    var calories by remember { mutableStateOf(CalorieUtils.formatNumber(item.calories)) }
    var carbs by remember { mutableStateOf(CalorieUtils.formatNumber(item.carbs)) }
    var protein by remember { mutableStateOf(CalorieUtils.formatNumber(item.protein)) }
    var fat by remember { mutableStateOf(CalorieUtils.formatNumber(item.fat)) }
    var time by remember { mutableStateOf(item.time) }
    var notes by remember { mutableStateOf(item.notes ?: "") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showMediaSourceDialog by remember { mutableStateOf(false) }
    var currentImagePath by remember { mutableStateOf(item.imageUrl) }
    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = pendingCameraUri
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempCameraUri(context)
            if (uri == null) {
                Toast.makeText(context, "无法创建拍照文件", Toast.LENGTH_SHORT).show()
            } else {
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            Toast.makeText(context, "相机权限被拒绝", Toast.LENGTH_SHORT).show()
        }
    }
    // Check if it's an exercise with duration
    val isExercise = item.type == "exercise"
    val mealCategoryOptions = remember {
        CalorieUtils.manualSelectableMealCategories.map { it.label }
    }
    var selectedMealCategory by remember {
        mutableStateOf(
            if (isExercise) null else CalorieUtils.resolveMealCategory(item.mealCategory, item.time).label
        )
    }
    var showMealCategoryDialog by remember { mutableStateOf(false) }
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
                    endTimeStr = item.time
                }
            } else {
                endTimeStr = item.time
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("编辑记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onContainerColor)
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().compactInput()
                )
                
                OutlinedTextField(
                    value = calories,
                    onValueChange = { if (allowDecimalInput(it)) calories = it },
                    label = { Text("卡路里 (kcal)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().compactInput()
                )
                
                if (!isExercise) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { if (allowDecimalInput(it)) carbs = it },
                            label = { Text("碳水") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).compactInput()
                        )
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { if (allowDecimalInput(it)) protein = it },
                            label = { Text("蛋白质") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).compactInput()
                        )
                        OutlinedTextField(
                            value = fat,
                            onValueChange = { if (allowDecimalInput(it)) fat = it },
                            label = { Text("脂肪") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).compactInput()
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
                            modifier = Modifier.weight(1f).compactInput()
                        )
                        OutlinedTextField(
                            value = endTimeStr,
                            onValueChange = { endTimeStr = it },
                            label = { Text("结束时间") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).compactInput()
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("时间") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().compactInput()
                    )
                    Box {
                        OutlinedTextField(
                            value = selectedMealCategory ?: "",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("类别") },
                            placeholder = { Text("请选择类别") },
                            modifier = Modifier.fillMaxWidth().compactInput(),
                            leadingIcon = { Icon(Icons.Default.Restaurant, null) },
                            singleLine = true
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showMealCategoryDialog = true }
                        )
                    }
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
                        onClick = { showMediaSourceDialog = true },
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
                            contentColor = PixelInk
                        )
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cal = CalorieUtils.parseDecimalInput(calories)
                            val c = CalorieUtils.parseDecimalInput(carbs) ?: 0.0
                            val p = CalorieUtils.parseDecimalInput(protein) ?: 0.0
                            val f = CalorieUtils.parseDecimalInput(fat) ?: 0.0

                            if (name.isNotBlank() && cal != null) {
                                isSaving = true
                                coroutineScope.launch {
                                    val finalImagePath = withContext(Dispatchers.IO) {
                                        selectedImageUri?.let {
                                            ImageStorageUtils.compressAndSaveImage(context, it)
                                        }
                                    } ?: currentImagePath

                                    if (isExercise) {
                                        var newNotes = notes
                                        try {
                                            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                            val start = sdf.parse(startTimeStr)
                                            val end = sdf.parse(endTimeStr)
                                            if (start != null && end != null) {
                                                var diff = end.time - start.time
                                                if (diff < 0) diff += 24 * 60 * 60 * 1000
                                                val minutes = diff / (1000 * 60)
                                                val durationRegex = Regex("时长[:：]\\s*\\d+\\s*分钟")
                                                newNotes = if (durationRegex.containsMatchIn(newNotes)) {
                                                    newNotes.replace(
                                                        durationRegex,
                                                        "时长: ${minutes}分钟"
                                                    )
                                                } else if (newNotes.isBlank()) {
                                                    "时长: ${minutes}分钟"
                                                } else {
                                                    "$newNotes, 时长: ${minutes}分钟"
                                                }
                                            }
                                        } catch (_: Exception) {
                                            // Keep the user's notes when the optional time format is invalid.
                                        }
                                        onConfirm(
                                            item.copy(
                                                name = name,
                                                calories = cal,
                                                carbs = 0.0,
                                                protein = 0.0,
                                                fat = 0.0,
                                                time = startTimeStr,
                                                mealCategory = null,
                                                notes = newNotes,
                                                imageUrl = finalImagePath
                                            )
                                        )
                                    } else {
                                        onConfirm(
                                            item.copy(
                                                name = name,
                                                calories = cal,
                                                carbs = c,
                                                protein = p,
                                                fat = f,
                                                time = time,
                                                mealCategory = selectedMealCategory,
                                                notes = notes,
                                                imageUrl = finalImagePath
                                            )
                                        )
                                    }
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = PixelInk
                        )
                    ) {
                        Text(if (isSaving) "处理中..." else "保存")
                    }
                }
            }
        }
    }

    if (showMediaSourceDialog) {
        ImageSourceDialog(
            containerColor = containerColor,
            textColor = onContainerColor,
            accentColor = accentColor,
            onDismiss = { showMediaSourceDialog = false },
            onPickAlbum = {
                showMediaSourceDialog = false
                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onTakePhoto = {
                showMediaSourceDialog = false
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val uri = createTempCameraUri(context)
                    if (uri == null) {
                        Toast.makeText(context, "无法创建拍照文件", Toast.LENGTH_SHORT).show()
                    } else {
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    }
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        )
    }

    if (showMealCategoryDialog && !isExercise) {
        AlertDialog(
            onDismissRequest = { showMealCategoryDialog = false },
            containerColor = containerColor,
            titleContentColor = onContainerColor,
            textContentColor = onContainerColor,
            title = { Text("选择类别", color = onContainerColor) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mealCategoryOptions) { label ->
                        val selected = selectedMealCategory == label
                        Surface(
                            onClick = {
                                selectedMealCategory = label
                                showMealCategoryDialog = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) accentColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selected) accentColor.copy(alpha = 0.7f) else onContainerColor.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) accentColor else onContainerColor.copy(alpha = 0.92f),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = accentColor,
                                        unselectedColor = onContainerColor.copy(alpha = 0.48f)
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showMealCategoryDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                ) {
                    Text("取消", color = accentColor)
                }
            }
        )
    }
}

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
        Column(modifier = Modifier.padding(16.dp)) {
            // Target is treated as BMR/Base TDEE, calculated dynamically based on effective weight
            val age = if (userProfile != null && userProfile.birthDate.isNotBlank()) {
                CalorieUtils.calculateAge(userProfile.birthDate)
            } else {
                userProfile?.age ?: 0
            }
            val target = if (userProfile != null) {
                CalorieUtils.calculateDailyTarget(
                    gender = userProfile.gender,
                    weight = effectiveWeight,
                    height = userProfile.height,
                    age = age,
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
            val statusColor = if (isSurplus) MaterialTheme.colorScheme.error else deficitColor()
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
            
            Spacer(modifier = Modifier.height(14.dp))
            
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
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val macroTargets = CalorieUtils.calculateMacroTargets(
                        gender = userProfile.gender,
                        age = age,
                        weight = effectiveWeight,
                        activityLevel = userProfile.activityLevel,
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
                            color = MacroProtein, // Cyan/Greenish
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )
                        MacroProgressBar(
                            label = "蛋白质",
                            current = currentProtein,
                            target = macroTargets.second,
                            color = MacroCarb, // Light Blue
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        MacroProgressBar(
                            label = "脂肪",
                            current = currentFat,
                            target = macroTargets.third,
                            color = MacroFat, // Pink/Red
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("目标消耗", "$target", MaterialTheme.colorScheme.secondary)
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
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
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
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
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
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
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
fun MedicationCard(
    name: String,
    time: String = "",
    isTaken: Boolean,
    onToggle: () -> Unit,
    containerColor: Color,
    onContainerColor: Color,
    isDarkTheme: Boolean
) {
    // Warning/alerting colors for not-taken, calming colors for taken
    val warnAccent = if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFFE65100)
    val warnBg = if (isDarkTheme) Color(0xFF3E2723) else Color(0xFFFFF3E0)
    val calmAccent = deficitColor()
    val calmBg = if (isDarkTheme) Color(0xFF1B3A1E) else Color(0xFFE8F5E9)
    val bgColor = if (isTaken) calmBg else warnBg
    val borderColor = if (isTaken) calmAccent.copy(alpha = 0.5f) else warnAccent.copy(alpha = 0.55f)
    val nameColor = if (isTaken) calmAccent else warnAccent
    val statusColor = if (isTaken) calmAccent.copy(alpha = 0.8f) else warnAccent.copy(alpha = 0.85f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 3.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = nameColor,
                    maxLines = 1
                )
                if (time.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = nameColor.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isTaken) "已服 ✓" else "待服",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = if (isTaken) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MedicationCheckDialog(
    medications: List<String>,
    taken: List<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    containerColor: Color,
    onContainerColor: Color,
    accentColor: Color
) {
    // Pad to match medication count inside remember (no snapshot writes during composition)
    val checked = remember(medications) {
        mutableStateListOf<Boolean>().apply {
            addAll(taken)
            repeat((medications.size - taken.size).coerceAtLeast(0)) { add(false) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("今日服药", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onContainerColor)
                Spacer(modifier = Modifier.height(16.dp))
                medications.forEachIndexed { index, med ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { checked[index] = !checked[index] }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked[index],
                            onCheckedChange = { checked[index] = it },
                            colors = CheckboxDefaults.colors(checkedColor = accentColor)
                        )
                        Text(
                            text = med,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (checked[index]) accentColor else onContainerColor.copy(alpha = 0.7f),
                            fontWeight = if (checked[index]) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(checked.map { if (it) "1" else "0" })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = PixelInk)
                    ) { Text("保存") }
                }
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
fun RecordSectionHeader(title: String, calories: Double, accentColor: Color) {
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
            text = "${CalorieUtils.formatNumber(calories)} kcal",
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

/**
 * 线程安全缓存 emoji 规则，避免每条记录每次重组都重读 assets 文件。
 * （分享长图在 IO 线程也会调用，故用 @Volatile + synchronized。）
 */
private object EmojiKeywordRulesCache {
    @Volatile
    private var cached: List<Pair<String, List<String>>>? = null

    fun get(context: Context): List<Pair<String, List<String>>> {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: loadEmojiKeywordRulesFromFile(context.applicationContext).also { cached = it }
        }
    }
}

fun resolveDefaultEmoji(context: Context, name: String, type: String): String {
    val locale = Locale.getDefault()
    val primaryText = name.trim().lowercase(locale)
    val rules = EmojiKeywordRulesCache.get(context)

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
                    "${if (isFood) "+" else "-"}${CalorieUtils.formatNumber(item.calories)}",
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
                Icon(Icons.Default.FitnessCenter, null, tint = PixelInk)
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
    onSave: (String, Double, String, String) -> Unit,
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
                    onValueChange = { if (allowDecimalInput(it)) calories = it },
                    label = { Text("消耗卡路里 (kcal)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
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
                            val cal = CalorieUtils.parseDecimalInput(calories)
                            if (name.isNotBlank() && cal != null && cal > 0.0) {
                                onSave(name, cal, startStr, endStr)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = PixelInk
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
    maskWeight: Boolean = false,
    showMeds: Boolean = false
): Bitmap {
    val effectiveWeight = CalorieUtils.getEffectiveWeight(selectedDate, allRecords, userProfile)
    val age = if (userProfile != null && userProfile.birthDate.isNotBlank()) {
        CalorieUtils.calculateAge(userProfile.birthDate)
    } else {
        userProfile?.age ?: 0
    }
    val target = if (userProfile != null) {
        CalorieUtils.calculateDailyTarget(
            gender = userProfile.gender,
            weight = effectiveWeight,
            height = userProfile.height,
            age = age,
            activityLevel = userProfile.activityLevel,
            goal = userProfile.goal
        )
    } else {
        2000
    }
    val intake = dailyRecord?.totalIntake ?: 0
    val burned = dailyRecord?.totalBurned ?: 0
    val balance = intake - (target + burned)

    val foodSections = listOf(
        Triple(CalorieUtils.MealCategory.BREAKFAST, "早餐", android.graphics.Color.parseColor("#4CAF50")),
        Triple(CalorieUtils.MealCategory.MORNING_EXTRA, "早加餐", android.graphics.Color.parseColor("#6D4C41")),
        Triple(CalorieUtils.MealCategory.LUNCH, "午餐", android.graphics.Color.parseColor("#26A69A")),
        Triple(CalorieUtils.MealCategory.AFTERNOON_EXTRA, "午加餐", android.graphics.Color.parseColor("#00897B")),
        Triple(CalorieUtils.MealCategory.AFTERNOON_TEA, "下午茶", android.graphics.Color.parseColor("#FFB300")),
        Triple(CalorieUtils.MealCategory.DINNER, "晚餐", android.graphics.Color.parseColor("#FF7043")),
        Triple(CalorieUtils.MealCategory.EVENING_EXTRA, "晚加餐", android.graphics.Color.parseColor("#5D4037")),
        Triple(CalorieUtils.MealCategory.SNACK, "零食", android.graphics.Color.parseColor("#8E24AA")),
        Triple(CalorieUtils.MealCategory.NIGHT_SNACK, "夜宵", android.graphics.Color.parseColor("#7E57C2"))
    ).map { (category, title, color) ->
        Triple(
            title,
            color,
            items.filter {
                it.type == "food" && CalorieUtils.resolveMealCategory(it.mealCategory, it.time) == category
            }.sortedBy { it.time }
        )
    }.filter { it.third.isNotEmpty() }
    val exerciseItems = items.filter { it.type == "exercise" }.sortedByDescending { it.time }

    data class ShareSection(val title: String, val color: Int, val list: List<CalorieItemEntity>)
    val sections = (foodSections.map { ShareSection(it.first, it.second, it.third) } +
        listOf(ShareSection("运动", android.graphics.Color.parseColor("#2196F3"), exerciseItems)))
        .filter { it.list.isNotEmpty() }

    val width = 1080f
    val padding = 52f
    val headerH = 244f
    val showMacros = userProfile?.showMacros == true
    val summaryH = if (showMacros) 668f else 516f
    val metricsH = 192f
    val showMedsInShare = showMeds && userProfile?.medicationEnabled == true && userProfile?.medications?.isNotBlank() == true
    val hasMedsInShare = showMedsInShare
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
            gender = userProfile?.gender ?: "male",
            age = age,
            weight = effectiveWeight,
            activityLevel = userProfile?.activityLevel ?: "sedentary",
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
        Triple("目标消耗", target.toString(), android.graphics.Color.parseColor("#F9A825")),
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
    val metricColumns = if (hasMedsInShare) 4 else 3
    val metricWidth = (width - padding * 2 - metricGap * (metricColumns - 1)) / metricColumns
    val weightText = if (maskWeight) "****" else dailyRecord?.weight?.let { String.format(Locale.getDefault(), "%.1f kg", it) } ?: "记录"
    val waterText = "${dailyRecord?.totalWater ?: 0} ml"
    val sleepHour = (dailyRecord?.sleepDuration ?: 0) / 60
    val sleepMinute = (dailyRecord?.sleepDuration ?: 0) % 60
    val medMeds = if (userProfile?.medications.isNullOrBlank()) emptyList<String>() else userProfile!!.medications.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val medTaken = if (dailyRecord?.medicationTaken.isNullOrBlank()) emptyList<String>() else dailyRecord!!.medicationTaken.split(",").map { it.trim() }
    val medCount = medMeds.indices.count { it < medTaken.size && medTaken[it] == "1" }
    val medText = if (medMeds.isEmpty()) "未设置" else "${medCount}/${medMeds.size} 种"
    val metricList = buildList {
        add(listOf("今日体重", weightText, "", android.graphics.Color.parseColor("#1E1E1E"), android.graphics.Color.parseColor("#FFF3CD"), android.graphics.Color.parseColor("#C28B00")))
        add(listOf("今日饮水", waterText, "", android.graphics.Color.parseColor("#2196F3"), android.graphics.Color.parseColor("#E3F2FD"), android.graphics.Color.parseColor("#2196F3")))
        if (hasMedsInShare) {
            add(listOf("今日服药", medText, "", android.graphics.Color.parseColor("#E53935"), android.graphics.Color.parseColor("#FFEBEE"), android.graphics.Color.parseColor("#E53935")))
        }
        add(listOf("今日睡眠", "${sleepHour}h ${sleepMinute}m", "", android.graphics.Color.parseColor("#673AB7"), android.graphics.Color.parseColor("#EDE7F6"), android.graphics.Color.parseColor("#673AB7")))
    }
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
    fun drawSectionTitle(title: String, calories: Double, color: Int) {
        textPaint.color = color
        textPaint.textSize = 40f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText(title, padding, currentY + 46f, textPaint)
        textPaint.color = android.graphics.Color.parseColor("#6A7570")
        textPaint.textSize = 36f
        textPaint.typeface = android.graphics.Typeface.DEFAULT
        textPaint.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText("${CalorieUtils.formatNumber(calories)} kcal", caloriesRightX, currentY + 46f, textPaint)
        textPaint.textAlign = android.graphics.Paint.Align.LEFT
        currentY += sectionHeaderH
    }

    fun decodeThumbnail(path: String): Bitmap? {
        // Icons are drawn at ~108px diameter (itemH=126); decode at 2x target size to avoid OOM
        return try {
            if (path.startsWith("content://")) {
                BitmapUtils.decodeSampledFromUri(context.contentResolver, Uri.parse(path), 216)
            } else {
                BitmapUtils.decodeSampledFromPath(path, 216)
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
        val calText = "${if (isFood) "+" else "-"}${CalorieUtils.formatNumber(item.calories)}"
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
    val loadedIcon = com.example.calorietracker.util.IconManager.loadIconBitmap(context, iconSize.toInt())
    if (loadedIcon != null) {
        canvas.save()
        val iconClipPath = android.graphics.Path()
        iconClipPath.addRoundRect(android.graphics.RectF(iconX, iconY, iconX + iconSize, iconY + iconSize), 18f, 18f, android.graphics.Path.Direction.CW)
        canvas.clipPath(iconClipPath)
        canvas.drawBitmap(loadedIcon, iconX, iconY, null)
        canvas.restore()
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

// 注意：本函数包含磁盘 IO，请在后台线程调用；Toast 由调用方在主线程展示。
fun saveTodayBitmap(context: Context, bitmap: Bitmap): Boolean {
    val filename = "MeowFit_Today_${System.currentTimeMillis()}.png"
    var insertedUri: Uri? = null
    fun cleanupFailedInsert() {
        insertedUri?.let { uri ->
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
        insertedUri = null
    }
    return try {
        val outputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MeowFit")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false
            insertedUri = uri
            context.contentResolver.openOutputStream(uri) ?: run {
                cleanupFailedInsert() // Avoid leaving a 0-byte broken entry in the gallery
                return false
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "MeowFit")
            if (!appDir.exists()) appDir.mkdirs()
            val imageFile = File(appDir, filename)
            FileOutputStream(imageFile)
        }

        val success = outputStream.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (!success) cleanupFailedInsert()
        success
    } catch (e: Exception) {
        cleanupFailedInsert()
        false
    }
}

// PNG 压缩写文件在 IO 线程执行，随后回到主线程发起分享 Intent。
suspend fun shareTodayBitmap(context: Context, bitmap: Bitmap) {
    try {
        val uri = withContext(Dispatchers.IO) {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "share_today.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
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


@Composable
fun MedicationReminderDialog(
    medicationNames: List<String>,
    onTakeAll: () -> Unit,
    onDismiss: () -> Unit,
    containerColor: Color,
    textColor: Color,
    accentColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        titleContentColor = textColor,
        textContentColor = textColor,
        title = {
            Text("💊 服药提醒", color = textColor)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "以下药品已过服用时间，记得按时服药哦~",
                    color = textColor.copy(alpha = 0.8f)
                )
                medicationNames.forEach { name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onTakeAll,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = PixelInk
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("已服药")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后再说", color = textColor.copy(alpha = 0.6f))
            }
        }
    )
}
