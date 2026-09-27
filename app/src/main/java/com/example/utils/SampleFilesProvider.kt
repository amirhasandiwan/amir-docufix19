package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SampleFilesProvider {

    suspend fun getOrCreateSamplePdf(context: Context): File = withContext(Dispatchers.IO) {
        val sampleDir = File(context.filesDir, "sample_docs").apply { mkdirs() }
        val samplePdf = File(sampleDir, "Sample_Business_Report.pdf")

        // If exists and non-empty, return
        if (samplePdf.exists() && samplePdf.length() > 1000) {
            return@withContext samplePdf
        }

        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 3 pages: Page 1 Cover & Overview, Page 2 Detailed Analytics, Page 3 Terms & Signature
        val pages = listOf(
            "QUARTERLY BUSINESS OVERVIEW" to "Q3 Performance Review & Operational Report\nPrepared for Board of Directors\nConfidential Document\n\n• Revenue: $1,420,000 (+18% YoY)\n• Active Users: 840,000 (+32% YoY)\n• System Uptime: 99.98%\n• Offline First Architecture Deployed",
            "FINANCIAL PERFORMANCE BREAKDOWN" to "Detailed revenue streams and expenditure.\n\nProduct Sales: $850,000\nEnterprise Subscriptions: $420,000\nSupport & Consulting: $150,000\n\nOperating Margin: 34.2%\nGross Profit: $980,000\nR&D Investment: $240,000\nMarketing & Growth: $190,000",
            "AUTHORIZATION & CONCLUSION" to "Summary of Strategic Milestones for 2026.\n\nAll digital assets have been archived securely.\nCompliance checked against local regulatory guidelines.\n\nAuthorized Signatory:\nJohnathan Vance, Chief Executive Officer\nDate: September 2026\nStatus: APPROVED"
        )

        pages.forEachIndexed { index, (title, body) ->
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Background
            canvas.drawColor(Color.WHITE)

            // Header Banner
            val headerColor = when (index) {
                0 -> Color.rgb(37, 99, 235) // Blue
                1 -> Color.rgb(13, 148, 136) // Teal
                else -> Color.rgb(79, 70, 229) // Indigo
            }
            paint.color = headerColor
            canvas.drawRect(0f, 0f, 595f, 100f, paint)

            // Top title text
            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("Amir DocuFix Document Suite", 40f, 50f, paint)
            paint.textSize = 12f
            paint.isFakeBoldText = false
            canvas.drawText("Page ${index + 1} of 3 • Generated Report", 40f, 75f, paint)

            // Section card
            paint.color = Color.rgb(241, 245, 249)
            val cardRect = RectF(40f, 130f, 555f, 220f)
            canvas.drawRoundRect(cardRect, 12f, 12f, paint)

            paint.color = headerColor
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText(title, 60f, 175f, paint)

            // Body text
            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 14f
            paint.isFakeBoldText = false

            var yPos = 260f
            body.split("\n").forEach { line ->
                if (line.startsWith("•") || line.contains(":")) {
                    paint.isFakeBoldText = true
                    paint.color = Color.rgb(30, 41, 59)
                } else {
                    paint.isFakeBoldText = false
                    paint.color = Color.rgb(71, 85, 105)
                }
                canvas.drawText(line, 60f, yPos, paint)
                yPos += 26f
            }

            // Decorative graphics
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawRect(40f, 750f, 555f, 752f, paint)
            paint.textSize = 11f
            paint.color = Color.rgb(148, 163, 184)
            paint.isFakeBoldText = false
            canvas.drawText("Generated completely offline on device via Android Native PDF Engine", 40f, 780f, paint)

            document.finishPage(page)
        }

        FileOutputStream(samplePdf).use { out ->
            document.writeTo(out)
        }
        document.close()

        samplePdf
    }

    suspend fun getOrCreateSampleImages(context: Context): List<File> = withContext(Dispatchers.IO) {
        val sampleDir = File(context.filesDir, "sample_images").apply { mkdirs() }
        val imageFiles = mutableListOf<File>()

        val samples = listOf(
            Triple("Sample_Document_Scan.jpg", Color.rgb(248, 250, 252), "Contract Scan #1042"),
            Triple("Sample_Receipt.jpg", Color.rgb(254, 243, 199), "Store Receipt $84.20"),
            Triple("Sample_Diagram.jpg", Color.rgb(224, 242, 254), "System Architecture V2")
        )

        samples.forEachIndexed { idx, (filename, bgColor, caption) ->
            val file = File(sampleDir, filename)
            if (!file.exists() || file.length() < 100) {
                val bitmap = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                // Fill background
                canvas.drawColor(bgColor)

                // Draw border
                paint.color = Color.rgb(203, 213, 225)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 10f
                canvas.drawRect(20f, 20f, 1180f, 1580f, paint)
                paint.style = Paint.Style.FILL

                // Draw header band
                val bannerColor = when (idx) {
                    0 -> Color.rgb(37, 99, 235)
                    1 -> Color.rgb(217, 119, 6)
                    else -> Color.rgb(13, 148, 136)
                }
                paint.color = bannerColor
                canvas.drawRect(40f, 40f, 1160f, 220f, paint)

                paint.color = Color.WHITE
                paint.textSize = 54f
                paint.isFakeBoldText = true
                canvas.drawText("SAMPLE IMAGE #${idx + 1}", 80f, 140f, paint)

                // Center visual card
                paint.color = Color.WHITE
                val card = RectF(80f, 300f, 1120f, 1200f)
                canvas.drawRoundRect(card, 24f, 24f, paint)

                paint.color = Color.rgb(30, 41, 59)
                paint.textSize = 46f
                paint.isFakeBoldText = true
                canvas.drawText(caption, 120f, 400f, paint)

                paint.textSize = 32f
                paint.color = Color.rgb(100, 116, 139)
                paint.isFakeBoldText = false
                canvas.drawText("High Resolution Sample Image for PDF & Compression Testing", 120f, 480f, paint)
                canvas.drawText("Dimensions: 1200 × 1600 px", 120f, 540f, paint)
                canvas.drawText("Offline processing benchmark asset", 120f, 600f, paint)

                // Simulated lines
                paint.color = Color.rgb(226, 232, 240)
                for (y in 680..1100 step 70) {
                    canvas.drawRect(120f, y.toFloat(), 1080f, (y + 16).toFloat(), paint)
                }

                // Footer badge
                paint.color = bannerColor
                canvas.drawRoundRect(RectF(80f, 1300f, 480f, 1420f), 20f, 20f, paint)
                paint.color = Color.WHITE
                paint.textSize = 36f
                paint.isFakeBoldText = true
                canvas.drawText("READY TO CONVERT", 110f, 1375f, paint)

                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                bitmap.recycle()
            }
            imageFiles.add(file)
        }

        imageFiles
    }

    suspend fun getOrCreateMultipleSamplePdfs(context: Context): List<File> = withContext(Dispatchers.IO) {
        val sampleDir = File(context.filesDir, "sample_docs").apply { mkdirs() }
        val pdf1 = getOrCreateSamplePdf(context)

        val pdf2 = File(sampleDir, "Sample_Financial_Annex.pdf")
        if (!pdf2.exists() || pdf2.length() < 500) {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawColor(Color.WHITE)

            paint.color = Color.rgb(16, 185, 129) // Emerald Green
            canvas.drawRect(0f, 0f, 595f, 100f, paint)

            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("Financial Annex - Part B", 40f, 55f, paint)

            paint.color = Color.rgb(30, 41, 59)
            paint.textSize = 14f
            canvas.drawText("Audited Balance Sheet & Cash Flow Statement", 40f, 150f, paint)
            canvas.drawText("Operating Cash: $640,000", 40f, 190f, paint)
            canvas.drawText("Net Liquidity Ratio: 2.8x", 40f, 220f, paint)
            canvas.drawText("Tax Provision: Compliant", 40f, 250f, paint)

            doc.finishPage(page)
            FileOutputStream(pdf2).use { doc.writeTo(it) }
            doc.close()
        }

        val pdf3 = File(sampleDir, "Sample_Terms_Addendum.pdf")
        if (!pdf3.exists() || pdf3.length() < 500) {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawColor(Color.WHITE)

            paint.color = Color.rgb(239, 68, 68) // Coral Red
            canvas.drawRect(0f, 0f, 595f, 100f, paint)

            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("Contract Terms & Addendum", 40f, 55f, paint)

            paint.color = Color.rgb(30, 41, 59)
            paint.textSize = 14f
            canvas.drawText("Standard Non-Disclosure Agreement (NDA)", 40f, 150f, paint)
            canvas.drawText("Jurisdiction: Digital Document Protocol 2026", 40f, 190f, paint)
            canvas.drawText("Signatures verified and sealed.", 40f, 220f, paint)

            doc.finishPage(page)
            FileOutputStream(pdf3).use { doc.writeTo(it) }
            doc.close()
        }

        listOf(pdf1, pdf2, pdf3)
    }

    /**
     * Generates or retrieves an encrypted sample PDF with password "1234"
     * for instant testing of password removal/unlocking.
     */
    suspend fun getOrCreateEncryptedSamplePdf(context: Context, password: String = "1234"): File = withContext(Dispatchers.IO) {
        val sampleDir = File(context.filesDir, "sample_docs").apply { mkdirs() }
        val lockedSample = File(sampleDir, "Sample_Encrypted_Confidential.pdf")
        if (lockedSample.exists() && lockedSample.length() > 500) {
            return@withContext lockedSample
        }

        val baseSample = getOrCreateSamplePdf(context)
        PdfEngine.encryptPdf(
            context = context,
            inputPdf = baseSample,
            userPassword = password,
            ownerPassword = "${password}_admin",
            permissions = PdfEngine.PdfPermissions(
                canPrint = false,
                canModify = false,
                canExtractContent = false,
                canModifyAnnotations = false
            ),
            keyLength = 128,
            outputFileName = "Sample_Encrypted_Confidential.pdf"
        ).also { encryptedFile ->
            // Copy or rename into sampleDir
            encryptedFile.copyTo(lockedSample, overwrite = true)
        }
        lockedSample
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
