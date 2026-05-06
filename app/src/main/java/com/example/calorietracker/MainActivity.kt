package com.example.calorietracker

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.calorietracker.ui.MainViewModel
import com.example.calorietracker.ui.MainViewModelFactory
import com.example.calorietracker.ui.BackupViewModel
import com.example.calorietracker.ui.BackupViewModelFactory
import com.example.calorietracker.ui.screens.*
import com.example.calorietracker.ui.theme.CalorieTrackerTheme
import com.example.calorietracker.ui.AiViewModel
import com.example.calorietracker.util.IconManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        
        val app = application as CalorieTrackerApp
        val viewModel: MainViewModel by viewModels { MainViewModelFactory(app.repository) }
        val backupViewModel: BackupViewModel by viewModels { BackupViewModelFactory(app) }
        val aiViewModel: AiViewModel by viewModels()

        setContent {
            CalorieTrackerTheme {
                val userProfile by viewModel.userProfile.collectAsState()
                
                // If no profile, force setup first (simplified flow)
                if (userProfile == null) {
                    ProfileSetupScreen(
                        onSave = { profile ->
                            viewModel.saveProfile(profile)
                        }
                    )
                } else {
                    MainApp(viewModel, aiViewModel, backupViewModel)
                }
            }
        }
    }
}

private object BottomNavTuning {
    val barHeight = 60.dp
    val iconSize = 22.dp
    val itemVerticalOffset = (-5).dp
    val iconVerticalOffset = 0.dp
    val iconBottomPadding = 1.dp
    val labelVerticalOffset = 14.dp
    val labelTopPadding = 1.dp
    val labelFontSize = 12.sp
    val labelLineHeight = 12.sp
    const val selectedIndicatorBlendToWhiteLight = 0.05f
    const val selectedIndicatorBlendToBlackDark = 0.05f
    const val selectedIndicatorAlphaLight = 0.8f
    const val selectedIndicatorAlphaDark = 0.8f
}

private object ScreenOffsetTuning {
    val overviewPageOffsetY = 0.dp
    val statsPageOffsetY = 0.dp
    val settingsPageOffsetY = 0.dp
}

private fun Modifier.upwardOffsetWithoutBottomGap(offsetY: Dp): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val offsetPx = offsetY.roundToPx()
        val extraHeight = if (offsetPx < 0) -offsetPx else 0
        val expandedConstraints = constraints.copy(
            minHeight = constraints.minHeight + extraHeight,
            maxHeight = constraints.maxHeight + extraHeight
        )
        val placeable = measurable.measure(expandedConstraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeRelative(0, offsetPx)
        }
    }
)

@Composable
fun MainApp(viewModel: MainViewModel, aiViewModel: AiViewModel, backupViewModel: BackupViewModel) {
    val navController = rememberNavController()
    val userProfile by viewModel.userProfile.collectAsState()
    val selectedThemeIndex = userProfile?.selectedTodayThemeIndex ?: 0
    val shouldForceThemeSelection = userProfile?.hasSelectedTodayTheme == false
    val selectedTheme = remember(selectedThemeIndex) { getTodayVisualTheme(selectedThemeIndex) }
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val accentColor = remember(selectedTheme, isDarkTheme) { themedAccentColor(selectedTheme, isDarkTheme) }
    val navCardColor = remember(selectedTheme, isDarkTheme) { themedDashboardCardColor(selectedTheme, isDarkTheme) }
    val navOnCardColor = com.example.calorietracker.ui.theme.onCardColor(navCardColor, isDarkTheme)
    val selectedIndicatorColor = remember(navCardColor, isDarkTheme) {
        if (isDarkTheme) {
            lerp(navCardColor, Color.Black, BottomNavTuning.selectedIndicatorBlendToBlackDark)
                .copy(alpha = BottomNavTuning.selectedIndicatorAlphaDark)
        } else {
            lerp(navCardColor, Color.White, BottomNavTuning.selectedIndicatorBlendToWhiteLight)
                .copy(alpha = BottomNavTuning.selectedIndicatorAlphaLight)
        }
    }
    val view = LocalView.current
    val context = LocalContext.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkTheme
    }

    // Auto-update check on startup (only when online)
    val currentVersion = "1.5.2"
    val autoRelease by viewModel.autoUpdateRelease.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.checkAutoUpdate(context, currentVersion)
    }

    // Auto-update dialog
    if (autoRelease != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAutoUpdate(context) },
            containerColor = navCardColor,
            titleContentColor = navOnCardColor,
            textContentColor = navOnCardColor,
            title = { Text("发现新版本") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("新版本: ${autoRelease!!.tagName}")
                    Text("当前版本: $currentVersion")
                    if (autoRelease!!.body.isNotBlank()) {
                        Text(
                            autoRelease!!.body.take(300),
                            style = MaterialTheme.typography.bodySmall,
                            color = navOnCardColor.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.openUpdateInBrowser(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Text("去 GitHub 下载")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissAutoUpdate(context) },
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                ) {
                    Text("稍后再说")
                }
            }
        )
    }

    // Nested scroll tracking for status bar blur
    val blurAmount by viewModel.scrollBlurAmount.collectAsState()
    var cumulativeScroll by remember { mutableStateOf(0f) }
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                cumulativeScroll = (cumulativeScroll - available.y).coerceIn(0f, 300f)
                viewModel.updateScrollBlur(cumulativeScroll / 300f)
                return Offset.Zero
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .nestedScroll(scrollConnection)
    ) {
        TodayBackground(
            theme = selectedTheme,
            seed = (selectedThemeIndex + 1) * 1031,
            isDarkTheme = isDarkTheme,
            modifier = Modifier
                .matchParentSize()
                .blur(if (isDarkTheme) 35.dp else 18.dp)
        )
        Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(modifier = Modifier.navigationBarsPadding()) {
                NavigationBar(
                    containerColor = navCardColor.copy(alpha = 0.95f),
                    tonalElevation = 4.dp,
                    modifier = Modifier.height(BottomNavTuning.barHeight),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    
                    val items = listOf(
                        Triple("today", "今日", Icons.Default.Home),
                        Triple("stats", "运动统计", Icons.Default.FitnessCenter),
                        Triple("overview", "日历", Icons.Default.DateRange),
                        Triple("analysis", "分析", Icons.Default.AutoGraph),
                        Triple("settings", "设置", Icons.Default.Settings)
                    )

                    items.forEach { (route, label, icon) ->
                        val selected = currentRoute == route
                        
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.offset(y = BottomNavTuning.itemVerticalOffset),
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = BottomNavTuning.iconBottomPadding)
                                        .offset(y = BottomNavTuning.iconVerticalOffset)
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(BottomNavTuning.iconSize)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = BottomNavTuning.labelFontSize,
                                        lineHeight = BottomNavTuning.labelLineHeight
                                    ),
                                    modifier = Modifier
                                        .padding(top = BottomNavTuning.labelTopPadding)
                                        .offset(y = BottomNavTuning.labelVerticalOffset)
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = navOnCardColor,
                                selectedTextColor = accentColor,
                                indicatorColor = selectedIndicatorColor,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "today",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("today") {
                val dailyRecord by viewModel.dailyRecord.collectAsState()
                val items by viewModel.dailyItems.collectAsState()
                val selectedDate by viewModel.selectedDate.collectAsState()
                val userProfile by viewModel.userProfile.collectAsState()
                val allRecords by viewModel.allRecords.collectAsState()
                val allItems by viewModel.allCalorieItems.collectAsState()

                // Trigger medication reminder check on today page
                LaunchedEffect(Unit) {
                    viewModel.checkMedicationReminder()
                }

                TodayScreen(
                    userProfile = userProfile,
                    dailyRecord = dailyRecord,
                    allRecords = allRecords,
                    items = items,
                    allItems = allItems,
                    selectedDate = selectedDate,
                    onDateChange = { viewModel.setDate(it) },
                    onAddClick = { date -> navController.navigate("add_entry?date=$date") },
                    onDeleteItem = { viewModel.deleteItem(it) },
                    onUpdateItem = { viewModel.updateRecordItem(it) },
                    onUpdateWeight = { viewModel.updateWeight(it, selectedDate) },
                    onUpdateWater = { viewModel.updateWater(it, selectedDate) },
                    onUpdateSleep = { viewModel.updateSleep(it, selectedDate) },
                    onUpdateMedicationTaken = { viewModel.updateMedicationTaken(selectedDate, it) },
                    onSaveExercise = { name, calories, startTime, endTime ->
                        val minutes = com.example.calorietracker.util.CalorieUtils.calculateDuration(startTime, endTime)
                        val notes = if (minutes > 0) "时长: ${minutes}分钟" else ""
                        
                        viewModel.addRecordItem(
                            type = "exercise",
                            name = name,
                            calories = calories.toDouble(),
                            time = startTime,
                            notes = notes,
                            targetDate = selectedDate
                        )
                    }
                )

                // Medication reminder dialog (only show when update dialog is not active)
                if (autoRelease == null) {
                    val medReminder by viewModel.medicationReminder.collectAsState()
                    if (medReminder.show) {
                        MedicationReminderDialog(
                            medicationNames = medReminder.medicationNames,
                            onTakeAll = {
                                viewModel.markMedicationsTaken(selectedDate, medReminder.medicationIndices)
                            },
                            onDismiss = { viewModel.dismissMedicationReminder() },
                            containerColor = navCardColor,
                            textColor = navOnCardColor,
                            accentColor = accentColor
                        )
                    }
                }
            }
            
            composable("stats") {
                val allItems by viewModel.allCalorieItems.collectAsState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .upwardOffsetWithoutBottomGap(ScreenOffsetTuning.statsPageOffsetY)
                ) {
                    StatisticsScreen(
                        allItems = allItems,
                        selectedThemeIndex = selectedThemeIndex
                    )
                }
            }
            
            composable("overview") {
                val allRecords by viewModel.allRecords.collectAsState()
                val allItems by viewModel.allCalorieItems.collectAsState()
                
                val (selectedDate, setSelectedDate) = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
                
                val context = androidx.compose.ui.platform.LocalContext.current
                val app = context.applicationContext as CalorieTrackerApp
                
                val itemsForDialog by androidx.compose.runtime.produceState<List<com.example.calorietracker.data.CalorieItemEntity>>(initialValue = emptyList(), selectedDate) {
                    if (selectedDate != null) {
                        app.repository.getItemsForDate(selectedDate).collect { value = it }
                    } else {
                        value = emptyList()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .upwardOffsetWithoutBottomGap(ScreenOffsetTuning.overviewPageOffsetY)
                ) {
                    OverviewScreen(
                        records = allRecords,
                        allItems = allItems,
                        userProfile = userProfile,
                        onAddRecord = { date ->
                            navController.navigate("add_entry?date=$date")
                        },
                        onUpdateWeight = { weight, date ->
                            viewModel.updateWeight(weight, date)
                        },
                        onUpdateWater = { water, date ->
                            viewModel.updateWater(water, date)
                        },
                        onUpdateSleep = { sleep, date ->
                            viewModel.updateSleep(sleep, date)
                        },
                        detailDate = selectedDate,
                        detailItems = itemsForDialog,
                        onDetailDateChange = setSelectedDate
                    )
                }
            }
            
            composable("analysis") {
                val allRecords by viewModel.allRecords.collectAsState()
                val allItems by viewModel.allCalorieItems.collectAsState()
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .upwardOffsetWithoutBottomGap(ScreenOffsetTuning.settingsPageOffsetY)
                ) {
                    AnalysisScreen(
                        records = allRecords,
                        allItems = allItems,
                        userProfile = userProfile,
                        aiViewModel = aiViewModel,
                        selectedThemeIndex = selectedThemeIndex,
                        onNavigateToDetail = { weekStart ->
                            navController.navigate("analysis_detail/$weekStart")
                        }
                    )
                }
            }

            composable(
                route = "analysis_detail/{weekStart}",
                arguments = listOf(androidx.navigation.navArgument("weekStart") { 
                    nullable = false
                })
            ) { backStackEntry ->
                val weekStart = backStackEntry.arguments?.getString("weekStart") ?: return@composable
                val allRecords by viewModel.allRecords.collectAsState()
                val allItems by viewModel.allCalorieItems.collectAsState()

                AnalysisDetailScreen(
                    weekStartDate = weekStart,
                    records = allRecords,
                    allItems = allItems,
                    userProfile = userProfile,
                    aiViewModel = aiViewModel,
                    selectedThemeIndex = selectedThemeIndex,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                val allItems by viewModel.allCalorieItems.collectAsState()
                val updateStatus by viewModel.updateStatus.collectAsState()
                val context = LocalContext.current
                var iconVersion by remember { mutableStateOf(0L) }
                val hasCustomIcon = remember(iconVersion) { IconManager.hasCustomIcon(context) }

                val imagePicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = inputStream?.use { BitmapFactory.decodeStream(it) }
                        inputStream?.close()
                        if (bitmap != null) {
                            IconManager.saveCustomIcon(context, bitmap)
                            iconVersion++
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .upwardOffsetWithoutBottomGap(ScreenOffsetTuning.settingsPageOffsetY)
                ) {
                    SettingsScreen(
                        userProfile = userProfile,
                        availableExercises = allItems.filter { it.type == "exercise" }.map { it.name }.distinct(),
                        updateStatus = updateStatus,
                        onEditProfile = { navController.navigate("profile_edit") },
                        onBackupSettings = { navController.navigate("backup_settings") },
                        onAiSettings = { navController.navigate("ai_settings") },
                        onSystemPromptSettings = { navController.navigate("system_prompt_settings") },
                        onUpdateSleepGoal = { goal ->
                            userProfile?.let {
                                viewModel.saveProfile(it.copy(sleepGoal = goal))
                            }
                        },
                        onUpdateExcludedExercises = { viewModel.updateExcludedExercises(it) },
                        onUpdateShowMacros = { viewModel.updateShowMacros(it) },
                        onUpdateTodayThemeIndex = { viewModel.updateTodayThemeIndex(it) },
                        onUpdateWeekStartDay = { viewModel.updateWeekStartDay(it) },
                        onUpdateMedicationEnabled = { viewModel.updateMedicationEnabled(it) },
                        onUpdateMedications = { meds, times -> viewModel.updateMedications(meds, times) },
                        onCheckUpdate = { currentVersion -> viewModel.checkForUpdate(currentVersion) },
                        onDismissUpdateDialog = { viewModel.resetUpdateStatus() },
                        onPickAppIcon = { imagePicker.launch("image/*") },
                        onResetAppIcon = {
                            IconManager.deleteCustomIcon(context)
                            iconVersion++
                        },
                        hasCustomIcon = hasCustomIcon
                    )
                }
            }

            composable("ai_settings") {
                ApiSettingsScreen(
                    viewModel = aiViewModel,
                    selectedThemeIndex = selectedThemeIndex,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("system_prompt_settings") {
                SystemPromptSettingsScreen(
                    viewModel = aiViewModel,
                    selectedThemeIndex = selectedThemeIndex,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("backup_settings") {
                BackupSettingsScreen(
                    viewModel = backupViewModel,
                    selectedThemeIndex = selectedThemeIndex,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(
                route = "add_entry?date={date}",
                arguments = listOf(androidx.navigation.navArgument("date") { 
                    nullable = true 
                    defaultValue = null
                })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date")
                val userProfile by viewModel.userProfile.collectAsState()
                AddEntryScreen(
                    targetDate = date ?: com.example.calorietracker.util.CalorieUtils.getTodayString(),
                    aiViewModel = aiViewModel,
                    selectedThemeIndex = selectedThemeIndex,
                    userWeight = userProfile?.weight ?: 70f,
                    showMacros = userProfile?.showMacros ?: false,
                    onSave = { items ->
                        items.forEach { item ->
                            viewModel.addRecordItem(
                                type = item.type,
                                name = item.name,
                                calories = item.calories,
                                carbs = item.carbs,
                                protein = item.protein,
                                fat = item.fat,
                                time = item.time,
                                mealCategory = item.mealCategory,
                                notes = item.notes,
                                imageUrl = item.imagePath,
                                targetDate = date
                            )
                        }
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            
            composable("profile_edit") {
                val userProfile by viewModel.userProfile.collectAsState()
                ProfileSetupScreen(
                    userProfile = userProfile,
                    onSave = { profile ->
                        viewModel.saveProfile(profile)
                        navController.popBackStack()
                    }
                )

            }
        }
    }
        if (shouldForceThemeSelection) {
            TodayThemeDialog(
                currentIndex = selectedThemeIndex,
                onDismiss = {},
                onConfirm = { viewModel.updateTodayThemeIndex(it) },
                containerColor = navCardColor,
                textColor = navOnCardColor,
                accentColor = accentColor
            )
        }

        // Status bar blur overlay (appears on scroll)
        if (blurAmount > 0.01f) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
            val overlayColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFFFFFFF)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarHeight + 8.dp)
                    .then(
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            Modifier.blur((blurAmount * 30).dp)
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                overlayColor.copy(alpha = blurAmount * 0.85f),
                                overlayColor.copy(alpha = blurAmount * 0.5f),
                                overlayColor.copy(alpha = 0f)
                            )
                        )
                    )
            )
        }
    }
}
