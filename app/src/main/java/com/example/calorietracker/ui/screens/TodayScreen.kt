package com.example.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.calorietracker.data.CalorieItemEntity
import com.example.calorietracker.data.DailyRecordEntity
import com.example.calorietracker.data.UserProfileEntity
import com.example.calorietracker.util.CalorieUtils
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
    var showWeightDialog by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerStartTime by remember { mutableStateOf<Long?>(null) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var exerciseName by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<CalorieItemEntity?>(null) }

    // Calculate effective weight for today (or selected date)
    // If dailyRecord.weight is set, use it. Otherwise find previous.
    val effectiveWeight = remember(dailyRecord, allRecords, selectedDate, userProfile) {
        CalorieUtils.getEffectiveWeight(selectedDate, allRecords, userProfile)
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

            items(items) { item ->
                RecordItem(
                    item = item, 
                    onDelete = onDeleteItem, 
                    onEdit = { editingItem = it }
                )
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
    var time by remember { mutableStateOf(item.time) }
    var notes by remember { mutableStateOf(item.notes ?: "") }
    
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
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
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
                                
                                onConfirm(item.copy(name = name, calories = cal, time = startTimeStr, notes = newNotes))
                            } else {
                                onConfirm(item.copy(name = name, calories = cal, time = time, notes = notes))
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
fun RecordItem(item: CalorieItemEntity, onDelete: (CalorieItemEntity) -> Unit, onEdit: (CalorieItemEntity) -> Unit) {
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
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${item.time} · ${if(isFood) "食物" else "运动"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
