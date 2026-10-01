package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppHeader
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.PdfUtilViewModel
import com.example.utils.BgRemovalEngine
import com.example.utils.FileOpener
import com.example.utils.ImageEngine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BgRemoverScreen(
    viewModel: PdfUtilViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val sourceBitmap by viewModel.bgSourceBitmap.collectAsStateWithLifecycle()
    val previewBitmap by viewModel.bgPreviewBitmap.collectAsStateWithLifecycle()
    val selectedColor by viewModel.bgSelectedColor.collectAsStateWithLifecycle()
    val customHexColor by viewModel.bgCustomHexColor.collectAsStateWithLifecycle()
    val removalMode by viewModel.bgRemovalMode.collectAsStateWithLifecycle()
    val threshold by viewModel.bgThreshold.collectAsStateWithLifecycle()
    val savedFile by viewModel.bgSavedFile.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isBgProcessing.collectAsStateWithLifecycle()

    var showOriginal by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var customR by remember { mutableIntStateOf(59) }
    var customG by remember { mutableIntStateOf(130) }
    var customB by remember { mutableIntStateOf(246) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.selectImageForBgRemoval(uri)
        }
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("bg_remover_screen")
    ) {
        AppHeader(
            title = "Photo BG Remover & Color",
            subtitle = "Remove background & apply any custom color",
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Upload Photo Button
            item {
                Button(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_upload_photo_bg_remover"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (sourceBitmap == null) "Upload Photo" else "Change Photo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            if (sourceBitmap != null) {
                // 2. Interactive Preview Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFE11D48),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (showOriginal) "Original Photo" else "Background Removed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = { showOriginal = !showOriginal },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (showOriginal) "Show Edited" else "Compare", fontSize = 11.sp)
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { viewModel.clearBgRemoval() },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Preview Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selectedColor != null && !showOriginal) Color(selectedColor!!)
                                        else Color(0xFFF1F5F9)
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val displayBitmap = if (showOriginal) sourceBitmap else previewBitmap

                                if (isProcessing) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Color(0xFFE11D48), strokeWidth = 3.dp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Removing Background...",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else if (displayBitmap != null) {
                                    Image(
                                        bitmap = displayBitmap.asImageBitmap(),
                                        contentDescription = "Photo Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Background Color Selector Card ("backgroung me koi bhi colour apne hisab se select kar sake")
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ColorLens, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Select Background Color",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Text(
                                text = "Transparent rakhein ya koi bhi manchaha color choose karein:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Preset Palette Swatches
                            val colorPresets = listOf(
                                null to "Transparent",
                                android.graphics.Color.WHITE to "White",
                                android.graphics.Color.parseColor("#0D47A1") to "Passport Blue",
                                android.graphics.Color.parseColor("#3B82F6") to "Sky Blue",
                                android.graphics.Color.parseColor("#DC2626") to "Red",
                                android.graphics.Color.parseColor("#18181B") to "Black",
                                android.graphics.Color.parseColor("#9CA3AF") to "Gray",
                                android.graphics.Color.parseColor("#059669") to "Green",
                                android.graphics.Color.parseColor("#F59E0B") to "Yellow",
                                android.graphics.Color.parseColor("#8B5CF6") to "Purple",
                                android.graphics.Color.parseColor("#EC4899") to "Pink"
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(colorPresets) { (colorValue, label) ->
                                    val isSelected = selectedColor == colorValue
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable {
                                            viewModel.setBgColor(colorValue)
                                            if (colorValue != null) {
                                                customR = android.graphics.Color.red(colorValue)
                                                customG = android.graphics.Color.green(colorValue)
                                                customB = android.graphics.Color.blue(colorValue)
                                            }
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (colorValue == null) Color.LightGray.copy(alpha = 0.5f)
                                                    else Color(colorValue)
                                                )
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFFE11D48) else Color.Gray.copy(alpha = 0.4f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (colorValue == null) {
                                                Text("PNG", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.DarkGray)
                                            } else if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = if (colorValue == android.graphics.Color.WHITE || colorValue == android.graphics.Color.parseColor("#F59E0B")) Color.Black else Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom Color Adjuster (RGB Sliders & Hex Input)
                            Text(
                                text = "🎨 Custom Color Mixer (Koi Bhi Color Banayein):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Live Custom Color Swatch & Hex input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(customR, customG, customB))
                                        .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                )

                                OutlinedTextField(
                                    value = customHexColor,
                                    onValueChange = {
                                        viewModel.setCustomHexColor(it)
                                    },
                                    label = { Text("Color HEX Code") },
                                    placeholder = { Text("#3B82F6") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_custom_color_hex")
                                )

                                Button(
                                    onClick = {
                                        val c = android.graphics.Color.rgb(customR, customG, customB)
                                        viewModel.setBgColor(c)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Red Slider
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("R", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Red, modifier = Modifier.width(20.dp))
                                Slider(
                                    value = customR.toFloat(),
                                    onValueChange = {
                                        customR = it.toInt()
                                        val c = android.graphics.Color.rgb(customR, customG, customB)
                                        viewModel.setBgColor(c)
                                    },
                                    valueRange = 0f..255f,
                                    colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red.copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f)
                                )
                                Text("$customR", fontSize = 11.sp, modifier = Modifier.width(30.dp))
                            }

                            // Green Slider
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("G", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF059669), modifier = Modifier.width(20.dp))
                                Slider(
                                    value = customG.toFloat(),
                                    onValueChange = {
                                        customG = it.toInt()
                                        val c = android.graphics.Color.rgb(customR, customG, customB)
                                        viewModel.setBgColor(c)
                                    },
                                    valueRange = 0f..255f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF059669), activeTrackColor = Color(0xFF059669).copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f)
                                )
                                Text("$customG", fontSize = 11.sp, modifier = Modifier.width(30.dp))
                            }

                            // Blue Slider
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("B", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Blue, modifier = Modifier.width(20.dp))
                                Slider(
                                    value = customB.toFloat(),
                                    onValueChange = {
                                        customB = it.toInt()
                                        val c = android.graphics.Color.rgb(customR, customG, customB)
                                        viewModel.setBgColor(c)
                                    },
                                    valueRange = 0f..255f,
                                    colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue.copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f)
                                )
                                Text("$customB", fontSize = 11.sp, modifier = Modifier.width(30.dp))
                            }
                        }
                    }
                }

                // 4. Edge Refinement & Detection Mode (Optional Fine-Tuning)
                item {
                    OutlinedButton(
                        onClick = { showAdvancedSettings = !showAdvancedSettings },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showAdvancedSettings) "Hide Edge Sensitivity" else "Fine-Tune Edge Sensitivity", fontSize = 12.sp)
                    }

                    if (showAdvancedSettings) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Edge Smoothness & Threshold", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Slider(
                                    value = threshold,
                                    onValueChange = { viewModel.setBgThreshold(it) },
                                    valueRange = 0.1f..0.9f
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Softer Edge", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Sharper Edge", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Detection Engine", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = removalMode == BgRemovalEngine.RemovalMode.AI_PORTRAIT,
                                        onClick = { viewModel.setBgRemovalMode(BgRemovalEngine.RemovalMode.AI_PORTRAIT) },
                                        label = { Text("AI Portrait", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = removalMode == BgRemovalEngine.RemovalMode.COLOR_KEY,
                                        onClick = { viewModel.setBgRemovalMode(BgRemovalEngine.RemovalMode.COLOR_KEY) },
                                        label = { Text("Color Detection", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Save & Download Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveBgRemovedPhoto(isPng = true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_save_png_photo"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save as PNG", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.saveBgRemovedPhoto(isPng = false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_save_jpg_photo"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save as JPG", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // 6. Download Complete Result Card
                if (savedFile != null) {
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
                                        text = "Photo Saved Successfully!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = SuccessGreen
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = savedFile!!.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${ImageEngine.formatFileSize(savedFile!!.length())} • Ready to use",
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
                                            val mime = if (savedFile!!.name.endsWith(".png")) "image/png" else "image/jpeg"
                                            FileOpener.openFile(context, savedFile!!, mime)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                    ) {
                                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Photo")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val mime = if (savedFile!!.name.endsWith(".png")) "image/png" else "image/jpeg"
                                            FileOpener.shareFile(context, savedFile!!, mime)
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
