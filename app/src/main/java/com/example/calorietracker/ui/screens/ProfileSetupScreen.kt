package com.example.calorietracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.calorietracker.data.UserProfileEntity
import com.example.calorietracker.util.CalorieUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    userProfile: UserProfileEntity? = null,
    onSave: (UserProfileEntity) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val selectedThemeIndex = userProfile?.selectedTodayThemeIndex ?: 0
    val selectedTheme = remember(selectedThemeIndex) { getTodayVisualTheme(selectedThemeIndex) }
    val accentColor = remember(selectedTheme, isDarkTheme) { themedAccentColor(selectedTheme, isDarkTheme) }
    var name by remember { mutableStateOf(userProfile?.name ?: "") }
    var gender by remember { mutableStateOf(userProfile?.gender ?: "male") }
    
    // Parse existing birthDate or fallback to age-based estimation or default
    val initialDate = remember(userProfile) {
        if (userProfile?.birthDate?.isNotBlank() == true) {
            userProfile.birthDate
        } else if (userProfile != null && userProfile.age > 0) {
            val estimatedYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - userProfile.age
            "$estimatedYear-01-01"
        } else {
            "2000-01-01"
        }
    }

    val initialParts = initialDate.split("-")
    var birthYear by remember { mutableStateOf(if (initialParts.size == 3) initialParts[0] else "2000") }
    var birthMonth by remember { mutableStateOf(if (initialParts.size == 3) initialParts[1] else "01") }
    var birthDay by remember { mutableStateOf(if (initialParts.size == 3) initialParts[2] else "01") }

    // Calculate age dynamically
    val ageInt = remember(birthYear, birthMonth, birthDay) {
        val y = birthYear.toIntOrNull() ?: 2000
        val m = birthMonth.toIntOrNull() ?: 1
        val d = birthDay.toIntOrNull() ?: 1
        val dateStr = String.format("%04d-%02d-%02d", y, m, d)
        CalorieUtils.calculateAge(dateStr)
    }

    var height by remember { mutableStateOf(userProfile?.height?.toString() ?: "170") }
    var weight by remember { mutableStateOf(userProfile?.weight?.toString() ?: "60") }
    var targetWeight by remember { mutableStateOf(userProfile?.targetWeight?.toString() ?: "55") }
    var activityLevel by remember { mutableStateOf(userProfile?.activityLevel ?: "sedentary") }
    var goal by remember { mutableStateOf(userProfile?.goal ?: "lose") }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置个人资料") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("昵称") },
                modifier = Modifier.fillMaxWidth()
            )

            // Gender
            Text("性别")
            Row {
                RadioButton(
                    selected = gender == "male",
                    onClick = { gender = "male" },
                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                )
                Text("男", modifier = Modifier.padding(top = 12.dp))
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = gender == "female",
                    onClick = { gender = "female" },
                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                )
                Text("女", modifier = Modifier.padding(top = 12.dp))
            }

            // Birth Date Input
            Text("出生日期")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = birthYear,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) birthYear = it },
                    label = { Text("年") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.5f)
                )
                OutlinedTextField(
                    value = birthMonth,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) birthMonth = it },
                    label = { Text("月") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = birthDay,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) birthDay = it },
                    label = { Text("日") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "年龄: $ageInt 岁",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("身高 (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("当前体重 (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = targetWeight,
                onValueChange = { targetWeight = it },
                label = { Text("目标体重 (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Activity Level (Simplified dropdown or radio)
            Text("活动水平", style = MaterialTheme.typography.titleMedium)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionButton(
                    text = "久坐",
                    selected = activityLevel == "sedentary",
                    onClick = { activityLevel = "sedentary" },
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                SelectionButton(
                    text = "适中",
                    selected = activityLevel == "moderate",
                    onClick = { activityLevel = "moderate" },
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                SelectionButton(
                    text = "活跃",
                    selected = activityLevel == "active",
                    onClick = { activityLevel = "active" },
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Text("目标", style = MaterialTheme.typography.titleMedium)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionButton(
                    text = "减重",
                    selected = goal == "lose",
                    onClick = { goal = "lose" },
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                SelectionButton(
                    text = "保持",
                    selected = goal == "maintain",
                    onClick = { goal = "maintain" },
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                SelectionButton(
                    text = "增重",
                    selected = goal == "gain",
                    onClick = { goal = "gain" },
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    val y = birthYear.toIntOrNull() ?: 2000
                    val m = birthMonth.toIntOrNull() ?: 1
                    val d = birthDay.toIntOrNull() ?: 1
                    val dateStr = String.format("%04d-%02d-%02d", y, m, d)

                    val heightFloat = height.toFloatOrNull() ?: 170f
                    val weightFloat = weight.toFloatOrNull() ?: 60f
                    val targetWeightFloat = targetWeight.toFloatOrNull() ?: 55f
                    
                    val dailyTarget = CalorieUtils.calculateDailyTarget(
                        gender, weightFloat, heightFloat, ageInt, activityLevel, goal
                    )

                    val profile = UserProfileEntity(
                        name = name.ifEmpty { "用户" },
                        gender = gender,
                        age = ageInt,
                        birthDate = dateStr,
                        height = heightFloat,
                        weight = weightFloat,
                        targetWeight = targetWeightFloat,
                        activityLevel = activityLevel,
                        goal = goal,
                        dailyCalorieTarget = dailyTarget,
                        sleepGoal = userProfile?.sleepGoal ?: 7.5f,
                        showMacros = userProfile?.showMacros ?: false,
                        selectedTodayThemeIndex = userProfile?.selectedTodayThemeIndex ?: 0,
                        hasSelectedTodayTheme = userProfile?.hasSelectedTodayTheme ?: false,
                        excludedExercises = userProfile?.excludedExercises ?: "",
                        createdAt = userProfile?.createdAt ?: java.util.Date().toString()
                    )
                    onSave(profile)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                Text("保存并开始")
            }
        }
    }
}

@Composable
fun SelectionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) accentColor else Color(0xFFE0E0E0),
            contentColor = if (selected) Color.White else Color.Black
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (selected) 4.dp else 0.dp
        )
    ) {
        Text(text)
    }
}
