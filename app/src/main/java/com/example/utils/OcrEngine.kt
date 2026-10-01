package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OcrEngine {

    data class OcrElement(
        val text: String,
        val boundingBox: Rect?
    )

    data class OcrLine(
        val text: String,
        val boundingBox: Rect?,
        val elements: List<OcrElement>
    )

    data class OcrBlock(
        val text: String,
        val boundingBox: Rect?,
        val lines: List<OcrLine>
    )

    data class OcrResult(
        val fullText: String,
        val blocks: List<OcrBlock>
    )

    /**
     * Extracts text and geometric bounding boxes from a bitmap using on-device ML Kit.
     */
    suspend fun recognizeText(bitmap: Bitmap): OcrResult = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks.map { b ->
                    val lines = b.lines.map { l ->
                        val elements = l.elements.map { e ->
                            OcrElement(e.text, e.boundingBox)
                        }
                        OcrLine(l.text, l.boundingBox, elements)
                    }
                    OcrBlock(b.text, b.boundingBox, lines)
                }
                recognizer.close()
                if (cont.isActive) {
                    cont.resume(OcrResult(visionText.text, blocks))
                }
            }
            .addOnFailureListener { e ->
                recognizer.close()
                if (cont.isActive) {
                    cont.resumeWithException(e)
                }
            }
    }

    /**
     * Creates a Searchable PDF where the original image is displayed,
     * and an invisible searchable/selectable text layer is overlaid at the exact word coordinates.
     */
    suspend fun createSearchablePdf(
        context: Context,
        sourceBitmap: Bitmap,
        ocrResult: OcrResult,
        outputFileName: String = "Searchable_OCR_${System.currentTimeMillis()}.pdf"
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "ocr_documents").apply { mkdirs() }
        val outputFile = File(outputDir, outputFileName)

        val imgWidth = sourceBitmap.width
        val imgHeight = sourceBitmap.height

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(imgWidth, imgHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // 1. Draw source image as visible layer
        canvas.drawBitmap(sourceBitmap, 0f, 0f, null)

        // 2. Invisible text paint (alpha = 0 ensures text is invisible visually but indexed by PDF parsers)
        val invisibleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0, 0, 0, 0)
            isSubpixelText = true
        }

        // 3. Draw each line/element at its exact bounding box
        for (block in ocrResult.blocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                if (box != null && line.text.isNotBlank()) {
                    val boxHeight = (box.bottom - box.top).toFloat()
                    val boxWidth = (box.right - box.left).toFloat()
                    if (boxHeight > 0f && boxWidth > 0f) {
                        invisibleTextPaint.textSize = boxHeight * 0.85f
                        // In Android canvas, drawText y is the baseline (near box.bottom)
                        val baselineY = box.bottom.toFloat() - (boxHeight * 0.15f)
                        canvas.drawText(line.text, box.left.toFloat(), baselineY, invisibleTextPaint)
                    }
                }
            }
        }

        document.finishPage(page)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        outputFile
    }

    /**
     * Saves extracted plain text to a .txt file.
     */
    suspend fun saveExtractedTextFile(
        context: Context,
        text: String,
        outputFileName: String = "Extracted_Text_${System.currentTimeMillis()}.txt"
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "ocr_documents").apply { mkdirs() }
        val outputFile = File(outputDir, outputFileName)
        outputFile.writeText(text, Charsets.UTF_8)
        outputFile
    }
}
