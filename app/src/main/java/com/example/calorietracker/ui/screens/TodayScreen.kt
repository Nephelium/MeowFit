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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
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
    var showWeightDialog by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerStartTime by remember { mutableStateOf<Long?>(null) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var exerciseName by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<CalorieItemEntity?>(null) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }

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

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("分享今日记录") },
            text = { Text("可保存为图片，或直接分享给好友") },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val bitmap = generateTodayLongScreenshot(context, userProfile, dailyRecord, allRecords, items, selectedDate)
                        saveTodayBitmap(context, bitmap)
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
                            val bitmap = generateTodayLongScreenshot(context, userProfile, dailyRecord, allRecords, items, selectedDate)
                            shareTodayBitmap(context, bitmap)
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
            }
        )
    }

    if (editingItem != null) {
        EditRecordDialog(
            item = editingItem!!,
            onDismiss = { editingItem = null },
            onConfirm = { updatedItem ->
                onUpdateItem(updatedItem)
                editingItem = null
            }
        )
    }

    if (showWeightDialog) {
        WeightDialog(
            currentWeight = dailyRecord?.weight ?: effectiveWeight,
            onDismiss = { showWeightDialog = false },
            onConfirm = { 
                onUpdateWeight(it)
                showWeightDialog = false
            }
        )
    }

    if (showWaterDialog) {
        WaterDialog(
            currentWater = dailyRecord?.totalWater ?: 0,
            onDismiss = { showWaterDialog = false },
            onConfirm = { 
                onUpdateWater(it)
                showWaterDialog = false
            }
        )
    }

    if (showSleepDialog) {
        SleepDialog(
            currentDuration = dailyRecord?.sleepDuration ?: 0,
            onDismiss = { showSleepDialog = false },
            onConfirm = { 
                onUpdateSleep(it)
                showSleepDialog = false
            }
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
            }
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
                Button(onClick = {
                    if (exerciseName.isNotBlank()) {
                        timerStartTime = System.currentTimeMillis()
                    }
                }) {
                    Text("开始计时")
                }
            },
            dismissButton = {
                TextButton(onClick = { isTimerRunning = false }) {
                    Text("取消")
                }
            }
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
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // ... (existing TopBar code) ...
            CenterAlignedTopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDatePicker = true }
                            .padding(8.dp)
                    ) {
                        Text(
                            CalorieUtils.formatDate(selectedDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.ArrowDropDown, 
                            contentDescription = "Select Date",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        try {
                            cal.time = sdf.parse(selectedDate)!!
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                            onDateChange(sdf.format(cal.time))
                        } catch (e: Exception) {}
                    }) {
                        Icon(Icons.Default.ArrowBack, "Previous Day")
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, "Share Today")
                    }
                    IconButton(onClick = {
                         val cal = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        try {
                            cal.time = sdf.parse(selectedDate)!!
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            onDateChange(sdf.format(cal.time))
                        } catch (e: Exception) {}
                    }) {
                        Icon(Icons.Default.ArrowForward, "Next Day")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, "Add Item")
                }
            }
        }
    ) { padding ->
        // ... (rest of content) ...
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timer Status Banner
            if (isTimerRunning && timerStartTime != null) {
                item {
                    TimerStatusCard(exerciseName, timerStartTime!!)
                }
            }

            // Summary Card
            item {
                SummaryCard(userProfile, dailyRecord, effectiveWeight)
            }
// ...

            // Weight Card
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        WeightCard(
                            weight = dailyRecord?.weight,
                            onEdit = { showWeightDialog = true }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WaterCard(
                            water = dailyRecord?.totalWater ?: 0,
                            onEdit = { showWaterDialog = true }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SleepCard(
                            duration = dailyRecord?.sleepDuration ?: 0,
                            onEdit = { showSleepDialog = true }
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
                    RecordItem(item = item, onDelete = onDeleteItem, onEdit = { editingItem = it }, onImagePreview = { previewImagePath = it })
                }
            }
            if (lunchItems.isNotEmpty()) {
                item { RecordSectionHeader("午餐", lunchItems.sumOf { it.calories }, Color(0xFF26A69A)) }
                items(lunchItems) { item ->
                    RecordItem(item = item, onDelete = onDeleteItem, onEdit = { editingItem = it }, onImagePreview = { previewImagePath = it })
                }
            }
            if (dinnerItems.isNotEmpty()) {
                item { RecordSectionHeader("晚餐", dinnerItems.sumOf { it.calories }, Color(0xFFFF7043)) }
                items(dinnerItems) { item ->
                    RecordItem(item = item, onDelete = onDeleteItem, onEdit = { editingItem = it }, onImagePreview = { previewImagePath = it })
                }
            }
            if (nightSnackItems.isNotEmpty()) {
                item { RecordSectionHeader("宵夜", nightSnackItems.sumOf { it.calories }, Color(0xFF7E57C2)) }
                items(nightSnackItems) { item ->
                    RecordItem(item = item, onDelete = onDeleteItem, onEdit = { editingItem = it }, onImagePreview = { previewImagePath = it })
                }
            }
            if (exerciseItems.isNotEmpty()) {
                item { RecordSectionHeader("运动", exerciseItems.sumOf { it.calories }, MaterialTheme.colorScheme.secondary) }
                items(exerciseItems) { item ->
                    RecordItem(item = item, onDelete = onDeleteItem, onEdit = { editingItem = it }, onImagePreview = { previewImagePath = it })
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun EditRecordDialog(
    item: CalorieItemEntity,
    onDismiss: () -> Unit,
    onConfirm: (CalorieItemEntity) -> Unit
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("编辑记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
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
                        }
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (selectedImageUri == null && currentImagePath.isNullOrBlank()) "添加备注图片" else "更换图片")
                    }
                    if (selectedImageUri != null || !currentImagePath.isNullOrBlank()) {
                        TextButton(onClick = {
                            selectedImageUri = null
                            currentImagePath = null
                        }) {
                            Text("移除")
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
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
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
                    }) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

// Helper to reuse parseDuration removed - using CalorieUtils.parseDuration

@Composable
fun SummaryCard(userProfile: UserProfileEntity?, dailyRecord: DailyRecordEntity?, effectiveWeight: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
            val statusColor = if (isSurplus) MaterialTheme.colorScheme.error else Color(0xFF4CAF50) // Red vs Green
            val statusText = if (isSurplus) "今日热量盈余" else "今日热量缺口"
            val balanceAbs = Math.abs(balance)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("今日状态", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("已摄入 $intake", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("限额 $limit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun WeightCard(weight: Float?, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Text("今日体重", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledIconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (weight != null) "$weight" else "记录",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (weight != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "kg",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WaterCard(water: Int, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Text("今日饮水", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledIconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2196F3), modifier = Modifier.size(14.dp))
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$water",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ml",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SleepCard(duration: Int, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Text("今日睡眠", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledIconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEDE7F6))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF673AB7), modifier = Modifier.size(14.dp))
                }
            }
            val hours = duration / 60
            val minutes = duration % 60
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$hours",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF673AB7)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "h",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF673AB7),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$minutes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF673AB7)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "m",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF673AB7),
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
            style = MaterialTheme.typography.titleSmall,
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

@Composable
fun RecordItem(
    item: CalorieItemEntity,
    onDelete: (CalorieItemEntity) -> Unit,
    onEdit: (CalorieItemEntity) -> Unit,
    onImagePreview: (String) -> Unit = {}
) {
    val notesText = remember(item.notes) { item.notes?.trim().orEmpty() }
    val imagePath = remember(item.imageUrl) { item.imageUrl?.takeIf { !it.isNullOrBlank() && File(it).exists() } }
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onEdit(item) }
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val isFood = item.type == "food"
            val iconBg = if (isFood) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            val iconColor = if (isFood) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
            val icon = if (isFood) Icons.Default.Restaurant else Icons.Default.FitnessCenter
            
            if (!imagePath.isNullOrBlank()) {
                AsyncImage(
                    model = File(imagePath),
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
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${item.time} · ${if(isFood) "食物" else "运动"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (notesText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        notesText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            Text(
                "${if (isFood) "+" else "-"}${item.calories}",
                style = MaterialTheme.typography.titleMedium,
                color = if (isFood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { onDelete(item) }) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun TimerStatusCard(name: String, startTime: Long) {
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("正在进行: $name", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(timeStr, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
    onDiscard: () -> Unit
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
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onDiscard()
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("取消") }
            }
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("运动结束", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Duration Display
                Text("运动时长", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    durationStr,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("$startStr - $endStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
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
                    TextButton(onClick = { showDiscardConfirm = true }) {
                        Text("放弃")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val cal = calories.toIntOrNull()
                        if (name.isNotBlank() && cal != null && cal > 0) {
                            onSave(name, cal, startStr, endStr)
                        }
                    }) {
                        Text("保存记录")
                    }
                }
            }
        }
    }
}

@Composable
fun SleepDialog(currentDuration: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var hours by remember { mutableStateOf((currentDuration / 60).toString()) }
    var minutes by remember { mutableStateOf((currentDuration % 60).toString()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("记录睡眠", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
fun WaterDialog(currentWater: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var waterStr by remember { mutableStateOf(if (currentWater > 0) currentWater.toString() else "") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("记录饮水", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
fun WeightDialog(currentWeight: Float, onDismiss: () -> Unit, onConfirm: (Float) -> Unit) {
    var weightStr by remember { mutableStateOf(currentWeight.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("记录今日体重", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
                        Text("保存")
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
    selectedDate: String
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
    val footerH = 260f
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
    data class ShareTheme(
        val bgColor: Int,
        val bgPatternEmoji: List<String>,
        val primaryColor: Int
    )
    val themePool = listOf(
        ShareTheme(android.graphics.Color.parseColor("#FFFDE7"), listOf("🍎", "🥗", "🍇", "🥑"), android.graphics.Color.parseColor("#FFF59D")),
        ShareTheme(android.graphics.Color.parseColor("#FFEBEE"), listOf("🔥", "💪", "🏃", "✨"), android.graphics.Color.parseColor("#FFCDD2")),
        ShareTheme(android.graphics.Color.parseColor("#E1F5FE"), listOf("💧", "🌊", "🧊", "💙"), android.graphics.Color.parseColor("#B3E5FC")),
        ShareTheme(android.graphics.Color.parseColor("#F3E5F5"), listOf("💤", "🌙", "⭐", "🛌"), android.graphics.Color.parseColor("#E1BEE7")),
        ShareTheme(android.graphics.Color.parseColor("#E8F5E9"), listOf("🐱", "🐾", "🌿", "🍀"), android.graphics.Color.parseColor("#C8E6C9")),
        ShareTheme(android.graphics.Color.parseColor("#E0F7FA"), listOf("⚖️", "✨", "📉", "💪"), android.graphics.Color.parseColor("#B2EBF2"))
    )
    val randomTheme = themePool[java.util.Random().nextInt(themePool.size)]
    canvas.drawColor(randomTheme.bgColor)

    val bgPaint = android.graphics.Paint().apply {
        shader = android.graphics.LinearGradient(
            0f, 0f, 0f, 420f,
            randomTheme.primaryColor,
            randomTheme.bgColor,
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
            val emoji = randomTheme.bgPatternEmoji[bgRandom.nextInt(randomTheme.bgPatternEmoji.size)]
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

    var y = 88f
    textPaint.color = android.graphics.Color.parseColor("#1F2A24")
    textPaint.textSize = 52f
    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    canvas.drawText("今日记录", padding, y, textPaint)

    y += 66f
    textPaint.textSize = 30f
    textPaint.typeface = android.graphics.Typeface.DEFAULT
    textPaint.color = android.graphics.Color.parseColor("#4F5B56")
    canvas.drawText(selectedDate, padding, y, textPaint)

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
    val weightText = dailyRecord?.weight?.let { String.format(Locale.getDefault(), "%.1f kg", it) } ?: "记录"
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
    fun drawSectionTitle(title: String, calories: Int, color: Int) {
        textPaint.color = color
        textPaint.textSize = 32f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText(title, padding, currentY + 42f, textPaint)
        textPaint.color = android.graphics.Color.parseColor("#6A7570")
        textPaint.textSize = 28f
        textPaint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText("${calories} kcal", width - padding - 190f, currentY + 42f, textPaint)
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

    fun drawItemRow(item: CalorieItemEntity) {
        val rect = android.graphics.RectF(padding, currentY, width - padding, currentY + itemH - 14f)
        paint.color = android.graphics.Color.argb(150, 255, 255, 255)
        paint.setShadowLayer(5f, 0f, 2f, android.graphics.Color.parseColor("#16000000"))
        canvas.drawRoundRect(rect, 24f, 24f, paint)
        paint.clearShadowLayer()

        val isFood = item.type == "food"
        val iconBg = if (isFood) android.graphics.Color.parseColor("#DDF7D8") else android.graphics.Color.parseColor("#D9ECFF")
        val iconCx = padding + 44f
        val iconCy = currentY + 54f
        val imagePath = resolveImagePath(item)
        var thumbnailDrawn = false
        if (!imagePath.isNullOrBlank()) {
            val srcBitmap = decodeThumbnail(imagePath)
            if (srcBitmap != null) {
                val srcSide = kotlin.math.min(srcBitmap.width, srcBitmap.height)
                val srcLeft = (srcBitmap.width - srcSide) / 2
                val srcTop = (srcBitmap.height - srcSide) / 2
                val srcRect = android.graphics.Rect(srcLeft, srcTop, srcLeft + srcSide, srcTop + srcSide)
                val dstRect = android.graphics.RectF(iconCx - 30f, iconCy - 30f, iconCx + 30f, iconCy + 30f)
                val clipPath = android.graphics.Path().apply { addCircle(iconCx, iconCy, 30f, android.graphics.Path.Direction.CW) }
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
            canvas.drawCircle(iconCx, iconCy, 30f, paint)
            textPaint.textSize = 24f
            textPaint.color = if (isFood) android.graphics.Color.parseColor("#2E7D32") else android.graphics.Color.parseColor("#1565C0")
            canvas.drawText(if (isFood) "🍴" else "💪", iconCx - 17f, iconCy + 11f, textPaint)
        }

        textPaint.textSize = 30f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textPaint.color = android.graphics.Color.parseColor("#222C27")
        val name = if (item.name.length > 22) item.name.take(22) + "…" else item.name
        canvas.drawText(name, padding + 94f, currentY + 52f, textPaint)

        textPaint.typeface = android.graphics.Typeface.DEFAULT
        textPaint.textSize = 24f
        textPaint.color = android.graphics.Color.parseColor("#7B8681")
        canvas.drawText("${item.time} · ${if (isFood) "食物" else "运动"}", padding + 94f, currentY + 88f, textPaint)

        textPaint.textSize = 30f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textPaint.color = if (isFood) android.graphics.Color.parseColor("#2E7D32") else android.graphics.Color.parseColor("#1565C0")
        val calText = "${if (isFood) "+" else "-"}${item.calories}"
        val calWidth = textPaint.measureText(calText)
        canvas.drawText(calText, width - padding - calWidth - 12f, currentY + 70f, textPaint)
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
    canvas.drawLine(padding, footerTop + 36f, width - padding, footerTop + 36f, paint)

    val iconSize = 86f
    val iconX = width / 2f - 250f
    val iconY = footerTop + 74f
    val iconId = context.resources.getIdentifier("app_icon", "drawable", context.packageName)
    if (iconId != 0) {
        val iconBitmap = BitmapFactory.decodeResource(context.resources, iconId)
        if (iconBitmap != null) {
            val scaled = Bitmap.createScaledBitmap(iconBitmap, iconSize.toInt(), iconSize.toInt(), true)
            canvas.drawBitmap(scaled, iconX, iconY, null)
        }
    } else {
        paint.color = randomTheme.primaryColor
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

fun saveTodayBitmap(context: Context, bitmap: Bitmap) {
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
        } else {
            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
