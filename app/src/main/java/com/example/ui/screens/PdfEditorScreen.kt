package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppHeader
import com.example.ui.components.ConversionResultCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.viewmodel.PdfUtilViewModel
import com.example.utils.ImageEngine
import com.example.utils.PdfEngine
import java.io.File

@Composable
fun PdfEditorScreen(
    viewModel: PdfUtilViewModel,
    sourcePdfFile: File?,
    pageThumbnails: List<Bitmap>,
    pagesPlan: List<PdfEngine.PageEditInfo>,
    editedResultFile: File?,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectPdfUriForEditor(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pdf_editor_screen")
    ) {
        AppHeader(
            title = "PDF Page Editor",
            subtitle = "Reorder, rotate, delete, or extract pages",
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success Card if exported
            if (editedResultFile != null) {
                item {
                    ConversionResultCard(
                        file = editedResultFile,
                        operationName = "Edited PDF"
                    )
                }
            }

            // PDF Picker
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_select_pdf_editor"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose PDF")
                            }

                            OutlinedButton(
                                onClick = { viewModel.loadSamplePdfForEditor() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_sample_pdf_editor"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = AccentAmber)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Load Sample")
                            }
                        }

                        if (sourcePdfFile != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val activeCount = pagesPlan.count { !it.isDeleted }
                            Text(
                                text = "${sourcePdfFile.name} • $activeCount active pages",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AccentAmber
                            )
                        }
                    }
                }
            }

            // Page Editor Cards
            if (pagesPlan.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page Arrangement (${pagesPlan.count { !it.isDeleted }} / ${pagesPlan.size} Included)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                itemsIndexed(pagesPlan) { planIndex, pagePlan ->
                    val origIndex = pagePlan.originalPageIndex
                    val thumbnail = pageThumbnails.getOrNull(origIndex)

                    EditorPageItem(
                        planIndex = planIndex,
                        origPageNumber = origIndex + 1,
                        rotation = pagePlan.rotationDegrees,
                        isDeleted = pagePlan.isDeleted,
                        thumbnail = thumbnail,
                        canMoveUp = planIndex > 0,
                        canMoveDown = planIndex < pagesPlan.size - 1,
                        onRotate = { viewModel.rotateEditorPage(planIndex) },
                        onToggleDelete = { viewModel.toggleDeleteEditorPage(planIndex) },
                        onMoveUp = { viewModel.moveEditorPage(planIndex, planIndex - 1) },
                        onMoveDown = { viewModel.moveEditorPage(planIndex, planIndex + 1) }
                    )
                }

                // Export Button
                item {
                    val activePages = pagesPlan.count { !it.isDeleted }
                    Button(
                        onClick = { viewModel.exportEditedPdf() },
                        enabled = activePages > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("btn_export_edited_pdf"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Export Reorganized PDF ($activePages Pages)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorPageItem(
    planIndex: Int,
    origPageNumber: Int,
    rotation: Int,
    isDeleted: Boolean,
    thumbnail: Bitmap?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRotate: () -> Unit,
    onToggleDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("editor_page_$planIndex"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isDeleted) Color(0xFFF1F5F9).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Order badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isDeleted) Color.Gray else AccentAmber),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${planIndex + 1}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Thumbnail with rotation applied
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = "Original Page $origPageNumber",
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotation.toFloat()),
                        contentScale = ContentScale.Fit
                    )
                }
                if (isDeleted) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("EXCLUDED", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Page $origPageNumber",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isDeleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (rotation > 0) "Rotated ${rotation}°" else "0° Normal",
                    fontSize = 11.sp,
                    color = if (rotation > 0) AccentAmber else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Rotate button
            IconButton(
                onClick = onRotate,
                enabled = !isDeleted,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = "Rotate 90 degrees",
                    tint = AccentAmber,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Move Up
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp && !isDeleted,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Move Up",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Move Down
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown && !isDeleted,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Move Down",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete / Restore
            IconButton(
                onClick = onToggleDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isDeleted) Icons.Default.Restore else Icons.Default.Delete,
                    contentDescription = if (isDeleted) "Restore" else "Exclude",
                    tint = if (isDeleted) PrimaryIndigo else AccentCoral,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
