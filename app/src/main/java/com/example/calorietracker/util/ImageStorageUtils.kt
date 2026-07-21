package com.example.calorietracker.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageUtils {
    private const val IMAGE_DIR_NAME = "record_images"

    fun getImageDir(context: Context): File {
        val dir = File(context.filesDir, IMAGE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun compressAndSaveImage(
        context: Context,
        uri: Uri,
        maxLongSide: Int = 720,
        maxBytes: Int = 48 * 1024
    ): String? {
        val bitmap = decodeScaledBitmap(context, uri, maxLongSide) ?: return null
        return saveCompressedBitmap(context, bitmap, maxBytes)
    }

    fun saveCompressedBitmap(
        context: Context,
        bitmap: Bitmap,
        maxBytes: Int = 48 * 1024
    ): String? {
        var quality = 82
        var compressed = compress(bitmap, quality)
        while (compressed.size > maxBytes && quality > 42) {
            quality -= 6
            compressed = compress(bitmap, quality)
        }

        // 拼 UUID 防止同一毫秒内两张图互相覆盖
        val file = File(getImageDir(context), "img_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
        return try {
            FileOutputStream(file).use { it.write(compressed) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun deleteRecordImage(context: Context, path: String?): Boolean {
        if (path.isNullOrBlank()) return true
        return runCatching {
            val root = getImageDir(context).canonicalPath + File.separator
            val target = File(path)
            if (!target.canonicalPath.startsWith(root)) return false
            !target.exists() || target.delete()
        }.getOrDefault(false)
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxLongSide: Int): Bitmap? {
        return try {
            // BitmapUtils 负责采样与 EXIF 旋转校正，这里再把最长边严格压到 maxLongSide 以内
            val decoded = BitmapUtils.decodeSampledFromUri(context.contentResolver, uri, maxLongSide) ?: return null
            val width = decoded.width
            val height = decoded.height
            val scale = if (width >= height) maxLongSide.toFloat() / width else maxLongSide.toFloat() / height
            if (scale >= 1f) decoded else Bitmap.createScaledBitmap(decoded, (width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), true)
        } catch (e: Exception) {
            null
        }
    }

    private fun compress(bitmap: Bitmap, quality: Int): ByteArray {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return output.toByteArray()
    }
}
