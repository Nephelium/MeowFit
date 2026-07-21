package com.example.calorietracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object IconManager {
    private const val CUSTOM_ICON_DIR = "icons"
    private const val CUSTOM_ICON_NAME = "custom_app_icon.png"

    fun getCustomIconFile(context: Context): File {
        val dir = File(context.filesDir, CUSTOM_ICON_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, CUSTOM_ICON_NAME)
    }

    fun hasCustomIcon(context: Context): Boolean {
        return getCustomIconFile(context).exists()
    }

    fun saveCustomIcon(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val file = getCustomIconFile(context)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveCustomIcon(context: Context, uri: Uri, maxSide: Int = 512): Boolean {
        return runCatching {
            // BitmapUtils 负责采样与 EXIF 旋转校正
            val decoded = BitmapUtils.decodeSampledFromUri(context.contentResolver, uri, maxSide) ?: return false
            val scale = maxSide.toFloat() / maxOf(decoded.width, decoded.height)
            val output = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else decoded
            saveCustomIcon(context, output)
        }.getOrDefault(false)
    }

    fun loadIconBitmap(context: Context, size: Int): Bitmap? {
        return try {
            val file = getCustomIconFile(context)
            val source: Bitmap? = if (file.exists()) {
                // 走 BitmapUtils：带采样（防 OOM）与 EXIF 旋转校正
                BitmapUtils.decodeSampledFromPath(file.absolutePath, size)
            } else {
                val iconId = context.resources.getIdentifier(
                    "app_icon", "drawable", context.packageName
                )
                if (iconId != 0) {
                    BitmapFactory.decodeResource(context.resources, iconId)
                } else null
            }
            source?.let {
                Bitmap.createScaledBitmap(it, size, size, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteCustomIcon(context: Context): Boolean {
        return try {
            val file = getCustomIconFile(context)
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
