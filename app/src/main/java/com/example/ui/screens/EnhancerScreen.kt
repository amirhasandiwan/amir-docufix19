package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppHeader
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.PdfUtilViewModel
import com.example.utils.FileOpener
import com.example.utils.ImageEngine
import java.io.File

@Composable
fun EnhancerScreen(
    viewModel: PdfUtilViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentTab by viewModel.enhancerTab.collectAsStateWithLifecycle()

    // Image Enhancer State
    val sourceImage by viewModel.enhanceSourceImage.collectAsStateWithLifecycle()
    val sourceImgSize by viewModel.enhanceSourceImageOriginalSize.collectAsStateWithLifecycle()
    val imageScale by viewModel.enhanceImageScale.collectAsStateWithLifecycle()
    val imageSharpness by viewModel.enhanceImageSharpness.collectAsStateWithLifecycle()
    val imageContrast by viewModel.enhanceImageContrast.collectAsStateWithLifecycle()
    val imageTargetMinKb by viewModel.enhanceImageTargetMinKb.collectAsStateWithLifecycle()
    val imageFormat by viewModel.enhanceImageFormat.collectAsStateWithLifecycle()
    val enhancedImageResult by viewModel.enhancedImageResult.collectAsStateWithLifecycle()
    val enhancedImageBitmap by viewModel.enhancedImageResultBitmap.collectAsStateWithLifecycle()

    // PDF Enhancer State
    val sourcePdf by viewModel.enhanceSourcePdf.collectAsStateWithLifecycle()
    val pdfDpiScale by viewModel.enhancePdfDpiScale.collectAsStateWithLifecycle()
    val pdfSharpness by viewModel.enhancePdfSharpness.collectAsStateWithLifecycle()
    val pdfContrast by viewModel.enhancePdfContrast.collectAsStateWithLifecycle()
    val pdfTargetMinKb by viewModel.enhancePdfTargetMinKb.collectAsStateWithLifecycle()
    val enhancedPdfResult by viewModel.enhancedPdfResult.collectAsStateWithLifecycle()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.selectImageForEnhance(uri)
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectPdfUriForEnhance(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("enhancer_screen")
    ) {
        AppHeader(
            title = "Size & Quality Enhancer",
            subtitle = "Super-Resolution • Text Clarity • Minimum Size Padding",
            onBackClick = onBack
        )

        // Tab Selector: Image vs PDF
        TabRow(
            selectedTabIndex = currentTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryIndigo
        ) {
            Tab(
                selected = currentTab == PdfUtilViewModel.EnhancerTab.IMAGE,
                onClick = { viewModel.setEnhancerTab(PdfUtilViewModel.EnhancerTab.IMAGE) },
                icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text("Image Enhancer", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = currentTab == PdfUtilViewModel.EnhancerTab.PDF,
                onClick = { viewModel.setEnhancerTab(PdfUtilViewModel.EnhancerTab.PDF) },
                icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text("PDF Enhancer", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (currentTab == PdfUtilViewModel.EnhancerTab.IMAGE) {
                // ==========================================
                // IMAGE ENHANCER TAB
                // ==========================================
                item {
                    // Pickers Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_pick_image_enhance"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pick Image")
                        }

                        OutlinedButton(
                            onClick = { viewModel.loadSampleImageForEnhance() },
                            modifier = Modifier.testTag("btn_load_sample_enhance_image"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentAmber)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sample")
                        }
                    }
                }

                if (sourceImage != null) {
                    // Source Preview Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = sourceImage!!.asImageBitmap(),
                                        contentDescription = "Original preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Original Image",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Dimensions: ${sourceImage!!.width} × ${sourceImage!!.height} px",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Size: ${ImageEngine.formatFileSize(sourceImgSize)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryIndigo
                                    )
                                }
                            }
                        }
                    }

                    // Enhancement Controls Card
                    item {
                        OutlinedCard(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryIndigo)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Enhancement & Upscaling Controls",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 1. Upscale Factor
                                Text(
                                    text = "Resolution Upscale: ${"%.1f".format(imageScale)}x  (${((sourceImage?.width ?: 0) * imageScale).toInt()} × ${((sourceImage?.height ?: 0) * imageScale).toInt()} px)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(1.0f to "1.0x", 1.5f to "1.5x", 2.0f to "2.0x HD", 3.0f to "3.0x", 4.0f to "4.0x Max").forEach { (scale, label) ->
                                        FilterChip(
                                            selected = (imageScale == scale),
                                            onClick = { viewModel.setEnhanceImageScale(scale) },
                                            label = { Text(label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryIndigo,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // 2. Edge & Text Sharpness
                                Text(
                                    text = "Detail & Edge Sharpness: ${(imageSharpness * 100).toInt()}%",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Slider(
                                    value = imageSharpness,
                                    onValueChange = { viewModel.setEnhanceImageSharpness(it) },
                                    valueRange = 0.0f..1.0f,
                                    modifier = Modifier.testTag("slider_image_sharpness")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // 3. Contrast & Legibility Boost
                                Text(
                                    text = "Contrast & Text Clarity: ${"%.2f".format(imageContrast)}x",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Slider(
                                    value = imageContrast,
                                    onValueChange = { viewModel.setEnhanceImageContrast(it) },
                                    valueRange = 1.0f..1.4f,
                                    modifier = Modifier.testTag("slider_image_contrast")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // 4. Target Minimum File Size (for passport/exam/visa upload portals)
                                Text(
                                    text = "Target Minimum Size: ${if (imageTargetMinKb == 0) "No minimum" else "$imageTargetMinKb KB"}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Guarantees file satisfies portals requiring minimum file size.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(0 to "None", 200 to "200 KB", 500 to "500 KB", 1000 to "1 MB", 2000 to "2 MB").forEach { (kb, label) ->
                                        FilterChip(
                                            selected = (imageTargetMinKb == kb),
                                            onClick = { viewModel.setEnhanceImageTargetMinKb(kb) },
                                            label = { Text(label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SecondaryTeal,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 5. Output Format
                                Text(
                                    text = "Output Format",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ImageEngine.OutputFormat.values().forEach { fmt ->
                                        FilterChip(
                                            selected = (imageFormat == fmt),
                                            onClick = { viewModel.setEnhanceImageFormat(fmt) },
                                            label = { Text(fmt.extension.uppercase(), fontSize = 12.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Execute Button
                    item {
                        Button(
                            onClick = { viewModel.runEnhanceImage() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_run_enhance_image"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enhance & Upscale Image", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                } else {
                    // Empty State Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryIndigo.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = null,
                                        tint = PrimaryIndigo,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Smart Image Size & Quality Enhancer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Upscale images up to 4x resolution, sharpen blurry scans, enhance text legibility, or pad file size for strict portal minimum requirements.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Enhanced Result Card
                if (enhancedImageResult != null && enhancedImageBitmap != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuccessGreen.copy(alpha = 0.5f))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Enhanced Image Ready!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = SuccessGreen
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = enhancedImageBitmap!!.asImageBitmap(),
                                            contentDescription = "Enhanced output",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = enhancedImageResult!!.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "New Resolution: ${enhancedImageBitmap!!.width} × ${enhancedImageBitmap!!.height} px",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Enhanced Size: ${ImageEngine.formatFileSize(enhancedImageResult!!.length())}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SuccessGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            FileOpener.openFile(context, enhancedImageResult!!, "image/*")
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                    ) {
                                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Image")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            FileOpener.shareFile(context, enhancedImageResult!!, "image/*")
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // PDF ENHANCER TAB
                // ==========================================
                item {
                    // Pickers Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_pick_pdf_enhance"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pick PDF")
                        }

                        OutlinedButton(
                            onClick = { viewModel.loadSamplePdfForEnhance() },
                            modifier = Modifier.testTag("btn_load_sample_enhance_pdf"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentAmber)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sample")
                        }
                    }
                }

                if (sourcePdf != null) {
                    // Source PDF Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(AccentCoral.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentCoral)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sourcePdf!!.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Current Size: ${ImageEngine.formatFileSize(sourcePdf!!.length())}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // PDF Controls Card
                    item {
                        OutlinedCard(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryIndigo)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PDF Enhancement Settings",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // DPI Preset
                                Text(
                                    text = "Rendering Density / DPI",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        2.0f to "150 DPI",
                                        2.77f to "200 DPI",
                                        3.5f to "250 DPI",
                                        4.16f to "300 DPI (Print)"
                                    ).forEach { (scale, label) ->
                                        FilterChip(
                                            selected = (pdfDpiScale == scale),
                                            onClick = { viewModel.setEnhancePdfDpiScale(scale) },
                                            label = { Text(label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryIndigo,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Text Sharpness
                                Text(
                                    text = "Document Sharpness: ${(pdfSharpness * 100).toInt()}%",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Slider(
                                    value = pdfSharpness,
                                    onValueChange = { viewModel.setEnhancePdfSharpness(it) },
                                    valueRange = 0.0f..0.8f,
                                    modifier = Modifier.testTag("slider_pdf_sharpness")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Contrast Boost
                                Text(
                                    text = "Text Contrast & Clean Background: ${"%.2f".format(pdfContrast)}x",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Slider(
                                    value = pdfContrast,
                                    onValueChange = { viewModel.setEnhancePdfContrast(it) },
                                    valueRange = 1.0f..1.35f,
                                    modifier = Modifier.testTag("slider_pdf_contrast")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Target Minimum Size
                                Text(
                                    text = "Target Minimum Size: ${if (pdfTargetMinKb == 0) "No minimum" else "$pdfTargetMinKb KB"}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Ensures the PDF satisfies university, visa, or tender portal requirements.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(0 to "None", 250 to "250 KB", 500 to "500 KB", 1000 to "1 MB", 2000 to "2 MB").forEach { (kb, label) ->
                                        FilterChip(
                                            selected = (pdfTargetMinKb == kb),
                                            onClick = { viewModel.setEnhancePdfTargetMinKb(kb) },
                                            label = { Text(label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SecondaryTeal,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Execute Button
                    item {
                        Button(
                            onClick = { viewModel.runEnhancePdf() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_run_enhance_pdf"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enhance & Upscale PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                } else {
                    // Empty state card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(AccentCoral.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = AccentCoral,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "PDF Resolution & Size Enhancer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Render scanned or low-res PDFs at 300 DPI print quality, boost text sharpness and contrast, or expand file size to satisfy portal requirements.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Enhanced PDF Result Card
                if (enhancedPdfResult != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SuccessGreen.copy(alpha = 0.5f))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Enhanced PDF Ready!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = SuccessGreen
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(SuccessGreen.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = SuccessGreen)
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = enhancedPdfResult!!.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Enhanced Size: ${ImageEngine.formatFileSize(enhancedPdfResult!!.length())}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SuccessGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            FileOpener.openFile(context, enhancedPdfResult!!, "application/pdf")
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                    ) {
                                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open PDF")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            FileOpener.shareFile(context, enhancedPdfResult!!, "application/pdf")
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
