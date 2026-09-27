package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfEngine {

    enum class PageFormat(val displayName: String, val widthPoints: Int, val heightPoints: Int) {
        A4("A4 (595 × 842 pt)", 595, 842),
        LETTER("US Letter (612 × 792 pt)", 612, 792),
        FIT_IMAGE("Fit to Image Size", 0, 0)
    }

    enum class OrientationMode(val displayName: String) {
        AUTO("Auto Detect"),
        PORTRAIT("Portrait"),
        LANDSCAPE("Landscape")
    }

    data class PageEditInfo(
        val originalPageIndex: Int,
        var rotationDegrees: Int = 0, // 0, 90, 180, 270
        var isDeleted: Boolean = false
    )

    /**
     * Converts a list of image URIs to a high quality PDF
     */
    suspend fun convertImagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        pageFormat: PageFormat = PageFormat.A4,
        orientation: OrientationMode = OrientationMode.AUTO,
        marginPt: Int = 15,
        imageQuality: Int = 85,
        outputFileName: String = "Doc_${System.currentTimeMillis()}.pdf",
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "generated_pdfs").apply { mkdirs() }
        val outputFile = File(outputDir, outputFileName)

        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            imageUris.forEachIndexed { index, uri ->
                onProgress(index + 1, imageUris.size)
                var bitmap = ImageEngine.decodeBitmapFromUri(context, uri, maxDimension = 2400)
                if (bitmap != null) {
                    // Compress bitmap if needed to save PDF size
                    if (imageQuality < 100) {
                        val stream = java.io.ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, stream)
                        val bytes = stream.toByteArray()
                        val compressed = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (compressed != null) {
                            bitmap = compressed
                        }
                    }

                    val imgWidth = bitmap.width
                    val imgHeight = bitmap.height

                    val (targetWidth, targetHeight) = when (pageFormat) {
                        PageFormat.FIT_IMAGE -> Pair(imgWidth, imgHeight)
                        else -> {
                            val isLandscape = when (orientation) {
                                OrientationMode.AUTO -> imgWidth > imgHeight
                                OrientationMode.PORTRAIT -> false
                                OrientationMode.LANDSCAPE -> true
                            }
                            if (isLandscape) {
                                Pair(pageFormat.heightPoints, pageFormat.widthPoints)
                            } else {
                                Pair(pageFormat.widthPoints, pageFormat.heightPoints)
                            }
                        }
                    }

                    val pageInfo = PdfDocument.PageInfo.Builder(targetWidth, targetHeight, index + 1).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas

                    // Draw white background
                    canvas.drawColor(Color.WHITE)

                    // Calculate destination rect respecting margin
                    val availWidth = (targetWidth - (marginPt * 2)).coerceAtLeast(10)
                    val availHeight = (targetHeight - (marginPt * 2)).coerceAtLeast(10)

                    val scaleX = availWidth.toFloat() / imgWidth.toFloat()
                    val scaleY = availHeight.toFloat() / imgHeight.toFloat()
                    val scale = Math.min(scaleX, scaleY)

                    val drawWidth = imgWidth * scale
                    val drawHeight = imgHeight * scale

                    val left = marginPt + (availWidth - drawWidth) / 2f
                    val top = marginPt + (availHeight - drawHeight) / 2f

                    val srcRect = Rect(0, 0, imgWidth, imgHeight)
                    val dstRect = RectF(left, top, left + drawWidth, top + drawHeight)

                    canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
                    document.finishPage(page)
                }
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
        } finally {
            document.close()
        }

        outputFile
    }

    /**
     * Renders a PDF to a list of image files (JPG or PNG)
     */
    suspend fun convertPdfToImages(
        context: Context,
        pdfFile: File,
        format: ImageEngine.OutputFormat = ImageEngine.OutputFormat.JPEG,
        dpiScale: Float = 2.0f, // 1.0f = 72 dpi, 2.0f = 144 dpi, 3.0f = 216 dpi
        quality: Int = 90,
        pageIndices: List<Int>? = null, // null means all
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<File> = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "extracted_pages_${System.currentTimeMillis()}").apply { mkdirs() }
        val outputFiles = mutableListOf<File>()

        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)

        try {
            val totalPages = renderer.pageCount
            val targetPages = pageIndices ?: (0 until totalPages).toList()

            targetPages.forEachIndexed { step, pageIdx ->
                if (pageIdx in 0 until totalPages) {
                    onProgress(step + 1, targetPages.size)
                    val page = renderer.openPage(pageIdx)
                    val width = (page.width * dpiScale).toInt().coerceAtLeast(1)
                    val height = (page.height * dpiScale).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val pageFile = File(outputDir, "page_${pageIdx + 1}.${format.extension}")
                    FileOutputStream(pageFile).use { out ->
                        bitmap.compress(format.compressFormat, quality, out)
                    }
                    bitmap.recycle()
                    outputFiles.add(pageFile)
                }
            }
        } finally {
            renderer.close()
            pfd.close()
        }

        outputFiles
    }

    /**
     * Renders PDF pages to in-memory Bitmaps for fast interactive preview and thumbnail grids
     */
    suspend fun renderPdfPageThumbnails(
        pdfFile: File,
        maxPages: Int = 50,
        thumbnailWidth: Int = 300
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        if (!pdfFile.exists()) return@withContext bitmaps

        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)

        try {
            val pagesToRender = Math.min(renderer.pageCount, maxPages)
            for (i in 0 until pagesToRender) {
                val page = renderer.openPage(i)
                val scale = thumbnailWidth.toFloat() / page.width.toFloat()
                val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(thumbnailWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            renderer.close()
            pfd.close()
        }

        bitmaps
    }

    /**
     * Compresses a PDF by downsampling and re-encoding page bitmaps at selected DPI & JPEG quality
     */
    suspend fun compressPdf(
        context: Context,
        inputPdf: File,
        dpiScale: Float = 1.33f, // ~96-100 DPI
        imageQuality: Int = 65,
        outputFileName: String = "Compressed_${System.currentTimeMillis()}.pdf",
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "compressed_pdfs").apply { mkdirs() }
        val outputFile = File(outputDir, outputFileName)

        val pfd = ParcelFileDescriptor.open(inputPdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            val totalPages = renderer.pageCount
            for (i in 0 until totalPages) {
                onProgress(i + 1, totalPages)
                val page = renderer.openPage(i)
                val origPtWidth = page.width
                val origPtHeight = page.height

                val renderWidth = (origPtWidth * dpiScale).toInt().coerceAtLeast(1)
                val renderHeight = (origPtHeight * dpiScale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                // Re-encode bitmap with JPEG compression
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality.coerceIn(10, 100), stream)
                val bytes = stream.toByteArray()
                bitmap.recycle()

                val compressedBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                val pageInfo = PdfDocument.PageInfo.Builder(origPtWidth, origPtHeight, i + 1).create()
                val docPage = document.startPage(pageInfo)
                val docCanvas = docPage.canvas

                docCanvas.drawColor(Color.WHITE)
                if (compressedBitmap != null) {
                    val src = Rect(0, 0, compressedBitmap.width, compressedBitmap.height)
                    val dst = RectF(0f, 0f, origPtWidth.toFloat(), origPtHeight.toFloat())
                    docCanvas.drawBitmap(compressedBitmap, src, dst, paint)
                    compressedBitmap.recycle()
                }
                document.finishPage(docPage)
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
        } finally {
            document.close()
            renderer.close()
            pfd.close()
        }

        outputFile
    }

    /**
     * Compresses a PDF to target a specific custom file size (e.g. 50 KB, 500 KB, 1 MB, 10 MB).
     */
    suspend fun compressPdfToTargetSize(
        context: Context,
        inputPdf: File,
        targetSizeBytes: Long,
        outputFileName: String = "Compressed_${System.currentTimeMillis()}.pdf",
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val pfd = ParcelFileDescriptor.open(inputPdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount
        renderer.close()
        pfd.close()

        val bytesPerPage = (targetSizeBytes / pageCount.coerceAtLeast(1))
        val (dpiScale, quality) = when {
            bytesPerPage < 40_000 -> Pair(0.9f, 35)
            bytesPerPage < 90_000 -> Pair(1.2f, 50)
            bytesPerPage < 250_000 -> Pair(1.5f, 65)
            bytesPerPage < 600_000 -> Pair(2.0f, 75)
            else -> Pair(2.6f, 85)
        }

        compressPdf(
            context = context,
            inputPdf = inputPdf,
            dpiScale = dpiScale,
            imageQuality = quality,
            outputFileName = outputFileName,
            onProgress = onProgress
        )
    }

    /**
     * PDF Page Editor: Reorder, rotate individual pages, delete pages, extract subset
     */
    suspend fun exportEditedPdf(
        context: Context,
        inputPdf: File,
        pagesPlan: List<PageEditInfo>,
        outputFileName: String = "Edited_${System.currentTimeMillis()}.pdf",
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "edited_pdfs").apply { mkdirs() }
        val outputFile = File(outputDir, outputFileName)

        val activePages = pagesPlan.filter { !it.isDeleted }
        val pfd = ParcelFileDescriptor.open(inputPdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            activePages.forEachIndexed { newIndex, pagePlan ->
                onProgress(newIndex + 1, activePages.size)
                val origIdx = pagePlan.originalPageIndex
                if (origIdx in 0 until renderer.pageCount) {
                    val page = renderer.openPage(origIdx)
                    val origWidth = page.width
                    val origHeight = page.height

                    // Render at high resolution (2x 144 DPI)
                    val renderW = origWidth * 2
                    val renderH = origHeight * 2

                    val renderedBitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                    val renderCanvas = Canvas(renderedBitmap)
                    renderCanvas.drawColor(Color.WHITE)
                    page.render(renderedBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    // Apply rotation if needed
                    val rotation = (pagePlan.rotationDegrees % 360 + 360) % 360
                    val finalBitmap = if (rotation != 0) {
                        ImageEngine.rotateBitmap(renderedBitmap, rotation.toFloat())
                    } else {
                        renderedBitmap
                    }

                    val finalPtWidth = if (rotation == 90 || rotation == 270) origHeight else origWidth
                    val finalPtHeight = if (rotation == 90 || rotation == 270) origWidth else origHeight

                    val pageInfo = PdfDocument.PageInfo.Builder(finalPtWidth, finalPtHeight, newIndex + 1).create()
                    val docPage = document.startPage(pageInfo)
                    val docCanvas = docPage.canvas
                    docCanvas.drawColor(Color.WHITE)

                    val src = Rect(0, 0, finalBitmap.width, finalBitmap.height)
                    val dst = RectF(0f, 0f, finalPtWidth.toFloat(), finalPtHeight.toFloat())
                    docCanvas.drawBitmap(finalBitmap, src, dst, paint)

                    document.finishPage(docPage)

                    if (finalBitmap != renderedBitmap) {
                        renderedBitmap.recycle()
                    }
                    finalBitmap.recycle()
                }
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
        } finally {
            document.close()
            renderer.close()
            pfd.close()
        }

        outputFile
    }

    /**
     * Enhances PDF resolution (upscaling up to 300-400 DPI), sharpens scanned text/charts,
     * boosts contrast, and optionally expands file size to meet minimum portal requirements.
     */
    suspend fun enhancePdf(
        context: Context,
        inputPdf: File,
        dpiScale: Float = 3.5f, // ~250-300 DPI
        sharpnessStrength: Float = 0.4f,
        contrastBoost: Float = 1.15f,
        targetMinSizeBytes: Long = 0L,
        outputFileName: String = "enhanced_${System.currentTimeMillis()}.pdf",
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "enhanced_files").apply { mkdirs() }
        val outputFile = File(outputDir, outputFileName)

        val pfd = ParcelFileDescriptor.open(inputPdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            for (i in 0 until pageCount) {
                onProgress(i + 1, pageCount)
                val page = renderer.openPage(i)
                val ptWidth = page.width
                val ptHeight = page.height

                // Render at enhanced DPI
                val renderW = (ptWidth * dpiScale).toInt().coerceAtLeast(1)
                val renderH = (ptHeight * dpiScale).toInt().coerceAtLeast(1)

                val pageBitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                val renderCanvas = Canvas(pageBitmap)
                renderCanvas.drawColor(Color.WHITE)
                page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                // Enhance contrast
                val colorAdjusted = if (contrastBoost != 1.0f) {
                    ImageEngine.adjustColorEnhance(pageBitmap, contrastBoost, 2f)
                } else {
                    pageBitmap
                }

                // Enhance sharpness
                val enhancedBitmap = if (sharpnessStrength > 0.05f) {
                    ImageEngine.sharpenBitmap(colorAdjusted, sharpnessStrength)
                } else {
                    colorAdjusted
                }

                val pageInfo = PdfDocument.PageInfo.Builder(ptWidth, ptHeight, i + 1).create()
                val docPage = document.startPage(pageInfo)
                val docCanvas = docPage.canvas
                docCanvas.drawColor(Color.WHITE)

                val src = Rect(0, 0, enhancedBitmap.width, enhancedBitmap.height)
                val dst = RectF(0f, 0f, ptWidth.toFloat(), ptHeight.toFloat())
                docCanvas.drawBitmap(enhancedBitmap, src, dst, paint)

                document.finishPage(docPage)

                if (colorAdjusted != pageBitmap) pageBitmap.recycle()
                if (enhancedBitmap != colorAdjusted) colorAdjusted.recycle()
                enhancedBitmap.recycle()
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }

            // If a minimum target size is required by a portal, safely append PDF comments (% padding)
            if (targetMinSizeBytes > 0 && outputFile.length() < targetMinSizeBytes) {
                val paddingNeeded = targetMinSizeBytes - outputFile.length()
                val commentHeader = "\n%DocuCraft-Enhanced-Padding-Begin\n".toByteArray()
                val commentFooter = "\n%DocuCraft-Enhanced-Padding-End\n".toByteArray()
                val actualPadding = (paddingNeeded - commentHeader.size - commentFooter.size).coerceAtLeast(0).toInt()

                FileOutputStream(outputFile, true).use { out ->
                    out.write(commentHeader)
                    var rem = actualPadding
                    val buf = ByteArray(8192) { 0x20.toByte() }
                    while (rem > 0) {
                        val toWrite = rem.coerceAtMost(buf.size)
                        out.write(buf, 0, toWrite)
                        rem -= toWrite
                    }
                    out.write(commentFooter)
                }
            }
        } finally {
            document.close()
            renderer.close()
            pfd.close()
        }

        outputFile
    }

    /**
     * Merges multiple PDF files in sequential order into a single unified PDF document.
     */
    suspend fun mergePdfs(
        context: Context,
        inputPdfs: List<File>,
        outputFileName: String = "Merged_${System.currentTimeMillis()}.pdf",
        dpiScale: Float = 2.0f,
        onProgress: (currentFile: Int, totalFiles: Int, totalPages: Int) -> Unit = { _, _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "merged_pdfs").apply { mkdirs() }
        val outputFile = File(outputDir, outputFileName)

        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        var globalPageNumber = 1

        try {
            inputPdfs.forEachIndexed { fileIdx, file ->
                onProgress(fileIdx + 1, inputPdfs.size, globalPageNumber - 1)
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)

                try {
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val ptWidth = page.width
                        val ptHeight = page.height

                        val renderW = (ptWidth * dpiScale).toInt().coerceAtLeast(1)
                        val renderH = (ptHeight * dpiScale).toInt().coerceAtLeast(1)

                        val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        page.close()

                        val pageInfo = PdfDocument.PageInfo.Builder(ptWidth, ptHeight, globalPageNumber++).create()
                        val docPage = document.startPage(pageInfo)
                        val src = Rect(0, 0, bitmap.width, bitmap.height)
                        val dst = RectF(0f, 0f, ptWidth.toFloat(), ptHeight.toFloat())
                        docPage.canvas.drawBitmap(bitmap, src, dst, paint)
                        document.finishPage(docPage)

                        bitmap.recycle()
                    }
                } finally {
                    renderer.close()
                    pfd.close()
                }
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
        } finally {
            document.close()
        }

        outputFile
    }

    /**
     * Splits a PDF by extracting selected page indices (0-based) into a new standalone PDF.
     */
    suspend fun splitPdf(
        context: Context,
        inputPdf: File,
        selectedPageIndices: List<Int>,
        outputFileName: String = "${inputPdf.nameWithoutExtension}_split_${System.currentTimeMillis()}.pdf",
        dpiScale: Float = 2.0f,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "split_pdfs").apply { mkdirs() }
        val outputFile = File(outputDir, outputFileName)

        val pfd = ParcelFileDescriptor.open(inputPdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            val validIndices = selectedPageIndices.filter { it in 0 until renderer.pageCount }
            validIndices.forEachIndexed { newIndex, pageIdx ->
                onProgress(newIndex + 1, validIndices.size)
                val page = renderer.openPage(pageIdx)
                val ptWidth = page.width
                val ptHeight = page.height

                val renderW = (ptWidth * dpiScale).toInt().coerceAtLeast(1)
                val renderH = (ptHeight * dpiScale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(ptWidth, ptHeight, newIndex + 1).create()
                val docPage = document.startPage(pageInfo)
                val src = Rect(0, 0, bitmap.width, bitmap.height)
                val dst = RectF(0f, 0f, ptWidth.toFloat(), ptHeight.toFloat())
                docPage.canvas.drawBitmap(bitmap, src, dst, paint)
                document.finishPage(docPage)

                bitmap.recycle()
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
        } finally {
            document.close()
            renderer.close()
            pfd.close()
        }

        outputFile
    }

    /**
     * Splits every page of a PDF into individual single-page PDF files.
     */
    suspend fun splitPdfAllPages(
        context: Context,
        inputPdf: File,
        dpiScale: Float = 2.0f,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<File> = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "split_pages_${System.currentTimeMillis()}").apply { mkdirs() }
        val resultFiles = mutableListOf<File>()

        val pfd = ParcelFileDescriptor.open(inputPdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            val total = renderer.pageCount
            for (i in 0 until total) {
                onProgress(i + 1, total)
                val page = renderer.openPage(i)
                val ptWidth = page.width
                val ptHeight = page.height

                val renderW = (ptWidth * dpiScale).toInt().coerceAtLeast(1)
                val renderH = (ptHeight * dpiScale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val singleDoc = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(ptWidth, ptHeight, 1).create()
                val docPage = singleDoc.startPage(pageInfo)
                val src = Rect(0, 0, bitmap.width, bitmap.height)
                val dst = RectF(0f, 0f, ptWidth.toFloat(), ptHeight.toFloat())
                docPage.canvas.drawBitmap(bitmap, src, dst, paint)
                singleDoc.finishPage(docPage)

                val singleFile = File(outputDir, "${inputPdf.nameWithoutExtension}_page_${i + 1}.pdf")
                FileOutputStream(singleFile).use { out ->
                    singleDoc.writeTo(out)
                }
                singleDoc.close()
                bitmap.recycle()
                resultFiles.add(singleFile)
            }
        } finally {
            renderer.close()
            pfd.close()
        }

        resultFiles
    }

    // ==========================================
    // 8. PDF SECURITY & ENCRYPTION / DECRYPTION
    // ==========================================

    data class PdfPermissions(
        val canPrint: Boolean = true,
        val canModify: Boolean = false,
        val canExtractContent: Boolean = true,
        val canModifyAnnotations: Boolean = true,
        val canAssembleDocument: Boolean = false
    )

    data class PdfSecurityInfo(
        val isEncrypted: Boolean,
        val pageCount: Int = 0,
        val requiresPassword: Boolean = false
    )

    @Volatile
    private var isPdfBoxInitialized = false

    fun initPdfBox(context: Context) {
        if (!isPdfBoxInitialized) {
            synchronized(this) {
                if (!isPdfBoxInitialized) {
                    com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context.applicationContext)
                    isPdfBoxInitialized = true
                }
            }
        }
    }

    /**
     * Checks if a PDF is encrypted or password-protected and reads page count if accessible.
     */
    suspend fun checkPdfSecurity(context: Context, pdfFile: File): PdfSecurityInfo = withContext(Dispatchers.IO) {
        initPdfBox(context)
        try {
            com.tom_roush.pdfbox.pdmodel.PDDocument.load(pdfFile).use { doc ->
                PdfSecurityInfo(
                    isEncrypted = doc.isEncrypted,
                    pageCount = doc.numberOfPages,
                    requiresPassword = false
                )
            }
        } catch (e: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
            PdfSecurityInfo(
                isEncrypted = true,
                pageCount = 0,
                requiresPassword = true
            )
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("password") || msg.contains("encrypted")) {
                PdfSecurityInfo(isEncrypted = true, pageCount = 0, requiresPassword = true)
            } else {
                PdfSecurityInfo(isEncrypted = false, pageCount = 0, requiresPassword = false)
            }
        }
    }

    /**
     * Encrypts a PDF with User (Open) password, Owner (Permissions) password,
     * and configurable access restrictions (printing, copying, editing, annotations).
     */
    suspend fun encryptPdf(
        context: Context,
        inputPdf: File,
        userPassword: String,
        ownerPassword: String,
        permissions: PdfPermissions,
        keyLength: Int = 128,
        outputFileName: String = "${inputPdf.nameWithoutExtension}_protected.pdf"
    ): File = withContext(Dispatchers.IO) {
        initPdfBox(context)
        val outputDir = File(context.cacheDir, "protected_pdfs").apply { mkdirs() }
        val outputFile = File(outputDir, outputFileName)

        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputPdf)
        try {
            val ap = com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission()
            ap.setCanPrint(permissions.canPrint)
            ap.setCanModify(permissions.canModify)
            ap.setCanExtractContent(permissions.canExtractContent)
            ap.setCanModifyAnnotations(permissions.canModifyAnnotations)
            ap.setCanAssembleDocument(permissions.canAssembleDocument)

            val effectiveOwnerPassword = ownerPassword.ifEmpty { userPassword }
            val spp = com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy(
                effectiveOwnerPassword,
                userPassword,
                ap
            )
            spp.encryptionKeyLength = if (keyLength == 256) 256 else 128
            document.protect(spp)
            document.save(outputFile)
        } finally {
            document.close()
        }
        outputFile
    }

    /**
     * Decrypts a password-protected PDF by providing the correct password and removing all security.
     */
    suspend fun decryptPdf(
        context: Context,
        inputPdf: File,
        password: String,
        outputFileName: String = "${inputPdf.nameWithoutExtension}_unlocked.pdf"
    ): File = withContext(Dispatchers.IO) {
        initPdfBox(context)
        val outputDir = File(context.cacheDir, "unlocked_pdfs").apply { mkdirs() }
        val outputFile = File(outputDir, outputFileName)

        val document = try {
            com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputPdf, password)
        } catch (e: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
            throw IllegalArgumentException("Incorrect password. Please verify the password and try again.")
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("password", ignoreCase = true)) {
                throw IllegalArgumentException("Incorrect password. Please enter the valid open or owner password.")
            } else {
                throw e
            }
        }

        try {
            if (document.isEncrypted) {
                document.isAllSecurityToBeRemoved = true
            }
            document.save(outputFile)
        } finally {
            document.close()
        }
        outputFile
    }
}
