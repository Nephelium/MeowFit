package com.example.calorietracker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CutCornerShape as RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import com.example.calorietracker.ui.theme.AppFontMode
import com.example.calorietracker.ui.theme.FontPreferences
import com.example.calorietracker.ui.theme.CalendarDisplayPreferences
import com.example.calorietracker.domain.CalendarMetricId
import com.example.calorietracker.ui.AiViewModel
import com.example.calorietracker.ui.components.PixelBackdrop
import com.example.calorietracker.ui.components.PixelNavDestination
import com.example.calorietracker.ui.components.PixelNavIcon
import com.example.calorietracker.util.IconManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val context = LocalContext.current
            var fontMode by remember { mutableStateOf(FontPreferences.read(context)) }
            var visibleCalendarMetricIds by remember {
                mutableStateOf(CalendarDisplayPreferences.read(context))
            }
            CalorieTrackerTheme(fontMode = fontMode) {
                val userProfile by viewModel.userProfile.collectAsState()
                val isProfileLoaded by viewModel.isProfileLoaded.collectAsState()

                if (!isProfileLoaded) {
                    // Wait for the first DB emission to avoid flashing the setup screen
                    PixelBackdrop(modifier = Modifier.fillMaxSize()) {}
                } else if (userProfile == null) {
                    // If no profile, force setup first (simplified flow)
                    ProfileSetupScreen(
                        onSave = { profile ->
                            viewModel.saveProfile(profile)
                        }
                    )
                } else {
                    MainApp(
                        viewModel = viewModel,
                        aiViewModel = aiViewModel,
                        backupViewModel = backupViewModel,
                        fontMode = fontMode,
                        onFontModeChange = { mode ->
                            FontPreferences.write(context, mode)
                            fontMode = mode
                        },
                        visibleCalendarMetricIds = visibleCalendarMetricIds,
                        onVisibleCalendarMetricsChange = { metricIds ->
                            CalendarDisplayPreferences.write(context, metricIds)
                            visibleCalendarMetricIds = CalendarDisplayPreferences.read(context)
                        }
                    )
                }
            }
        }
    }
}

private object BottomNavTuning {
    val barHeight = 64.dp
    val iconSize = 36.dp
    val itemVerticalOffset = 0.dp
    val iconVerticalOffset = 0.dp
    val iconBottomPadding = 0.dp
    val labelVerticalOffset = 0.dp
    val labelTopPadding = 0.dp
    val labelFontSize = 11.sp
    val labelLineHeight = 14.sp
    const val selectedIndicatorBlendToWhiteLight = 0.05f
    const val selectedIndicatorBlendToBlackDark = 0.05f
    const val selectedIndicatorAlphaLight = 0.18f
    const val selectedIndicatorAlphaDark = 0.24f
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
fun MainApp(
    viewModel: MainViewModel,
    aiViewModel: AiViewModel,
    backupViewModel: BackupViewModel,
    fontMode: AppFontMode,
    onFontModeChange: (AppFontMode) -> Unit,
    visibleCalendarMetricIds: Set<CalendarMetricId>,
    onVisibleCalendarMetricsChange: (Set<CalendarMetricId>) -> Unit
) {
    val navController = rememberNavController()
    val userProfile by viewModel.userProfile.collectAsState()
    val selectedThemeIndex = userProfile?.selectedTodayThemeIndex ?: 0
    val shouldForceThemeSelection = userProfile?.hasSelectedTodayTheme == false
    val selectedTheme = remember(selectedThemeIndex) { getTodayVisualTheme(selectedThemeIndex) }
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val accentColor = remember(selectedTheme, isDarkTheme) { themedAccentColor(selectedTheme, isDarkTheme) }
    val navCardColor = remember(selectedTheme, isDarkTheme) { themedDashboardCardColor(selectedTheme, isDarkTheme) }
    val navBackgroundColor = remember(selectedTheme, isDarkTheme) {
        Color(if (isDarkTheme) selectedTheme.darkBgColor else selectedTheme.lightBgColor)
    }
    val navOnCardColor = com.example.calorietracker.ui.theme.onCardColor(navCardColor, isDarkTheme)
    val selectedIndicatorColor = remember(accentColor, isDarkTheme) {
        accentColor.copy(
            alpha = if (isDarkTheme) BottomNavTuning.selectedIndicatorAlphaDark
            else BottomNavTuning.selectedIndicatorAlphaLight
        )
    }
    val context = LocalContext.current

    // Auto-update check on startup (only when online)
    val currentVersion = BuildConfig.VERSION_NAME
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
                    if (autoRelease?.body.orEmpty().isNotBlank()) {
                        Text(
                            autoRelease?.body.orEmpty().take(300),
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
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val topLevelRoutes = remember { setOf("today", "stats", "overview", "analysis", "settings") }
        val showBottomNavigation = currentRoute in topLevelRoutes

        PixelBackdrop(modifier = Modifier.matchParentSize()) {}
        Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomNavigation) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(navBackgroundColor)
                        .navigationBarsPadding()
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(accentColor.copy(alpha = 0.18f))
                        )

                        val items = listOf(
                            Triple("today", "今日", PixelNavDestination.TODAY),
                            Triple("stats", "运动统计", PixelNavDestination.STATS),
                            Triple("overview", "日历", PixelNavDestination.CALENDAR),
                            Triple("analysis", "分析", PixelNavDestination.ANALYSIS),
                            Triple("settings", "设置", PixelNavDestination.SETTINGS)
                        )

                        Row(modifier = Modifier.fillMaxWidth().height(BottomNavTuning.barHeight)) {
                            items.forEach { (route, label, destination) ->
                                val selected = currentRoute == route
                                val itemColor = if (selected) accentColor else navOnCardColor.copy(alpha = 0.56f)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .semantics {
                                            contentDescription = label
                                            this.selected = selected
                                        }
                                        .clickable {
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .offset(y = BottomNavTuning.itemVerticalOffset)
                                            .width(46.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (selected) selectedIndicatorColor else Color.Transparent)
                                            .padding(top = 5.dp, bottom = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        PixelNavIcon(
                                            destination = destination,
                                            color = itemColor,
                                            selected = selected,
                                            modifier = Modifier
                                                .offset(y = BottomNavTuning.iconVerticalOffset)
                                                .padding(bottom = BottomNavTuning.iconBottomPadding)
                                                .size(BottomNavTuning.iconSize)
                                        )
                                        Text(
                                            text = label,
                                            fontSize = BottomNavTuning.labelFontSize,
                                            lineHeight = BottomNavTuning.labelLineHeight,
                                            color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            modifier = Modifier
                                                .offset(y = BottomNavTuning.labelVerticalOffset)
                                                .padding(top = BottomNavTuning.labelTopPadding)
                                        )
                                    }
                                }
                            }
                        }
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
                        selectedThemeIndex = selectedThemeIndex,
                        weekStartDay = userProfile?.weekStartDay ?: java.util.Calendar.SUNDAY
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
                        visibleMetricIds = visibleCalendarMetricIds,
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
                val iconScope = rememberCoroutineScope()
                var iconVersion by remember { mutableStateOf(0L) }
                val hasCustomIcon = remember(iconVersion) { IconManager.hasCustomIcon(context) }

                val imagePicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        iconScope.launch {
                            val saved = withContext(Dispatchers.IO) {
                                IconManager.saveCustomIcon(context, uri)
                            }
                            if (saved) iconVersion++
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
                        hasCustomIcon = hasCustomIcon,
                        fontMode = fontMode,
                        onFontModeChange = onFontModeChange,
                        visibleCalendarMetricIds = visibleCalendarMetricIds,
                        onVisibleCalendarMetricsChange = onVisibleCalendarMetricsChange
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
                val foodTemplates by viewModel.foodTemplates.collectAsState()
                AddEntryScreen(
                    targetDate = date ?: com.example.calorietracker.util.CalorieUtils.getTodayString(),
                    aiViewModel = aiViewModel,
                    selectedThemeIndex = selectedThemeIndex,
                    userWeight = userProfile?.weight ?: 70f,
                    showMacros = userProfile?.showMacros ?: false,
                    foodTemplates = foodTemplates,
                    onSaveFoodTemplate = viewModel::saveFoodTemplate,
                    onDeleteFoodTemplate = viewModel::deleteFoodTemplate,
                    onSave = { items ->
                        viewModel.addRecordItems(
                            drafts = items.map { item ->
                                com.example.calorietracker.data.RecordItemDraft(
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
                                nutritionReferenceAmount = item.nutritionReferenceAmount,
                                nutritionActualAmount = item.nutritionActualAmount,
                                nutritionAmountUnit = item.nutritionAmountUnit,
                                nutritionReferenceEnergy = item.nutritionReferenceEnergy,
                                nutritionEnergyUnit = item.nutritionEnergyUnit,
                                nutritionReferenceCarbs = item.nutritionReferenceCarbs,
                                nutritionReferenceProtein = item.nutritionReferenceProtein,
                                nutritionReferenceFat = item.nutritionReferenceFat
                                )
                            },
                            targetDate = date ?: com.example.calorietracker.util.CalorieUtils.getTodayString()
                            )
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
