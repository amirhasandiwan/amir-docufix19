package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppHeader
import com.example.ui.components.ConversionResultCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.PdfUtilViewModel
import com.example.utils.FileOpener
import com.example.utils.ImageEngine
import java.io.File

@Composable
fun CompressorScreen(
    viewModel: PdfUtilViewModel,
    currentTab: PdfUtilViewModel.CompressorTab,
    // Image Compressor state
    sourceBitmap: Bitmap?,
    sourceImageOriginalSize: Long,
    imageQuality: Int,
    imageScale: Float,
    imageFormat: ImageEngine.OutputFormat,
    compressedImageResult: File?,
    // PDF Compressor state
    sourcePdfFile: File?,
    pdfCompressPreset: PdfUtilViewModel.PdfCompressPreset,
    compressedPdfResult: File?,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val imageCompressMode by viewModel.imageCompressMode.collectAsStateWithLifecycle()
    val imageCustomTargetSizeText by viewModel.imageCustomTargetSizeText.collectAsStateWithLifecycle()
    val imageCustomTargetUnit by viewModel.imageCustomTargetUnit.collectAsStateWithLifecycle()

    val pdfCompressMode by viewModel.pdfCompressMode.collectAsStateWithLifecycle()
    val pdfCustomTargetSizeText by viewModel.pdfCustomTargetSizeText.collectAsStateWithLifecycle()
    val pdfCustomTargetUnit by viewModel.pdfCustomTargetUnit.collectAsStateWithLifecycle()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.selectImageForCompression(uri)
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectPdfUriForCompression(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("compressor_screen")
    ) {
        AppHeader(
            title = "Custom Compressor",
            subtitle = "Reduce image and PDF file sizes on device",
            onBackClick = onBack
        )

        // Subtabs
        TabRow(
            selectedTabIndex = if (currentTab == PdfUtilViewModel.CompressorTab.IMAGE) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = currentTab == PdfUtilViewModel.CompressorTab.IMAGE,
                onClick = { viewModel.setCompressorTab(PdfUtilViewModel.CompressorTab.IMAGE) },
                text = { Text("Image Compressor", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = currentTab == PdfUtilViewModel.CompressorTab.PDF,
                onClick = { viewModel.setCompressorTab(PdfUtilViewModel.CompressorTab.PDF) },
                text = { Text("PDF Compressor", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentTab == PdfUtilViewModel.CompressorTab.IMAGE) {
                // ==================== IMAGE COMPRESSOR TAB ====================
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_select_img_compress"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                                ) {
                                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pick Image")
                                }

                                OutlinedButton(
                                    onClick = { viewModel.loadSampleImageForCompressor() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_sample_img_compress"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = SecondaryTeal)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Load Demo")
                                }
                            }
                        }
                    }
                }

                if (sourceBitmap != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = sourceBitmap.asImageBitmap(),
                                    contentDescription = "Source Image",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "Original Image",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Resolution: ${sourceBitmap.width} × ${sourceBitmap.height} px",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Size: ${ImageEngine.formatFileSize(sourceImageOriginalSize)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryIndigo
                                    )
                                }
                            }
                        }
                    }

                    // Compression Controls
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = SecondaryTeal)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Compression Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Mode Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = imageCompressMode == PdfUtilViewModel.CompressSizeMode.TARGET_SIZE,
                                        onClick = { viewModel.setImageCompressMode(PdfUtilViewModel.CompressSizeMode.TARGET_SIZE) },
                                        label = { Text("🎯 Custom Target Size") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SecondaryTeal,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                    FilterChip(
                                        selected = imageCompressMode == PdfUtilViewModel.CompressSizeMode.MANUAL_SLIDERS,
                                        onClick = { viewModel.setImageCompressMode(PdfUtilViewModel.CompressSizeMode.MANUAL_SLIDERS) },
                                        label = { Text("⚙️ Manual Sliders") }
                                    )
                                }

                                if (imageCompressMode == PdfUtilViewModel.CompressSizeMode.TARGET_SIZE) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Target File Size (Apne Hisab Se)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Aap jitna chahe utna exact size set kar sakte hain (e.g. 10 KB, 500 KB, 1 MB, 10 MB)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Input & Unit Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = imageCustomTargetSizeText,
                                            onValueChange = { viewModel.setImageCustomTargetSizeText(it) },
                                            label = { Text("Target Size") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("input_image_target_size")
                                        )

                                        // Unit Selector: KB vs MB
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            PdfUtilViewModel.SizeUnit.values().forEach { unit ->
                                                FilterChip(
                                                    selected = imageCustomTargetUnit == unit,
                                                    onClick = { viewModel.setImageCustomTargetUnit(unit) },
                                                    label = { Text(unit.label, fontWeight = FontWeight.Bold) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = SecondaryTeal,
                                                        selectedLabelColor = Color.White
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Popular Quick Presets:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Presets Row 1 (10 KB to 200 KB)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("10", "20", "50", "100", "200").forEach { v ->
                                            FilterChip(
                                                selected = (imageCustomTargetSizeText == v && imageCustomTargetUnit == PdfUtilViewModel.SizeUnit.KB),
                                                onClick = { viewModel.setQuickImageTarget(v, PdfUtilViewModel.SizeUnit.KB) },
                                                label = { Text("$v KB", fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Presets Row 2 (500 KB to 10 MB)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("500" to PdfUtilViewModel.SizeUnit.KB, "1" to PdfUtilViewModel.SizeUnit.MB, "2" to PdfUtilViewModel.SizeUnit.MB, "5" to PdfUtilViewModel.SizeUnit.MB, "10" to PdfUtilViewModel.SizeUnit.MB).forEach { (v, u) ->
                                            FilterChip(
                                                selected = (imageCustomTargetSizeText == v && imageCustomTargetUnit == u),
                                                onClick = { viewModel.setQuickImageTarget(v, u) },
                                                label = { Text("$v ${u.label}", fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Output Format
                                    Text("Format", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            ImageEngine.OutputFormat.JPEG to "JPEG",
                                            ImageEngine.OutputFormat.WEBP to "WEBP",
                                            ImageEngine.OutputFormat.PNG to "PNG"
                                        ).forEach { (fmt, label) ->
                                            FilterChip(
                                                selected = imageFormat == fmt,
                                                onClick = { viewModel.setImageCompressFormat(fmt) },
                                                label = { Text(label) }
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Quality slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Target Quality", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("$imageQuality%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SecondaryTeal)
                                    }
                                    Slider(
                                        value = imageQuality.toFloat(),
                                        onValueChange = { viewModel.setImageCompressQuality(it.toInt()) },
                                        valueRange = 10f..100f,
                                        modifier = Modifier.testTag("slider_compress_quality")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Scale ratio
                                    Text("Resolution Scale", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            1.0f to "100%",
                                            0.75f to "75%",
                                            0.5f to "50%",
                                            0.25f to "25%"
                                        ).forEach { (scale, label) ->
                                            FilterChip(
                                                selected = imageScale == scale,
                                                onClick = { viewModel.setImageCompressScale(scale) },
                                                label = { Text(label) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Output Format
                                    Text("Format", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            ImageEngine.OutputFormat.JPEG to "JPEG",
                                            ImageEngine.OutputFormat.WEBP to "WEBP",
                                            ImageEngine.OutputFormat.PNG to "PNG"
                                        ).forEach { (fmt, label) ->
                                            FilterChip(
                                                selected = imageFormat == fmt,
                                                onClick = { viewModel.setImageCompressFormat(fmt) },
                                                label = { Text(label) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Compress Button
                    item {
                        Button(
                            onClick = { viewModel.compressImage() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_run_compress_image"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                        ) {
                            Icon(Icons.Default.Compress, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (imageCompressMode == PdfUtilViewModel.CompressSizeMode.TARGET_SIZE)
                                    "Compress to $imageCustomTargetSizeText ${imageCustomTargetUnit.label}"
                                else
                                    "Compress Image Now",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Image Result Card
                if (compressedImageResult != null) {
                    item {
                        val beforeSize = sourceImageOriginalSize
                        val afterSize = compressedImageResult.length()
                        val diff = beforeSize - afterSize
                        val pctSaved = if (beforeSize > 0) ((diff.toDouble() / beforeSize.toDouble()) * 100).toInt() else 0

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Compression Complete!", fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 16.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SuccessGreen)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "-$pctSaved% Saved",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Before", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(ImageEngine.formatFileSize(beforeSize), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = SuccessGreen)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("After", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(ImageEngine.formatFileSize(afterSize), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SuccessGreen)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { FileOpener.openFile(context, compressedImageResult, "image/*") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                    ) {
                                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open")
                                    }

                                    OutlinedButton(
                                        onClick = { FileOpener.shareFile(context, compressedImageResult, "image/*") },
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
                // ==================== PDF COMPRESSOR TAB ====================
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_select_pdf_compress"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                                ) {
                                    Icon(imageVector = Icons.Default.Description, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pick PDF")
                                }

                                OutlinedButton(
                                    onClick = { viewModel.loadSamplePdfForCompress() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_sample_pdf_compress"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryIndigo)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Load Demo")
                                }
                            }

                            if (sourcePdfFile != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Selected: ${sourcePdfFile.name} (${ImageEngine.formatFileSize(sourcePdfFile.length())})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = PrimaryIndigo
                                )
                            }
                        }
                    }
                }

                if (sourcePdfFile != null) {
                    item {
                        // Mode Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = pdfCompressMode == PdfUtilViewModel.CompressSizeMode.TARGET_SIZE,
                                onClick = { viewModel.setPdfCompressMode(PdfUtilViewModel.CompressSizeMode.TARGET_SIZE) },
                                label = { Text("🎯 Custom Target Size") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryIndigo,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = pdfCompressMode == PdfUtilViewModel.CompressSizeMode.MANUAL_SLIDERS,
                                onClick = { viewModel.setPdfCompressMode(PdfUtilViewModel.CompressSizeMode.MANUAL_SLIDERS) },
                                label = { Text("⚙️ Preset Levels") }
                            )
                        }
                    }

                    if (pdfCompressMode == PdfUtilViewModel.CompressSizeMode.TARGET_SIZE) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Target PDF Size (Apne Hisab Se)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Aap jitna chahe utna exact size set kar sakte hain (e.g. 50 KB, 500 KB, 1 MB, 10 MB)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Input & Unit Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = pdfCustomTargetSizeText,
                                            onValueChange = { viewModel.setPdfCustomTargetSizeText(it) },
                                            label = { Text("Target Size") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("input_pdf_target_size")
                                        )

                                        // Unit Selector: KB vs MB
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            PdfUtilViewModel.SizeUnit.values().forEach { unit ->
                                                FilterChip(
                                                    selected = pdfCustomTargetUnit == unit,
                                                    onClick = { viewModel.setPdfCustomTargetUnit(unit) },
                                                    label = { Text(unit.label, fontWeight = FontWeight.Bold) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = PrimaryIndigo,
                                                        selectedLabelColor = Color.White
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Popular Quick Presets:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Preset row 1 (KB)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("50", "100", "200", "500").forEach { v ->
                                            FilterChip(
                                                selected = (pdfCustomTargetSizeText == v && pdfCustomTargetUnit == PdfUtilViewModel.SizeUnit.KB),
                                                onClick = { viewModel.setQuickPdfTarget(v, PdfUtilViewModel.SizeUnit.KB) },
                                                label = { Text("$v KB", fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Preset row 2 (MB)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("1", "2", "5", "10").forEach { v ->
                                            FilterChip(
                                                selected = (pdfCustomTargetSizeText == v && pdfCustomTargetUnit == PdfUtilViewModel.SizeUnit.MB),
                                                onClick = { viewModel.setQuickPdfTarget(v, PdfUtilViewModel.SizeUnit.MB) },
                                                label = { Text("$v MB", fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Text("Compression Level Presets", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        PdfUtilViewModel.PdfCompressPreset.values().forEach { preset ->
                            item {
                                OutlinedCard(
                                    onClick = { viewModel.setPdfCompressPreset(preset) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (pdfCompressPreset == preset) PrimaryIndigo.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = if (pdfCompressPreset == preset)
                                        androidx.compose.foundation.BorderStroke(2.dp, PrimaryIndigo)
                                    else
                                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(14.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(preset.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(preset.desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (pdfCompressPreset == preset) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryIndigo),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { viewModel.compressPdf() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_run_compress_pdf"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                        ) {
                            Icon(Icons.Default.Compress, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (pdfCompressMode == PdfUtilViewModel.CompressSizeMode.TARGET_SIZE)
                                    "Compress PDF to $pdfCustomTargetSizeText ${pdfCustomTargetUnit.label}"
                                else
                                    "Compress PDF Now",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // PDF Result Card
                if (compressedPdfResult != null && sourcePdfFile != null) {
                    item {
                        val beforeSize = sourcePdfFile.length()
                        val afterSize = compressedPdfResult.length()
                        val diff = beforeSize - afterSize
                        val pctSaved = if (beforeSize > 0) ((diff.toDouble() / beforeSize.toDouble()) * 100).toInt() else 0

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("PDF Compressed Successfully!", fontWeight = FontWeight.Bold, color = PrimaryIndigo, fontSize = 16.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PrimaryIndigo)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (pctSaved > 0) "-$pctSaved% Saved" else "Optimized",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Original PDF", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(ImageEngine.formatFileSize(beforeSize), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = PrimaryIndigo)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Compressed PDF", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(ImageEngine.formatFileSize(afterSize), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryIndigo)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { FileOpener.openFile(context, compressedPdfResult, "application/pdf") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                                    ) {
                                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open PDF")
                                    }

                                    OutlinedButton(
                                        onClick = { FileOpener.shareFile(context, compressedPdfResult, "application/pdf") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share PDF")
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
