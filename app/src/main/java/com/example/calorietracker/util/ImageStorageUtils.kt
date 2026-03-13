package com.example.calorietracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

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

        val file = File(getImageDir(context), "img_${System.currentTimeMillis()}.jpg")
        return try {
            FileOutputStream(file).use { it.write(compressed) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxLongSide: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            while ((bounds.outWidth / sampleSize) > maxLongSide || (bounds.outHeight / sampleSize) > maxLongSide) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) } ?: return null

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
