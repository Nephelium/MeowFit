package com.example.calorietracker.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.calorietracker.CalorieTrackerApp
import com.example.calorietracker.data.CalorieItemEntity
import com.example.calorietracker.data.NutritionDatabase
import com.example.calorietracker.data.NutritionFoodItem
import com.example.calorietracker.ui.AiUiState
import com.example.calorietracker.ui.AiViewModel
import com.example.calorietracker.util.CalorieUtils
import com.example.calorietracker.util.ImageStorageUtils
import java.io.InputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

data class EntryItem(
    val type: String, // "food" or "exercise"
    val name: String,
    val calories: Double,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val time: String = "",
    val mealCategory: String? = null,
    val notes: String = "",
    val imagePath: String? = null
)

private fun createTempCameraUri(context: Context): Uri? {
    return runCatching {
        val file = File.createTempFile("meowfit_camera_", ".jpg", context.cacheDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

private fun allowDecimalInput(text: String): Boolean {
    if (text.isEmpty()) return true
    return text.matches(Regex("\\d*(\\.\\d{0,2})?"))
}

@Composable
private fun SuggestionRow(
    name: String,
    calories: Double,
    source: String,
    sourceColor: Color,
    onCardColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(name, color = onCardColor, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(6.dp))
                // Source badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = sourceColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        source,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = sourceColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                "${CalorieUtils.formatNumber(calories)} kcal",
                color = onCardColor.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun MediaSourceDialog(
    cardColor: Color,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onPickAlbum: () -> Unit,
    onTakePhoto: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardColor,
        titleContentColor = textColor,
        textContentColor = textColor,
        title = { Text("选择图片来源") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPickAlbum,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = cardColor.copy(alpha = 0.96f),
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
                        containerColor = cardColor.copy(alpha = 0.96f),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    targetDate: String = CalorieUtils.getTodayString(),
    aiViewModel: AiViewModel,
    selectedThemeIndex: Int = 0,
    userWeight: Float = 70f,
    showMacros: Boolean = false,
    onSave: (List<EntryItem>) -> Unit,
    onCancel: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(2) } // Default to Manual for safety
    val tabs = listOf("AI 对话", "拍照识别", "手动输入")
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val selectedTheme = remember(selectedThemeIndex) { getTodayVisualTheme(selectedThemeIndex) }
    val accentColor = remember(selectedTheme, isDarkTheme) { themedAccentColor(selectedTheme, isDarkTheme) }
    val cardColor = remember(selectedTheme, isDarkTheme) { themedDashboardCardColor(selectedTheme, isDarkTheme) }
    val onCardColor = com.example.calorietracker.ui.theme.onCardColor(cardColor, isDarkTheme)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("添加记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onCardColor)
                        Text(CalorieUtils.formatDate(targetDate), style = MaterialTheme.typography.bodySmall, color = onCardColor.copy(alpha = 0.72f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = accentColor)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = cardColor.copy(alpha = if (isDarkTheme) 0.92f else 0.95f)
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Custom Tab Indicator
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = cardColor.copy(alpha = if (isDarkTheme) 0.88f else 0.92f),
                contentColor = accentColor,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = accentColor
                    )
                },
                divider = { Divider(color = onCardColor.copy(alpha = 0.18f)) }
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
                        selectedContentColor = accentColor,
                        unselectedContentColor = onCardColor.copy(alpha = 0.72f)
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cardColor.copy(alpha = if (isDarkTheme) 0.94f else 0.96f))
            ) {
                when (selectedTab) {
                    0 -> AiDialogueTab(aiViewModel, userWeight, showMacros, onSave, accentColor, cardColor, onCardColor)
                    1 -> PhotoRecognitionTab(aiViewModel, userWeight, showMacros, onSave, accentColor, cardColor, onCardColor)
                    2 -> ManualInputTab(showMacros, onSave = { item -> onSave(listOf(item)) }, onCancel = onCancel, accentColor = accentColor, cardColor = cardColor, onCardColor = onCardColor)
                }
            }
        }
    }
}

@Composable
fun ManualInputTab(
    showMacros: Boolean,
    onSave: (EntryItem) -> Unit,
    onCancel: () -> Unit,
    accentColor: Color,
    cardColor: Color,
    onCardColor: Color
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
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showMediaSourceDialog by remember { mutableStateOf(false) }
    var historySuggestions by remember { mutableStateOf<List<CalorieItemEntity>>(emptyList()) }
    var nutritionSuggestions by remember { mutableStateOf<List<NutritionFoodItem>>(emptyList()) }

    // Weight adjustment state
    var suggestionSource by remember { mutableStateOf<String?>(null) } // "history" or "official"
    var weightGrams by remember { mutableStateOf("100") } // for official: current grams
    var useSameWeight by remember { mutableStateOf(true) } // for history: same weight toggle
    var previousGrams by remember { mutableStateOf("") } // for history: previous weight
    var currentGrams by remember { mutableStateOf("") }  // for history: current weight
    // Base values (per 100g for official, or original values for history)
    var baseCalories by remember { mutableStateOf(0.0) }
    var baseCarbs by remember { mutableStateOf(0.0) }
    var baseProtein by remember { mutableStateOf(0.0) }
    var baseFat by remember { mutableStateOf(0.0) }

    val context = LocalContext.current
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

    val app = context.applicationContext as CalorieTrackerApp
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val defaultNowTime = String.format("%02d:%02d", hour, minute)
    val mealCategoryOptions = remember {
        CalorieUtils.manualSelectableMealCategories.map { it.label }
    }
    var selectedMealCategory by remember { mutableStateOf<String?>(null) }
    var mealCategoryTouched by remember { mutableStateOf(false) }
    var showMealCategoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(type) {
        if (type == "exercise") {
            selectedMealCategory = null
            mealCategoryTouched = false
        }
    }

    LaunchedEffect(type, time) {
        if (type == "food" && !mealCategoryTouched) {
            val categoryTime = if (time.isBlank()) defaultNowTime else time
            selectedMealCategory = CalorieUtils.getMealCategoryByTime(categoryTime).label
        }
    }

    LaunchedEffect(type, name) {
        val keyword = name.trim()
        if (keyword.isBlank()) {
            historySuggestions = emptyList()
            nutritionSuggestions = emptyList()
            suggestionSource = null
        } else {
            delay(120)
            // Search history (distinct by name, max 5 shown first)
            historySuggestions = app.repository
                .searchRecentItemsByTypeAndPrefix(type, keyword, 10)
                .distinctBy { it.name }
                .take(5)
            // Search official nutrition database (max 20, food only)
            nutritionSuggestions = if (type == "food") NutritionDatabase.search(keyword, 20) else emptyList()
        }
    }

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


    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            // Type Selection Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.96f)),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(8.dp)) {
                    val types = listOf("food" to "食物", "exercise" to "运动")
                    types.forEach { (key, label) ->
                        val selected = type == key
                        val bgColor = if (selected) accentColor else Color.Transparent
                        val contentColor = if (selected) Color.White else onCardColor.copy(alpha = 0.78f)
                        
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
                colors = CardDefaults.cardColors(
                    containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        cardColor.copy(alpha = 0.92f)
                    }
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

                    // ========== Combined Suggestions (History + Official) ==========
                    val hasSuggestions = historySuggestions.isNotEmpty() || nutritionSuggestions.isNotEmpty()
                    if (hasSuggestions) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)  // tighter spacing
                        ) {
                            // History items first
                            historySuggestions.forEach { historyItem ->
                                SuggestionRow(
                                    name = historyItem.name,
                                    calories = historyItem.calories,
                                    source = "历史",
                                    sourceColor = accentColor.copy(alpha = 0.8f),
                                    onCardColor = onCardColor,
                                    onClick = {
                                        name = historyItem.name
                                        baseCalories = historyItem.calories
                                        baseCarbs = historyItem.carbs
                                        baseProtein = historyItem.protein
                                        baseFat = historyItem.fat
                                        calories = CalorieUtils.formatNumber(historyItem.calories)
                                        carbs = CalorieUtils.formatNumber(historyItem.carbs)
                                        protein = CalorieUtils.formatNumber(historyItem.protein)
                                        fat = CalorieUtils.formatNumber(historyItem.fat)
                                        notes = historyItem.notes.orEmpty()
                                        suggestionSource = "history"
                                        useSameWeight = true
                                        previousGrams = ""
                                        currentGrams = ""
                                        historySuggestions = emptyList()
                                        nutritionSuggestions = emptyList()
                                    }
                                )
                            }
                            // Official nutrition items
                            nutritionSuggestions.forEach { nutritionItem ->
                                SuggestionRow(
                                    name = nutritionItem.foodName,
                                    calories = nutritionItem.calories,
                                    source = "官方",
                                    sourceColor = Color(0xFF4CAF50),
                                    onCardColor = onCardColor,
                                    onClick = {
                                        name = nutritionItem.foodName
                                        baseCalories = nutritionItem.calories
                                        baseCarbs = nutritionItem.carbsG
                                        baseProtein = nutritionItem.proteinG
                                        baseFat = nutritionItem.fatG
                                        calories = CalorieUtils.formatNumber(nutritionItem.calories)
                                        carbs = CalorieUtils.formatNumber(nutritionItem.carbsG)
                                        protein = CalorieUtils.formatNumber(nutritionItem.proteinG)
                                        fat = CalorieUtils.formatNumber(nutritionItem.fatG)
                                        suggestionSource = "official"
                                        weightGrams = "100"
                                        historySuggestions = emptyList()
                                        nutritionSuggestions = emptyList()
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = calories,
                        onValueChange = { if (allowDecimalInput(it)) calories = it },
                        label = { Text("热量 (kcal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Star, null) }, // Use star icon as fallback for calories
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    if (showMacros && type == "food") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = carbs,
                                onValueChange = { if (allowDecimalInput(it)) carbs = it },
                                label = { Text("碳水") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = protein,
                                onValueChange = { if (allowDecimalInput(it)) protein = it },
                                label = { Text("蛋白质") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = fat,
                                onValueChange = { if (allowDecimalInput(it)) fat = it },
                                label = { Text("脂肪") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                    }

                    // ========== Weight Adjustment Section ==========
                    if (type == "food" && suggestionSource != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (suggestionSource == "official") 
                                    Color(0xFF4CAF50).copy(alpha = 0.08f) 
                                else 
                                    accentColor.copy(alpha = 0.06f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (suggestionSource == "official") {
                                    // Official: per 100g, user inputs actual grams
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "📋 官方数据",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            "默认100g",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = onCardColor.copy(alpha = 0.5f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = weightGrams,
                                        onValueChange = { if (allowDecimalInput(it)) weightGrams = it },
                                        label = { Text("实际克数") },
                                        placeholder = { Text("例如: 250") },
                                        suffix = { Text("g") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                } else {
                                    // History: ask same weight?
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "🕐 历史记录",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("相同重量", style = MaterialTheme.typography.labelSmall, color = onCardColor.copy(alpha = 0.7f))
                                            Switch(
                                                checked = useSameWeight,
                                                onCheckedChange = {
                                                    useSameWeight = it
                                                    if (it) {
                                                        // Reset to original values
                                                        calories = CalorieUtils.formatNumber(baseCalories)
                                                        carbs = CalorieUtils.formatNumber(baseCarbs)
                                                        protein = CalorieUtils.formatNumber(baseProtein)
                                                        fat = CalorieUtils.formatNumber(baseFat)
                                                    }
                                                },
                                                colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.4f))
                                            )
                                        }
                                    }
                                    if (!useSameWeight) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = previousGrams,
                                                onValueChange = { if (allowDecimalInput(it)) previousGrams = it },
                                                label = { Text("之前克数") },
                                                suffix = { Text("g") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = currentGrams,
                                                onValueChange = { if (allowDecimalInput(it)) currentGrams = it },
                                                label = { Text("现在克数") },
                                                suffix = { Text("g") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Auto-recalculate when weight inputs change
                        LaunchedEffect(suggestionSource, weightGrams, useSameWeight, previousGrams, currentGrams) {
                            if (suggestionSource == "official") {
                                val grams = CalorieUtils.parseDecimalInput(weightGrams) ?: 100.0
                                val ratio = grams / 100.0
                                calories = CalorieUtils.formatNumber(baseCalories * ratio)
                                carbs = CalorieUtils.formatNumber(baseCarbs * ratio)
                                protein = CalorieUtils.formatNumber(baseProtein * ratio)
                                fat = CalorieUtils.formatNumber(baseFat * ratio)
                            } else if (suggestionSource == "history" && !useSameWeight) {
                                val prev = CalorieUtils.parseDecimalInput(previousGrams)
                                val curr = CalorieUtils.parseDecimalInput(currentGrams)
                                if (prev != null && curr != null && prev > 0) {
                                    val ratio = curr / prev
                                    calories = CalorieUtils.formatNumber(baseCalories * ratio)
                                    carbs = CalorieUtils.formatNumber(baseCarbs * ratio)
                                    protein = CalorieUtils.formatNumber(baseProtein * ratio)
                                    fat = CalorieUtils.formatNumber(baseFat * ratio)
                                }
                            }
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

                        Box {
                            OutlinedTextField(
                                value = selectedMealCategory ?: "",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("类别") },
                                placeholder = { Text("请选择类别") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Restaurant, null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showMealCategoryDialog = true }
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
                        val duration = CalorieUtils.calculateDuration(startTime, endTime)
                        if (duration > 0) {
                            Text(
                                "时长: $duration 分钟", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = accentColor,
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
                        singleLine = false,
                        minLines = 3,
                        maxLines = 5
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showMediaSourceDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = cardColor.copy(alpha = 0.96f),
                                contentColor = accentColor
                            ),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.56f))
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = accentColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (selectedImageUri == null) "添加备注图片" else "更换图片", color = accentColor)
                        }
                        if (selectedImageUri != null) {
                            TextButton(onClick = { selectedImageUri = null }) {
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
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val calValue = CalorieUtils.parseDecimalInput(calories) ?: 0.0
                    val carbsValue = CalorieUtils.parseDecimalInput(carbs) ?: 0.0
                    val proteinValue = CalorieUtils.parseDecimalInput(protein) ?: 0.0
                    val fatValue = CalorieUtils.parseDecimalInput(fat) ?: 0.0
                    
                    if (name.isNotEmpty() && calValue > 0.0) {
                        val compressedImagePath = selectedImageUri?.let { ImageStorageUtils.compressAndSaveImage(context, it) }
                        if (type == "exercise") {
                            val duration = CalorieUtils.calculateDuration(startTime, endTime)
                            // Use startTime as the record time
                            val recordTime = if (startTime.isNotBlank()) startTime else time
                            val durationNote = if (duration > 0) "时长: $duration 分钟" else ""
                            val finalNotes = if (notes.isNotBlank()) "$notes${if(durationNote.isNotBlank()) ", $durationNote" else ""}" else durationNote
                            
                            onSave(EntryItem(type = type, name = name, calories = calValue, time = recordTime, notes = finalNotes, imagePath = compressedImagePath))
                        } else {
                            onSave(
                                EntryItem(
                                    type = type,
                                    name = name,
                                    calories = calValue,
                                    carbs = carbsValue,
                                    protein = proteinValue,
                                    fat = fatValue,
                                    time = time,
                                    mealCategory = selectedMealCategory,
                                    notes = notes,
                                    imagePath = compressedImagePath
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
            ) {
                Text("保存记录", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showMealCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showMealCategoryDialog = false },
            containerColor = cardColor,
            titleContentColor = onCardColor,
            textContentColor = onCardColor,
            title = { Text("选择类别") },
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
                                mealCategoryTouched = true
                                showMealCategoryDialog = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) accentColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selected) accentColor.copy(alpha = 0.7f) else onCardColor.copy(alpha = 0.18f)
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
                                    color = if (selected) accentColor else onCardColor.copy(alpha = 0.9f),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = accentColor,
                                        unselectedColor = onCardColor.copy(alpha = 0.45f)
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

    if (showMediaSourceDialog) {
        MediaSourceDialog(
            cardColor = cardColor,
            textColor = onCardColor,
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
}

@Composable
fun AiDialogueTab(
    viewModel: AiViewModel,
    userWeight: Float,
    showMacros: Boolean,
    onSave: (List<EntryItem>) -> Unit,
    accentColor: Color,
    cardColor: Color,
    onCardColor: Color
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
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showMediaSourceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = uris
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            selectedImageUris = selectedImageUris + listOfNotNull(pendingCameraUri)
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
            accentColor = accentColor,
            cardColor = cardColor,
            onCardColor = onCardColor,
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
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
            items(displayMessages) { msg ->
                ChatBubble(msg, accentColor, cardColor, onCardColor)
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
                             tint = accentColor
                         )
                         Surface(
                             shape = MaterialTheme.shapes.medium.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp)),
                             color = cardColor.copy(alpha = 0.92f),
                             shadowElevation = 1.dp
                         ) {
                             Row(
                                 modifier = Modifier.padding(12.dp),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 CircularProgressIndicator(
                                     modifier = Modifier.size(16.dp),
                                     strokeWidth = 2.dp,
                                     color = onCardColor.copy(alpha = 0.8f)
                                 )
                                 Spacer(modifier = Modifier.width(8.dp))
                                 Text(
                                     text = "AI正在思考...",
                                     style = MaterialTheme.typography.bodyMedium,
                                     color = onCardColor.copy(alpha = 0.8f)
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
                colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = if (androidx.compose.foundation.isSystemInDarkTheme()) 0.85f else 0.78f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "待添加记录 (${recognizedItems.size})", 
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
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
                                onEdit = { editingIndex = index },
                                accentColor = accentColor,
                                cardColor = cardColor,
                                onCardColor = onCardColor
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                    ) {
                        val totalCal = recognizedItems.sumOf { if (it.type == "food") it.calories else -it.calories }
                        Text("确认添加 (${CalorieUtils.formatNumber(totalCal)} kcal)")
                    }
                }
            }
        }

        // Input Area
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.94f)),
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
                        showMediaSourceDialog = true
                    }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Image", tint = accentColor)
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
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = accentColor)
                        }
                    }
                }
            }
        }
        if (showMediaSourceDialog) {
            MediaSourceDialog(
                cardColor = cardColor,
                textColor = onCardColor,
                accentColor = accentColor,
                onDismiss = { showMediaSourceDialog = false },
                onPickAlbum = {
                    showMediaSourceDialog = false
                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
        }
        
        // Floating Clear Button (Top End)
        if (displayMessages.isNotEmpty()) {
            SmallFloatingActionButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                containerColor = cardColor.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.error
            ) {
                Icon(Icons.Default.Delete, "Clear Chat")
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    accentColor: Color,
    cardColor: Color,
    onCardColor: Color
) {
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
                 tint = accentColor
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
                    color = if (isUser) accentColor else cardColor.copy(alpha = 0.94f),
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
                        color = if (isUser) Color.White else onCardColor.copy(alpha = 0.88f),
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
                 tint = accentColor.copy(alpha = 0.82f)
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
    onSave: (List<EntryItem>) -> Unit,
    accentColor: Color,
    cardColor: Color,
    onCardColor: Color
) {
    val context = LocalContext.current
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showMediaSourceDialog by remember { mutableStateOf(false) }
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
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            selectedImageUris = selectedImageUris + listOfNotNull(pendingCameraUri)
            viewModel.clearPhotoState()
            viewModel.clearPhotoItems()
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

    // Edit Logic
    var editingIndex by remember { mutableIntStateOf(-1) }
    
    if (editingIndex != -1 && editingIndex < recognizedItems.size) {
        EditEntryDialog(
            item = recognizedItems[editingIndex],
            showMacros = showMacros,
            accentColor = accentColor,
            cardColor = cardColor,
            onCardColor = onCardColor,
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
                    .background(cardColor.copy(alpha = 0.8f))
                    .clickable { showMediaSourceDialog = true },
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
                            tint = accentColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("点击选择图片 (支持多张)", style = MaterialTheme.typography.bodyMedium, color = onCardColor.copy(alpha = 0.78f))
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
            minLines = 3,
            maxLines = 5
        )
        }

        // 3. Action Buttons
        item {
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
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
                        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.82f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onCardColor.copy(alpha = 0.86f),
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
                    onEdit = { editingIndex = index },
                    accentColor = accentColor,
                    cardColor = cardColor,
                    onCardColor = onCardColor
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
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                ) {
                    Text("确认添加", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(32.dp)) // Extra space at bottom
            }
        }
    }

    if (showMediaSourceDialog) {
        MediaSourceDialog(
            cardColor = cardColor,
            textColor = onCardColor,
            accentColor = accentColor,
            onDismiss = { showMediaSourceDialog = false },
            onPickAlbum = {
                showMediaSourceDialog = false
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
}

@Composable
fun RecognizedItemCard(
    item: EntryItem,
    showMacros: Boolean = false,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    accentColor: Color,
    cardColor: Color,
    onCardColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.78f)),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, onCardColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onCardColor)
                val timeInfo = if (item.time.isNotBlank()) " · ${item.time}" else ""
                val notesInfo = if (item.notes.isNotBlank()) " · ${item.notes}" else ""
                val imageInfo = if (!item.imagePath.isNullOrBlank()) " · 已附图" else ""
                val macroInfo = if (showMacros && item.type == "food") {
                    " · 碳${CalorieUtils.formatNumber(item.carbs)} 蛋${CalorieUtils.formatNumber(item.protein)} 脂${CalorieUtils.formatNumber(item.fat)}"
                } else ""
                Text("${CalorieUtils.formatNumber(item.calories)} kcal · ${if (item.type == "food") "食物" else "运动"}$macroInfo$timeInfo$notesInfo$imageInfo", style = MaterialTheme.typography.bodySmall, color = onCardColor.copy(alpha = 0.72f))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = accentColor)
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
    accentColor: Color,
    cardColor: Color,
    onCardColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (EntryItem) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var calories by remember { mutableStateOf(CalorieUtils.formatNumber(item.calories)) }
    var carbs by remember { mutableStateOf(CalorieUtils.formatNumber(item.carbs)) }
    var protein by remember { mutableStateOf(CalorieUtils.formatNumber(item.protein)) }
    var fat by remember { mutableStateOf(CalorieUtils.formatNumber(item.fat)) }
    var time by remember { mutableStateOf(item.time) }
    var notes by remember { mutableStateOf(item.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑记录", color = onCardColor) },
        containerColor = cardColor,
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
                    onValueChange = { if (allowDecimalInput(it)) calories = it },
                    label = { Text("卡路里") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (showMacros && item.type == "food") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { if (allowDecimalInput(it)) carbs = it },
                            label = { Text("碳水") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { if (allowDecimalInput(it)) protein = it },
                            label = { Text("蛋白质") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = fat,
                            onValueChange = { if (allowDecimalInput(it)) fat = it },
                            label = { Text("脂肪") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    shape = RoundedCornerShape(12.dp),
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                val cal = CalorieUtils.parseDecimalInput(calories) ?: 0.0
                val c = CalorieUtils.parseDecimalInput(carbs) ?: 0.0
                val p = CalorieUtils.parseDecimalInput(protein) ?: 0.0
                val f = CalorieUtils.parseDecimalInput(fat) ?: 0.0

                if (name.isNotBlank()) {
                    onConfirm(item.copy(name = name, calories = cal, carbs = c, protein = p, fat = f, time = time, notes = notes))
                }
            },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.72f), contentColor = Color.White)
            ) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
