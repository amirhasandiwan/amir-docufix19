package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat

object ImageEngine {

    enum class OutputFormat(val extension: String, val compressFormat: Bitmap.CompressFormat) {
        JPEG("jpg", Bitmap.CompressFormat.JPEG),
        WEBP("webp", Bitmap.CompressFormat.WEBP),
        PNG("png", Bitmap.CompressFormat.PNG)
    }

    suspend fun decodeBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                // Decode bounds first
                var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                val origWidth = options.outWidth
                val origHeight = options.outHeight
                if (origWidth <= 0 || origHeight <= 0) return@withContext null

                var inSampleSize = 1
                if (origWidth > maxDimension || origHeight > maxDimension) {
                    val halfWidth = origWidth / 2
                    val halfHeight = origHeight / 2
                    while ((halfWidth / inSampleSize) >= maxDimension && (halfHeight / inSampleSize) >= maxDimension) {
                        inSampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }

                inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                inputStream?.close()
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun scaleBitmap(bitmap: Bitmap, scaleRatio: Float): Bitmap {
        if (scaleRatio >= 0.99f && scaleRatio <= 1.01f) return bitmap
        val targetWidth = (bitmap.width * scaleRatio).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scaleRatio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    suspend fun compressImage(
        context: Context,
        sourceBitmap: Bitmap,
        quality: Int,
        scaleRatio: Float = 1.0f,
        format: OutputFormat = OutputFormat.JPEG,
        outputFileName: String = "compressed_${System.currentTimeMillis()}.${format.extension}"
    ): File = withContext(Dispatchers.IO) {
        val scaled = scaleBitmap(sourceBitmap, scaleRatio)
        val outputDir = File(context.cacheDir, "compressed_images").apply { mkdirs() }
        val outputFile = File(outputDir, outputFileName)

        FileOutputStream(outputFile).use { out ->
            scaled.compress(format.compressFormat, quality.coerceIn(1, 100), out)
        }
        outputFile
    }

    /**
     * Adaptively compresses and scales image to meet any custom target size (e.g. 10 KB, 500 KB, 1 MB, 10 MB)
     */
    suspend fun compressToTargetSize(
        context: Context,
        sourceBitmap: Bitmap,
        targetSizeBytes: Long,
        format: OutputFormat = OutputFormat.JPEG
    ): Pair<File, Int> = withContext(Dispatchers.IO) {
        val targetBytes = targetSizeBytes.coerceAtLeast(1024L)
        var currentBitmap = sourceBitmap
        var currentScale = 1.0f

        // Initial test compression at quality 70
        var testStream = ByteArrayOutputStream()
        currentBitmap.compress(format.compressFormat, 70, testStream)
        var testBytes = testStream.toByteArray()

        // If file is larger than target, iteratively adjust scale
        var attempts = 0
        while (testBytes.size > targetBytes && attempts < 5) {
            val ratio = Math.sqrt(targetBytes.toDouble() / testBytes.size.toDouble()).toFloat()
            val newScale = (currentScale * ratio * 0.95f).coerceIn(0.08f, 1.0f)
            if (Math.abs(newScale - currentScale) < 0.03f) break
            currentScale = newScale
            val targetW = (sourceBitmap.width * currentScale).toInt().coerceAtLeast(10)
            val targetH = (sourceBitmap.height * currentScale).toInt().coerceAtLeast(10)
            if (currentBitmap != sourceBitmap) currentBitmap.recycle()
            currentBitmap = Bitmap.createScaledBitmap(sourceBitmap, targetW, targetH, true)

            testStream = ByteArrayOutputStream()
            currentBitmap.compress(format.compressFormat, 50, testStream)
            testBytes = testStream.toByteArray()
            attempts++
        }

        // Binary search on quality (10 to 95)
        var lowQuality = 10
        var highQuality = 95
        var bestQuality = 75
        var bestBytes: ByteArray? = null

        for (i in 0..5) {
            val midQuality = (lowQuality + highQuality) / 2
            val stream = ByteArrayOutputStream()
            currentBitmap.compress(format.compressFormat, midQuality, stream)
            val bytes = stream.toByteArray()

            if (bytes.size <= targetBytes) {
                bestQuality = midQuality
                bestBytes = bytes
                lowQuality = midQuality + 1
            } else {
                highQuality = midQuality - 1
            }
        }

        if (bestBytes == null) {
            // Even at lowest quality it exceeds, resize down one more step
            val finalW = (currentBitmap.width * 0.75f).toInt().coerceAtLeast(10)
            val finalH = (currentBitmap.height * 0.75f).toInt().coerceAtLeast(10)
            val scaled = Bitmap.createScaledBitmap(currentBitmap, finalW, finalH, true)
            val stream = ByteArrayOutputStream()
            scaled.compress(format.compressFormat, 25, stream)
            bestBytes = stream.toByteArray()
            bestQuality = 25
            scaled.recycle()
        }

        if (currentBitmap != sourceBitmap) {
            currentBitmap.recycle()
        }

        val outputDir = File(context.cacheDir, "compressed_images").apply { mkdirs() }
        val outputFile = File(outputDir, "target_${System.currentTimeMillis()}.${format.extension}")
        outputFile.writeBytes(bestBytes)
        Pair(outputFile, bestQuality)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[digitGroups]
    }

    /**
     * Enhances image resolution, edge sharpness, and tonal clarity.
     * Can also guarantee a target minimum file size for portal submissions.
     */
    suspend fun enhanceImage(
        context: Context,
        sourceBitmap: Bitmap,
        scaleFactor: Float = 2.0f,
        sharpnessStrength: Float = 0.5f,
        contrastBoost: Float = 1.15f,
        brightnessBoost: Float = 5f,
        targetMinSizeBytes: Long = 0L,
        format: OutputFormat = OutputFormat.JPEG,
        quality: Int = 95,
        outputFileName: String = "enhanced_${System.currentTimeMillis()}.${format.extension}"
    ): Pair<File, Bitmap> = withContext(Dispatchers.IO) {
        // 1. High-fidelity upscaling with bilinear filtering
        val targetWidth = (sourceBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
        val targetHeight = (sourceBitmap.height * scaleFactor).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(sourceBitmap, targetWidth, targetHeight, true)

        // 2. Adjust contrast, vibrance, and brightness for crisp document text or vivid photos
        val colorAdjusted = adjustColorEnhance(scaled, contrastBoost, brightnessBoost)

        // 3. Apply detail sharpness convolution
        val sharpened = if (sharpnessStrength > 0.05f) {
            sharpenBitmap(colorAdjusted, sharpnessStrength)
        } else {
            colorAdjusted
        }

        // 4. Encode to memory
        val stream = ByteArrayOutputStream()
        sharpened.compress(format.compressFormat, quality.coerceIn(1, 100), stream)
        var encodedBytes = stream.toByteArray()

        // 5. If user needs to meet or exceed a minimum target size (e.g. visa / exam portal requirement)
        if (targetMinSizeBytes > 0 && encodedBytes.size < targetMinSizeBytes) {
            val paddingNeeded = (targetMinSizeBytes - encodedBytes.size).toInt()
            encodedBytes = padImageBytes(encodedBytes, paddingNeeded, format)
        }

        val outputDir = File(context.cacheDir, "enhanced_files").apply { mkdirs() }
        val outputFile = File(outputDir, outputFileName)
        outputFile.writeBytes(encodedBytes)

        Pair(outputFile, sharpened)
    }

    /**
     * Adjusts contrast and brightness using ColorMatrix
     */
    fun adjustColorEnhance(src: Bitmap, contrast: Float, brightness: Float): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Contrast formula: scale rgb and offset
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    /**
     * Fast 3x3 unsharp mask kernel for edge crisping and text clarity
     */
    fun sharpenBitmap(src: Bitmap, strength: Float): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val factor = strength.coerceIn(0.1f, 2.0f)
        val center = 1f + 4f * factor
        val edge = -factor

        // Apply 3x3 kernel on interior
        for (y in 1 until height - 1) {
            val yOffset = y * width
            for (x in 1 until width - 1) {
                val idx = yOffset + x

                // Central pixel and 4-neighbors
                val c = pixels[idx]
                val top = pixels[idx - width]
                val bottom = pixels[idx + width]
                val left = pixels[idx - 1]
                val right = pixels[idx + 1]

                val a = (c ushr 24) and 0xFF

                val rC = (c ushr 16) and 0xFF
                val rT = (top ushr 16) and 0xFF
                val rB = (bottom ushr 16) and 0xFF
                val rL = (left ushr 16) and 0xFF
                val rR = (right ushr 16) and 0xFF
                val newR = (rC * center + (rT + rB + rL + rR) * edge).toInt().coerceIn(0, 255)

                val gC = (c ushr 8) and 0xFF
                val gT = (top ushr 8) and 0xFF
                val gB = (bottom ushr 8) and 0xFF
                val gL = (left ushr 8) and 0xFF
                val gR = (right ushr 8) and 0xFF
                val newG = (gC * center + (gT + gB + gL + gR) * edge).toInt().coerceIn(0, 255)

                val bC = c and 0xFF
                val bT = top and 0xFF
                val bB = bottom and 0xFF
                val bL = left and 0xFF
                val bR = right and 0xFF
                val newB = (bC * center + (bT + bB + bL + bR) * edge).toInt().coerceIn(0, 255)

                outPixels[idx] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
            }
        }

        // Copy borders
        for (x in 0 until width) {
            outPixels[x] = pixels[x]
            outPixels[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            outPixels[y * width] = pixels[y * width]
            outPixels[y * width + (width - 1)] = pixels[y * width + (width - 1)]
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Safely pads image binary data to satisfy minimum size requirements (e.g. passport portals)
     */
    private fun padImageBytes(bytes: ByteArray, paddingSize: Int, format: OutputFormat): ByteArray {
        val out = ByteArrayOutputStream(bytes.size + paddingSize + 64)
        if (format == OutputFormat.JPEG && bytes.size >= 4 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
            // Write SOI
            out.write(bytes, 0, 2)
            // Insert JPEG COM (Comment) marker: 0xFF, 0xFE, length (2 bytes), payload
            var remaining = paddingSize
            while (remaining > 0) {
                val chunkSize = remaining.coerceAtMost(65530)
                val len = chunkSize + 2
                out.write(0xFF)
                out.write(0xFE)
                out.write((len ushr 8) and 0xFF)
                out.write(len and 0xFF)
                val padBytes = ByteArray(chunkSize) { 0x20.toByte() }
                out.write(padBytes)
                remaining -= chunkSize
            }
            // Write remaining original JPEG bytes
            out.write(bytes, 2, bytes.size - 2)
            return out.toByteArray()
        } else {
            // For other formats or fallback, append null/space padding at end
            out.write(bytes)
            val padding = ByteArray(paddingSize) { 0x00.toByte() }
            out.write(padding)
            return out.toByteArray()
        }
    }
}
