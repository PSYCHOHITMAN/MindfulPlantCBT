package com.mindfulplant.cbt.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Turns whatever photo the user picked into a small square JPEG that is cheap
 * to store and upload (about 512 x 512 px, normally well under 200 KB).
 *
 * Steps: decode at reduced size (so a 12 MP camera photo doesn't use much
 * memory), fix the rotation from the photo's EXIF data, crop to a centred
 * square, scale down, and compress.
 */
object ImageProcessor {

    const val MAX_SIZE = 512
    private const val JPEG_QUALITY = 85
    private const val TAG = "ImageProcessor"

    /** Returns JPEG bytes, or null if the picked file can't be read as an image. */
    fun toAvatarJpeg(context: Context, uri: Uri): ByteArray? = runCatching {
        val resolver = context.contentResolver

        // Read only the image size. With inJustDecodeBounds the decoder returns null
        // on purpose, so we must not treat that null as a failure.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val opened = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
            true
        } ?: run {
            Log.e(TAG, "Could not open $uri")
            return null
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIZE) sample *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
        }

        // Centred square crop (the matrix is applied to the cropped square).
        val side = min(decoded.width, decoded.height)
        val square = Bitmap.createBitmap(
            decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side, matrix, true
        )
        val scaled = if (square.width > MAX_SIZE) {
            Bitmap.createScaledBitmap(square, MAX_SIZE, MAX_SIZE, true)
        } else square

        // JPEG has no transparency, so put PNGs on white instead of black.
        val flattened = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        Canvas(flattened).apply {
            drawColor(Color.WHITE)
            drawBitmap(scaled, 0f, 0f, null)
        }

        ByteArrayOutputStream().use { out ->
            flattened.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }.onFailure { Log.e(TAG, "Could not process the picked image", it) }.getOrNull()
}