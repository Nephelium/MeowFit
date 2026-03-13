package com.example.calorietracker.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.calorietracker.ui.AiUiState
import com.example.calorietracker.ui.AiViewModel
import com.example.calorietracker.util.CalorieUtils
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class EntryItem(
    val type: String, // "food" or "exercise"
    val name: String,
    val calories: Int,
    val carbs: Int = 0,
    val protein: Int = 0,
    val fat: Int = 0,
    val time: String = "",
    val notes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    targetDate: String = CalorieUtils.getTodayString(),
    aiViewModel: AiViewModel,
    userWeight: Float = 70f,
    showMacros: Boolean = false,
    onSave: (List<EntryItem>) -> Unit,
    onCancel: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(2) } // Default to Manual for safety
    val tabs = listOf("AI 对话", "拍照识别", "手动输入")

    // Remove automatic state clearing to persist chat/results across navigation
    // LaunchedEffect(Unit) {
    //    aiViewModel.clearState()
    // }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("添加记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(CalorieUtils.formatDate(targetDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Custom Tab Indicator
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = { Divider(color = MaterialTheme.colorScheme.surfaceVariant) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title, 
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface) // Light background for content area
            ) {
                when (selectedTab) {
                    0 -> AiDialogueTab(aiViewModel, userWeight, showMacros, onSave)
                    1 -> PhotoRecognitionTab(aiViewModel, userWeight, showMacros, onSave)
                    2 -> ManualInputTab(showMacros, onSave = { item -> onSave(listOf(item)) }, onCancel = onCancel)
                }
            }
        }
    }
}

@Composable
fun ManualInputTab(
    showMacros: Boolean,
    onSave: (EntryItem) -> Unit,
    onCancel: () -> Unit
) {
    var type by remember { mutableStateOf("food") }
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    fun showTimePicker(onTimeSelected: (String) -> Unit) {
        android.app.TimePickerDialog(
            context,
            { _, h, m ->
                onTimeSelected(String.format("%02d:%02d", h, m))
            },
            hour,
            minute,
            true
        ).show()
    }

    // Helper to calculate duration
    fun calculateDuration(): Int {
        if (startTime.isBlank() || endTime.isBlank()) return 0
        try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val start = format.parse(startTime)
            val end = format.parse(endTime)
            if (start != null && end != null) {
                var diff = end.time - start.time
                // If end time is earlier than start time, assume it's next day
                if (diff < 0) diff += 24 * 60 * 60 * 1000 
                return (diff / (1000 * 60)).toInt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            // Type Selection Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(8.dp)) {
                    val types = listOf("food" to "食物", "exercise" to "运动")
                    types.forEach { (key, label) ->
                        val selected = type == key
                        val bgColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                        val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .clickable { type = key }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (key == "food") Icons.Default.Restaurant else Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = contentColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = contentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("名称") },
                        placeholder = { Text(if(type=="food") "例如: 米饭" else "例如: 跑步") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(if (type == "food") Icons.Default.Restaurant else Icons.Default.FitnessCenter, null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text("热量 (kcal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Star, null) }, // Use star icon as fallback for calories
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    if (showMacros && type == "food") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = carbs,
                                onValueChange = { if (it.all { char -> char.isDigit() }) carbs = it },
                                label = { Text("碳水") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = protein,
                                onValueChange = { if (it.all { char -> char.isDigit() }) protein = it },
                                label = { Text("蛋白质") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = fat,
                                onValueChange = { if (it.all { char -> char.isDigit() }) fat = it },
                                label = { Text("脂肪") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                    }
                    
                    if (type == "food") {
                        Box {
                            OutlinedTextField(
                                value = time,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("时间 (可选)") },
                                placeholder = { Text("HH:mm") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Schedule, null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showTimePicker { time = it } }
                            )
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("开始") },
                                    placeholder = { Text("HH:mm") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Timer, null) },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showTimePicker { startTime = it } }
                                )
                            }
                            
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = endTime,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("结束") },
                                    placeholder = { Text("HH:mm") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Outlined.Timer, null) },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showTimePicker { endTime = it } }
                                )
                            }
                        }
                        val duration = calculateDuration()
                        if (duration > 0) {
                            Text(
                                "时长: $duration 分钟", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                    
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("备注 (可选)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Notes, null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    val calInt = calories.toIntOrNull() ?: 0
                    val carbsInt = carbs.toIntOrNull() ?: 0
                    val proteinInt = protein.toIntOrNull() ?: 0
                    val fatInt = fat.toIntOrNull() ?: 0
                    
                    if (name.isNotEmpty() && calInt > 0) {
                        if (type == "exercise") {
                            val duration = calculateDuration()
                            // Use startTime as the record time
                            val recordTime = if (startTime.isNotBlank()) startTime else time
                            val durationNote = if (duration > 0) "时长: $duration 分钟" else ""
                            val finalNotes = if (notes.isNotBlank()) "$notes${if(durationNote.isNotBlank()) ", $durationNote" else ""}" else durationNote
                            
                            onSave(EntryItem(type = type, name = name, calories = calInt, time = recordTime, notes = finalNotes))
                        } else {
                            onSave(EntryItem(type = type, name = name, calories = calInt, carbs = carbsInt, protein = proteinInt, fat = fatInt, time = time, notes = notes))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("保存记录", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun AiDialogueTab(
    viewModel: AiViewModel,
    userWeight: Float,
    showMacros: Boolean,
    onSave: (List<EntryItem>) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // Use messages from ViewModel (DB)
    val dbMessages by viewModel.chatMessages.collectAsState()
    
    // Welcome message logic
    val welcomeMessage = ChatMessage(
        role = "assistant",
        content = "你好！我是你的AI营养师。请告诉我你今天吃了什么或做了什么运动，我会帮你记录。"
    )
    val displayMessages = if (dbMessages.isEmpty()) listOf(welcomeMessage) else dbMessages

    // Use ViewModel state for recognized items
    val recognizedItems by viewModel.chatItemsFlow.collectAsState()
    
    // Image selection
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = uris
        }
    }

    // Clear history dialog
    var showClearDialog by remember { mutableStateOf(false) }
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空对话") },
            text = { Text("确定要清空所有对话历史吗？") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.clearHistory()
                    showClearDialog = false 
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

    // Auto scroll
    LaunchedEffect(displayMessages.size, uiState) {
        if (displayMessages.isNotEmpty()) {
            val targetIndex = displayMessages.size + (if (uiState is AiUiState.Loading) 0 else -1)
            if (targetIndex >= 0) {
                 listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Edit Logic
    var editingIndex by remember { mutableIntStateOf(-1) }
    
    if (editingIndex != -1 && editingIndex < recognizedItems.size) {
        EditEntryDialog(
            item = recognizedItems[editingIndex],
            showMacros = showMacros,
            onDismiss = { editingIndex = -1 },
            onConfirm = { newItem ->
                viewModel.updateChatItem(editingIndex, newItem)
                editingIndex = -1
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header removed (Clear Chat moved to FAB)

            // Chat Area
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp, top = 40.dp) // Add top padding for FAB space
            ) {
            items(displayMessages) { msg ->
                ChatBubble(msg)
            }
            if (uiState is AiUiState.Loading) {
                 item {
                     Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                     ) {
                         Icon(
                             Icons.Default.SmartToy, 
                             contentDescription = "AI", 
                             modifier = Modifier.padding(top = 8.dp, end = 8.dp).size(28.dp),
                             tint = MaterialTheme.colorScheme.primary
                         )
                         Surface(
                             shape = MaterialTheme.shapes.medium.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp)),
                             color = MaterialTheme.colorScheme.surfaceVariant,
                             shadowElevation = 1.dp
                         ) {
                             Row(
                                 modifier = Modifier.padding(12.dp),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 CircularProgressIndicator(
                                     modifier = Modifier.size(16.dp),
                                     strokeWidth = 2.dp,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                                 Spacer(modifier = Modifier.width(8.dp))
                                 Text(
                                     text = "AI正在思考...",
                                     style = MaterialTheme.typography.bodyMedium,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                             }
                         }
                     }
                 }
            }
            if (uiState is AiUiState.Error) {
                item {
                    Text(
                        text = (uiState as AiUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        
        // Pending Items Area (Cart)
        if (recognizedItems.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "待添加记录 (${recognizedItems.size})", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 150.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(recognizedItems) { index, item ->
                            RecognizedItemCard(
                                item = item,
                                showMacros = showMacros,
                                onDelete = { viewModel.removeChatItem(index) },
                                onEdit = { editingIndex = index }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { 
                            onSave(recognizedItems.toList())
                            viewModel.clearChatItems()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val totalCal = recognizedItems.sumOf { if (it.type == "food") it.calories else -it.calories }
                        Text("确认添加 ($totalCal kcal)")
                    }
                }
            }
        }

        // Input Area
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Image Preview
                if (selectedImageUris.isNotEmpty()) {
                    Box(modifier = Modifier.padding(8.dp)) {
                         androidx.compose.foundation.lazy.LazyRow(
                             horizontalArrangement = Arrangement.spacedBy(8.dp),
                             contentPadding = PaddingValues(end = 32.dp)
                         ) {
                             items(selectedImageUris) { uri ->
                                 AsyncImage(
                                     model = uri,
                                     contentDescription = "Selected Image",
                                     modifier = Modifier.height(100.dp).aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                                 )
                             }
                         }
                        
                        IconButton(
                            onClick = { 
                                selectedImageUris = emptyList()
                            },
                            modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha=0.5f), CircleShape).size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Image", tint = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text(if (selectedImageUris.isNotEmpty()) "描述图片内容..." else "吃了什么？做了什么运动？") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = {
                        if (inputText.isNotBlank() || selectedImageUris.isNotEmpty()) {
                            if (selectedImageUris.isNotEmpty()) {
                                val bitmaps = selectedImageUris.mapNotNull { uri ->
                                    try {
                                        val stream = context.contentResolver.openInputStream(uri)
                                        BitmapFactory.decodeStream(stream)
                                    } catch (e: Exception) { null }
                                }
                                val uriStrings = selectedImageUris.map { it.toString() }
                                viewModel.sendMessageWithImage(inputText, bitmaps, uriStrings, userWeight)
                                selectedImageUris = emptyList()
                            } else {
                                viewModel.sendMessage(inputText, userWeight)
                            }
                            inputText = ""
                        }
                    }, enabled = (inputText.isNotBlank() || selectedImageUris.isNotEmpty()) && uiState !is AiUiState.Loading) {
                        if (uiState is AiUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        }
        
        // Floating Clear Button (Top End)
        if (displayMessages.isNotEmpty()) {
            SmallFloatingActionButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.error
            ) {
                Icon(Icons.Default.Delete, "Clear Chat")
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
             Icon(
                 Icons.Default.SmartToy, 
                 contentDescription = "AI", 
                 modifier = Modifier.padding(top = 8.dp, end = 8.dp).size(28.dp),
                 tint = MaterialTheme.colorScheme.primary
             )
        }
        
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            // Display Image if present
            if (!message.imageUrl.isNullOrBlank()) {
                val uris = message.imageUrl.split("|")
                if (uris.size > 1) {
                    // Multiple images - use a row or grid
                     androidx.compose.foundation.lazy.LazyRow(
                         horizontalArrangement = Arrangement.spacedBy(8.dp),
                         modifier = Modifier.padding(bottom = 4.dp)
                     ) {
                         items(uris) { uriStr ->
                             AsyncImage(
                                 model = Uri.parse(uriStr),
                                 contentDescription = "User Image",
                                 modifier = Modifier
                                     .size(150.dp)
                                     .clip(RoundedCornerShape(12.dp))
                                     .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                             )
                         }
                     }
                } else {
                    // Single image
                    AsyncImage(
                        model = Uri.parse(uris[0]),
                        contentDescription = "User Image",
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .widthIn(max = 200.dp)
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )
                }
            }

            if (message.content.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.medium.copy(
                        bottomStart = if (!isUser) androidx.compose.foundation.shape.CornerSize(0.dp) else MaterialTheme.shapes.medium.bottomStart,
                        bottomEnd = if (isUser) androidx.compose.foundation.shape.CornerSize(0.dp) else MaterialTheme.shapes.medium.bottomEnd
                    ),
                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clickable {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(message.content))
                            android.widget.Toast.makeText(context, "已复制", android.widget.Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(12.dp),
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        if (isUser) {
             Icon(
                 Icons.Default.Person, 
                 contentDescription = "User", 
                 modifier = Modifier.padding(top = 8.dp, start = 8.dp).size(28.dp),
                 tint = MaterialTheme.colorScheme.secondary
             )
        }
    }
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user", "assistant"
    val content: String,
    val imageUrl: String? = null,
    val items: List<EntryItem> = emptyList()
)

@Composable
fun PhotoRecognitionTab(
    viewModel: AiViewModel,
    userWeight: Float,
    showMacros: Boolean,
    onSave: (List<EntryItem>) -> Unit
) {
    val context = LocalContext.current
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val uiState by viewModel.photoUiState.collectAsState()
    
    val recognizedItems by viewModel.photoItemsFlow.collectAsState()
    var notes by remember { mutableStateOf("") }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = uris
            // Auto reset state on new image
            viewModel.clearPhotoState()
            viewModel.clearPhotoItems() // Clear previous results when selecting new images
        }
    }

    // Edit Logic
    var editingIndex by remember { mutableIntStateOf(-1) }
    
    if (editingIndex != -1 && editingIndex < recognizedItems.size) {
        EditEntryDialog(
            item = recognizedItems[editingIndex],
            showMacros = showMacros,
            onDismiss = { editingIndex = -1 },
            onConfirm = { newItem ->
                viewModel.updatePhotoItem(editingIndex, newItem)
                editingIndex = -1
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Image Picker Area
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUris.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(selectedImageUris) { uri ->
                             AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("点击选择图片 (支持多张)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        
        // 2. Notes Input
        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("备注 (可选)") },
                placeholder = { Text("例如：这碗面大概多少卡？") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        }

        // 3. Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { 
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重选")
                }
                
                Button(
                    onClick = {
                        if (selectedImageUris.isNotEmpty()) {
                            val bitmaps = selectedImageUris.mapNotNull { uri ->
                                try {
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    BitmapFactory.decodeStream(inputStream)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmaps.isNotEmpty()) {
                                viewModel.analyzeImage(bitmaps, userWeight, notes)
                            }
                        }
                    },
                    enabled = selectedImageUris.isNotEmpty() && uiState !is AiUiState.Loading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState is AiUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("开始识别")
                    }
                }
            }
        }
        
        // 4. Error Message
        if (uiState is AiUiState.Error) {
            item {
                Text(
                    text = (uiState as AiUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        
        // 5. Success Summary
        if (uiState is AiUiState.Success) {
            val summary = (uiState as AiUiState.Success).summary
            if (!summary.isNullOrBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // 6. Recognized Items List Header
        if (recognizedItems.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("识别结果", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    TextButton(onClick = { viewModel.clearPhotoItems() }) {
                        Text("清空结果", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            // 7. Recognized Items
            itemsIndexed(recognizedItems) { index, item ->
                RecognizedItemCard(
                    item = item,
                    showMacros = showMacros,
                    onDelete = { viewModel.removePhotoItem(index) },
                    onEdit = { editingIndex = index }
                )
            }
            
            // 8. Confirm Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        onSave(recognizedItems.toList())
                        viewModel.clearPhotoItems()
                    },
                    enabled = recognizedItems.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("确认添加", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(32.dp)) // Extra space at bottom
            }
        }
    }
}

@Composable
fun RecognizedItemCard(
    item: EntryItem,
    showMacros: Boolean = false,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val timeInfo = if (item.time.isNotBlank()) " · ${item.time}" else ""
                val notesInfo = if (item.notes.isNotBlank()) " · ${item.notes}" else ""
                val macroInfo = if (showMacros && item.type == "food") {
                    " · 碳${item.carbs} 蛋${item.protein} 脂${item.fat}"
                } else ""
                Text("${item.calories} kcal · ${if (item.type == "food") "食物" else "运动"}$macroInfo$timeInfo$notesInfo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun EditEntryDialog(
    item: EntryItem,
    showMacros: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (EntryItem) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var calories by remember { mutableStateOf(item.calories.toString()) }
    var carbs by remember { mutableStateOf(item.carbs.toString()) }
    var protein by remember { mutableStateOf(item.protein.toString()) }
    var fat by remember { mutableStateOf(item.fat.toString()) }
    var time by remember { mutableStateOf(item.time) }
    var notes by remember { mutableStateOf(item.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑记录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("卡路里") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (showMacros && item.type == "food") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { if (it.all { char -> char.isDigit() }) carbs = it },
                            label = { Text("碳水") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { if (it.all { char -> char.isDigit() }) protein = it },
                            label = { Text("蛋白质") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = fat,
                            onValueChange = { if (it.all { char -> char.isDigit() }) fat = it },
                            label = { Text("脂肪") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("时间") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cal = calories.toIntOrNull() ?: 0
                val c = carbs.toIntOrNull() ?: 0
                val p = protein.toIntOrNull() ?: 0
                val f = fat.toIntOrNull() ?: 0

                if (name.isNotBlank()) {
                    onConfirm(item.copy(name = name, calories = cal, carbs = c, protein = p, fat = f, time = time, notes = notes))
                }
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
