package com.example.calorietracker.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object AppImageStore {
    private const val ROOT_DIR = "images"
    private const val CHAT_DIR = "chat"

    fun chatDirectory(context: Context): File =
        File(File(context.filesDir, ROOT_DIR), CHAT_DIR).apply { mkdirs() }

    suspend fun decodeForAi(
        context: Context,
        uris: List<Uri>,
        maxCount: Int = 6,
        maxLongSide: Int = 1600
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        uris.take(maxCount).mapNotNull { decodeScaledBitmap(context, it, maxLongSide) }
    }

    suspend fun saveChatBitmap(context: Context, bitmap: Bitmap): String? =
        withContext(Dispatchers.IO) {
            val scaled = scaleToLongSide(bitmap, 1280)
            val file = File(chatDirectory(context), "chat_${UUID.randomUUID()}.jpg")
            runCatching {
                FileOutputStream(file).use { output ->
                    check(scaled.compress(Bitmap.CompressFormat.JPEG, 82, output))
                }
                Uri.fromFile(file).toString()
            }.getOrElse {
                file.delete()
                null
            }
        }

    suspend fun deleteChatImages(context: Context, imageUrls: Collection<String?>) = withContext(Dispatchers.IO) {
        val root = chatDirectory(context)
        imageUrls.filterNotNull()
            .flatMap { it.split('|') }
            .mapNotNull(::fileFromStoredUri)
            .filter { file -> isChildOf(file, root) }
            .forEach { it.delete() }
    }

    suspend fun clearChatImages(context: Context) = withContext(Dispatchers.IO) {
        chatDirectory(context).listFiles()?.forEach { file ->
            if (file.isFile) file.delete()
        }
    }

    // BitmapUtils 负责采样（防 OOM）与 EXIF 旋转，这里再把最长边严格压到 maxLongSide 以内
    private fun decodeScaledBitmap(context: Context, uri: Uri, maxLongSide: Int): Bitmap? =
        BitmapUtils.decodeSampledFromUri(context.contentResolver, uri, maxLongSide)
            ?.let { scaleToLongSide(it, maxLongSide) }

    private fun scaleToLongSide(bitmap: Bitmap, maxLongSide: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxLongSide) return bitmap
        val scale = maxLongSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun fileFromStoredUri(value: String): File? = runCatching {
        val uri = Uri.parse(value)
        when (uri.scheme) {
            "file" -> uri.path?.let(::File)
            null -> File(value)
            else -> null
        }
    }.getOrNull()

    private fun isChildOf(file: File, directory: File): Boolean = runCatching {
        val parent = directory.canonicalPath + File.separator
        file.canonicalPath.startsWith(parent)
    }.getOrDefault(false)
}
