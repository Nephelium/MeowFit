package com.example.calorietracker.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/**
 * 统一的 Bitmap 解码入口：带尺寸采样（防 OOM）与 EXIF 方向校正。
 * 所有页面禁止直接调用 BitmapFactory.decode*，应通过这里解码。
 */
object BitmapUtils {

    /** 计算 2 的幂采样率，保证解码后最长边不超过 [maxLongSide]。 */
    private fun computeSampleSize(width: Int, height: Int, maxLongSide: Int): Int {
        var sample = 1
        val longSide = maxOf(width, height)
        while (longSide / (sample * 2) >= maxLongSide && longSide / (sample * 2) > 0) {
            sample *= 2
        }
        return sample
    }

    private fun decodeStream(
        openStream: () -> java.io.InputStream?,
        maxLongSide: Int
    ): Bitmap? {
        // 1) 读尺寸
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream()?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // 2) 采样解码
        val opts = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, maxLongSide)
        }
        val raw = openStream()?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        // 3) EXIF 方向校正
        val orientation = openStream()?.use { stream ->
            runCatching { ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            ) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        return rotateForExif(raw, orientation)
    }

    fun decodeSampledFromUri(resolver: ContentResolver, uri: Uri, maxLongSide: Int): Bitmap? =
        runCatching {
            decodeStream({ resolver.openInputStream(uri) }, maxLongSide)
        }.getOrNull()

    fun decodeSampledFromPath(path: String, maxLongSide: Int): Bitmap? =
        runCatching {
            val orientation = runCatching {
                ExifInterface(path).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            val opts = BitmapFactory.Options().apply {
                inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, maxLongSide)
            }
            val raw = BitmapFactory.decodeFile(path, opts) ?: return@runCatching null
            rotateForExif(raw, orientation)
        }.getOrNull()

    fun rotateForExif(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setRotate(180f).also { matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> matrix.setRotate(90f).also { matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> matrix.setRotate(-90f).also { matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return source
        }
        return runCatching {
            val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            if (rotated != source) source.recycle()
            rotated
        }.getOrDefault(source)
    }
}
