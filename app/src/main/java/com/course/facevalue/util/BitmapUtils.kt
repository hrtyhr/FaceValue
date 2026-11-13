package com.course.facevalue.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object BitmapUtils {

    private const val TAG = "FaceValueBitmap"

    /** 从相册 Uri 解码并按 EXIF 方向摆正，最长边不超过 maxDimension。 */
    fun loadFromUri(context: Context, uri: Uri, maxDimension: Int = 1800): Bitmap {
        val resolver = context.contentResolver
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            Log.d(TAG, "loadFromUri uri=$uri bounds=${bounds.outWidth}x${bounds.outHeight}")
            if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
                if (decoded != null) {
                    val orientation = readOrientation(resolver, uri)
                    return finish(decoded, orientation, maxDimension)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "BitmapFactory 解码失败，尝试 ImageDecoder", t)
        }

        // 部分照片选择器 / HEIC / 云相册 Uri 无法用 BitmapFactory 预解码，走系统解码器兜底。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(resolver, uri)
                return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val longest = max(info.size.width, info.size.height)
                    if (longest > maxDimension) {
                        val scale = maxDimension.toFloat() / longest
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (t: Throwable) {
                Log.w(TAG, "ImageDecoder 解码失败", t)
            }
        }

        // 最后兜底：整图解码后再缩放。
        val full = try {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (t: Throwable) {
            Log.w(TAG, "整图解码失败", t)
            null
        } ?: throw IllegalArgumentException("无法读取所选图片")
        return finish(full, readOrientation(resolver, uri), maxDimension)
    }

    private fun readOrientation(resolver: android.content.ContentResolver, uri: Uri): Int = try {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    fun loadFromFile(path: String, maxDimension: Int = 1800): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IllegalArgumentException("无法读取照片")
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(path, opts) ?: throw IllegalArgumentException("无法解码照片")
        val orientation = try {
            ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        return finish(decoded, orientation, maxDimension)
    }

    private fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        var longest = max(width, height)
        while (longest / sample > maxDimension * 2) sample *= 2
        return sample
    }

    private fun finish(source: Bitmap, orientation: Int, maxDimension: Int): Bitmap {
        var bitmap = applyOrientation(source, orientation)
        val longest = max(bitmap.width, bitmap.height)
        if (longest > maxDimension) {
            val scale = maxDimension.toFloat() / longest
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (scaled !== bitmap) bitmap.recycle()
            bitmap = scaled
        }
        return bitmap
    }

    private fun applyOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return source
        }
        return try {
            val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            if (rotated !== source) source.recycle()
            rotated
        } catch (e: OutOfMemoryError) {
            source
        }
    }

    fun loadFromStream(input: () -> InputStream, maxDimension: Int = 1800): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        input().use { BitmapFactory.decodeStream(it, null, bounds) }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = input().use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalArgumentException("无法解码图片")
        return finish(decoded, ExifInterface.ORIENTATION_NORMAL, maxDimension)
    }

    /** 保存历史缩略图，返回文件。 */
    fun saveThumbnail(context: Context, bitmap: Bitmap, id: String, maxDimension: Int = 480): File? {
        return try {
            val scale = maxDimension.toFloat() / max(bitmap.width, bitmap.height)
            val thumb = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else bitmap
            val dir = File(context.filesDir, "thumbs").apply { mkdirs() }
            val file = File(dir, "$id.jpg")
            FileOutputStream(file).use { out -> thumb.compress(Bitmap.CompressFormat.JPEG, 88, out) }
            if (thumb !== bitmap) thumb.recycle()
            file
        } catch (e: Exception) {
            null
        }
    }
}
