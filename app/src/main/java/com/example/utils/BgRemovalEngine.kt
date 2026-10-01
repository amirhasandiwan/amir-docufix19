package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object BgRemovalEngine {

    enum class RemovalMode(val label: String) {
        AI_PORTRAIT("AI Auto (Portrait/Person)"),
        COLOR_KEY("Color Detection (Solid/Studio/Document)")
    }

    data class SegmentationResult(
        val mask: FloatArray,
        val maskWidth: Int,
        val maskHeight: Int
    )

    /**
     * Runs ML Kit Selfie Segmenter to generate foreground confidence (0.0 to 1.0) for each pixel.
     */
    suspend fun segmentAiPortrait(bitmap: Bitmap): SegmentationResult = suspendCancellableCoroutine { cont ->
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
        val segmenter = Segmentation.getClient(options)
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        segmenter.process(inputImage)
            .addOnSuccessListener { mask ->
                val width = mask.width
                val height = mask.height
                val floatBuffer: FloatBuffer = mask.buffer.asFloatBuffer()
                val array = FloatArray(width * height)
                floatBuffer.rewind()
                floatBuffer.get(array)
                segmenter.close()
                if (cont.isActive) {
                    cont.resume(SegmentationResult(array, width, height))
                }
            }
            .addOnFailureListener { e ->
                segmenter.close()
                if (cont.isActive) {
                    cont.resumeWithException(e)
                }
            }
    }

    /**
     * Color Keying: Detects background color based on corner sampling or custom color,
     * and produces foreground confidence (0.0 = background, 1.0 = foreground).
     */
    suspend fun segmentByColor(
        bitmap: Bitmap,
        sampleColor: Int? = null,
        tolerance: Float = 0.22f
    ): SegmentationResult = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val targetColor = sampleColor ?: run {
            // Auto-detect by sampling 4 corners
            val c1 = pixels[0]
            val c2 = pixels[width - 1]
            val c3 = pixels[(height - 1) * width]
            val c4 = pixels[width * height - 1]
            val r = (Color.red(c1) + Color.red(c2) + Color.red(c3) + Color.red(c4)) / 4
            val g = (Color.green(c1) + Color.green(c2) + Color.green(c3) + Color.green(c4)) / 4
            val b = (Color.blue(c1) + Color.blue(c2) + Color.blue(c3) + Color.blue(c4)) / 4
            Color.rgb(r, g, b)
        }

        val targetR = Color.red(targetColor)
        val targetG = Color.green(targetColor)
        val targetB = Color.blue(targetColor)

        // Max possible Euclidean distance in RGB is sqrt(255^2 * 3) ~ 441.67
        val maxDist = 441.67f
        val thresholdDist = tolerance * maxDist
        val featherRange = 25f

        val mask = FloatArray(width * height)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)

            val dR = (r - targetR).toFloat()
            val dG = (g - targetG).toFloat()
            val dB = (b - targetB).toFloat()
            val dist = sqrt(dR * dR + dG * dG + dB * dB)

            val confidence = when {
                dist < thresholdDist -> 0.0f
                dist < (thresholdDist + featherRange) -> (dist - thresholdDist) / featherRange
                else -> 1.0f
            }
            mask[i] = confidence
        }

        SegmentationResult(mask, width, height)
    }

    /**
     * Blends the source bitmap with the mask and new background color.
     * If [bgColor] is null, outputs transparent PNG.
     * If [bgColor] is an Int color, replaces background with that solid color.
     */
    suspend fun applyBackground(
        sourceBitmap: Bitmap,
        segmentation: SegmentationResult,
        bgColor: Int?,
        threshold: Float = 0.5f
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        val srcPixels = IntArray(width * height)
        sourceBitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val outPixels = IntArray(width * height)
        val mask = segmentation.mask
        val mWidth = segmentation.maskWidth
        val mHeight = segmentation.maskHeight

        val scaleX = mWidth.toFloat() / width.toFloat()
        val scaleY = mHeight.toFloat() / height.toFloat()

        val bgR = if (bgColor != null) Color.red(bgColor) else 0
        val bgG = if (bgColor != null) Color.green(bgColor) else 0
        val bgB = if (bgColor != null) Color.blue(bgColor) else 0

        for (y in 0 until height) {
            val my = min((y * scaleY).toInt(), mHeight - 1)
            val rowOffset = y * width
            val maskRowOffset = my * mWidth

            for (x in 0 until width) {
                val mx = min((x * scaleX).toInt(), mWidth - 1)
                val rawConfidence = mask[maskRowOffset + mx]

                // Soft transition around threshold
                val alphaFactor = when {
                    rawConfidence < (threshold - 0.15f) -> 0f
                    rawConfidence > (threshold + 0.15f) -> 1f
                    else -> ((rawConfidence - (threshold - 0.15f)) / 0.30f).coerceIn(0f, 1f)
                }

                val srcPixel = srcPixels[rowOffset + x]
                val sR = Color.red(srcPixel)
                val sG = Color.green(srcPixel)
                val sB = Color.blue(srcPixel)
                val sA = Color.alpha(srcPixel)

                if (bgColor == null) {
                    // Transparent output
                    val finalAlpha = (sA * alphaFactor).toInt().coerceIn(0, 255)
                    outPixels[rowOffset + x] = Color.argb(finalAlpha, sR, sG, sB)
                } else {
                    // Solid background color replacement
                    val r = (sR * alphaFactor + bgR * (1f - alphaFactor)).toInt().coerceIn(0, 255)
                    val g = (sG * alphaFactor + bgG * (1f - alphaFactor)).toInt().coerceIn(0, 255)
                    val b = (sB * alphaFactor + bgB * (1f - alphaFactor)).toInt().coerceIn(0, 255)
                    outPixels[rowOffset + x] = Color.argb(255, r, g, b)
                }
            }
        }

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)
        resultBitmap
    }

    /**
     * Saves the processed bitmap to a cached file.
     */
    suspend fun saveBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        isPng: Boolean,
        fileName: String = "bg_removed_${System.currentTimeMillis()}.${if (isPng) "png" else "jpg"}"
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "bg_removed_photos").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            bitmap.compress(format, 100, out)
        }
        file
    }
}
