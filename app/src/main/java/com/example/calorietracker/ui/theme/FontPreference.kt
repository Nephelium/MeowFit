package com.example.calorietracker.ui.theme

import android.content.Context

enum class AppFontMode(val storageValue: String) {
    MEOW_FIT("meow_fit"),
    SYSTEM("system");

    companion object {
        fun fromStorage(value: String?): AppFontMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM // 默认使用系统字体
    }
}

object FontPreferences {
    private const val PREFS_NAME = "ui_preferences"
    private const val KEY_FONT_MODE = "font_mode"

    fun read(context: Context): AppFontMode = AppFontMode.fromStorage(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FONT_MODE, null)
    )

    fun write(context: Context, mode: AppFontMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FONT_MODE, mode.storageValue)
            .apply()
    }
}
