package com.example.calorietracker.data.backup

import com.example.calorietracker.data.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupUserProfileTest {

    @Test
    fun oldBackupKeepsNewMedicationFieldsFromExistingProfile() {
        val existing = profile(
            medicationEnabled = true,
            medications = "甲药,乙药",
            medicationTimes = "08:00,20:00"
        )
        val oldBackup = BackupUserProfile(
            name = "旧备份名称",
            weight = 61f,
            medicationTimes = null
        )

        val restored = oldBackup.toEntity(existing)

        assertEquals("旧备份名称", restored.name)
        assertEquals(61f, restored.weight, 0f)
        assertTrue(restored.medicationEnabled)
        assertEquals("甲药,乙药", restored.medications)
        assertEquals("08:00,20:00", restored.medicationTimes)
    }

    @Test
    fun newBackupCanExplicitlyReplaceMedicationTimes() {
        val restored = BackupUserProfile(medicationTimes = "07:30").toEntity(profile())
        assertEquals("07:30", restored.medicationTimes)
    }

    private fun profile(
        medicationEnabled: Boolean = false,
        medications: String = "",
        medicationTimes: String = ""
    ) = UserProfileEntity(
        name = "User",
        gender = "female",
        age = 25,
        birthDate = "2001-01-01",
        height = 165f,
        weight = 60f,
        targetWeight = 55f,
        activityLevel = "sedentary",
        goal = "lose",
        dailyCalorieTarget = 1800,
        medicationEnabled = medicationEnabled,
        medications = medications,
        medicationTimes = medicationTimes,
        createdAt = "now"
    )
}
