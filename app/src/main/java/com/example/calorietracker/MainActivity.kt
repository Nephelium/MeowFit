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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
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
    val labelVerticalOffset = 15.dp
    val labelTopPadding = 1.dp
    val labelFontSize = 12.sp
    val labelLineHeight = 12.sp
    const val selectedIndicatorAlphaLight = 0.18f
    const val selectedIndicatorAlphaDark = 0.26f
}

private object ScreenOffsetTuning {
    val overviewPageOffsetY = (-15).dp
    val statsPageOffsetY = (-25).dp
    val settingsPageOffsetY = (-10).dp
}

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
    val navOnCardColor = if (isDarkTheme) Color.White else if (calculatePerceivedLuminance(navCardColor) > 0.5f) Color(0xFF1E1E1E) else Color(0xFFF4F4F4)
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkTheme
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TodayBackground(
            theme = selectedTheme,
            seed = (selectedThemeIndex + 1) * 1031,
            isDarkTheme = isDarkTheme,
            modifier = Modifier
                .matchParentSize()
                .blur(if (isDarkTheme) 22.dp else 10.dp)
        )
        Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
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
                            selectedIconColor = accentColor,
                            selectedTextColor = accentColor,
                            indicatorColor = accentColor.copy(
                                alpha = if (isDarkTheme) BottomNavTuning.selectedIndicatorAlphaDark else BottomNavTuning.selectedIndicatorAlphaLight
                            ),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
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

                TodayScreen(
                    userProfile = userProfile,
                    dailyRecord = dailyRecord,
                    allRecords = allRecords,
                    items = items,
                    selectedDate = selectedDate,
                    onDateChange = { viewModel.setDate(it) },
                    onAddClick = { date -> navController.navigate("add_entry?date=$date") },
                    onDeleteItem = { viewModel.deleteItem(it) },
                    onUpdateItem = { viewModel.updateRecordItem(it) },
                    onUpdateWeight = { viewModel.updateWeight(it, selectedDate) },
                    onUpdateWater = { viewModel.updateWater(it, selectedDate) },
                    onUpdateSleep = { viewModel.updateSleep(it, selectedDate) },
                    onSaveExercise = { name, calories, startTime, endTime ->
                        // Calculate duration
                        var notes = ""
                        try {
                            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            val start = sdf.parse(startTime)
                            val end = sdf.parse(endTime)
                            if (start != null && end != null) {
                                var diff = end.time - start.time
                                if (diff < 0) diff += 24 * 60 * 60 * 1000
                                val minutes = diff / (1000 * 60)
                                notes = "时长: ${minutes}分钟"
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        
                        viewModel.addRecordItem(
                            type = "exercise",
                            name = name,
                            calories = calories,
                            time = startTime,
                            notes = notes,
                            targetDate = selectedDate
                        )
                    }
                )
            }
            
            composable("stats") {
                val allItems by viewModel.allCalorieItems.collectAsState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = ScreenOffsetTuning.statsPageOffsetY)
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
                        .offset(y = ScreenOffsetTuning.overviewPageOffsetY)
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
            
            composable("settings") {
                val allItems by viewModel.allCalorieItems.collectAsState()
                val updateStatus by viewModel.updateStatus.collectAsState()
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = ScreenOffsetTuning.settingsPageOffsetY)
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
                        onCheckUpdate = { currentVersion -> viewModel.checkForUpdate(currentVersion) },
                        onDismissUpdateDialog = { viewModel.resetUpdateStatus() }
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
    }
}
