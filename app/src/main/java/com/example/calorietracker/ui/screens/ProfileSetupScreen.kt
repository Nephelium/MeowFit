package com.example.calorietracker.ui.screens

import androidx.compose.foundation.layout.*
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
    var name by remember { mutableStateOf(userProfile?.name ?: "") }
    var gender by remember { mutableStateOf(userProfile?.gender ?: "male") }
    var age by remember { mutableStateOf(userProfile?.age?.toString() ?: "25") }
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
                RadioButton(selected = gender == "male", onClick = { gender = "male" })
                Text("男", modifier = Modifier.padding(top = 12.dp))
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(selected = gender == "female", onClick = { gender = "female" })
                Text("女", modifier = Modifier.padding(top = 12.dp))
            }

            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("年龄") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.weight(1f)
                )
                SelectionButton(
                    text = "适中",
                    selected = activityLevel == "moderate",
                    onClick = { activityLevel = "moderate" },
                    modifier = Modifier.weight(1f)
                )
                SelectionButton(
                    text = "活跃",
                    selected = activityLevel == "active",
                    onClick = { activityLevel = "active" },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("目标", style = MaterialTheme.typography.titleMedium)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionButton(
                    text = "减重",
                    selected = goal == "lose",
                    onClick = { goal = "lose" },
                    modifier = Modifier.weight(1f)
                )
                SelectionButton(
                    text = "保持",
                    selected = goal == "maintain",
                    onClick = { goal = "maintain" },
                    modifier = Modifier.weight(1f)
                )
                SelectionButton(
                    text = "增重",
                    selected = goal == "gain",
                    onClick = { goal = "gain" },
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    val ageInt = age.toIntOrNull() ?: 25
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
                        height = heightFloat,
                        weight = weightFloat,
                        targetWeight = targetWeightFloat,
                        activityLevel = activityLevel,
                        goal = goal,
                        dailyCalorieTarget = dailyTarget,
                        sleepGoal = userProfile?.sleepGoal ?: 7.5f,
                        createdAt = userProfile?.createdAt ?: java.util.Date().toString()
                    )
                    onSave(profile)
                },
                modifier = Modifier.fillMaxWidth()
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
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else Color.Black
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (selected) 4.dp else 0.dp
        )
    ) {
        Text(text)
    }
}
