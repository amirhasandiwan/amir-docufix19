package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.HistoryEntity
import com.example.ui.components.OfflinePrivacyBanner
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.AppDestination
import com.example.utils.FileOpener
import com.example.utils.ImageEngine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    historyList: List<HistoryEntity>,
    onNavigate: (AppDestination) -> Unit,
    onDeleteHistory: (HistoryEntity) -> Unit,
    onSeedSampleHistory: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF0D9488))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Amir DocuFix",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "AMIR HASAN DIWAN",
                                color = Color(0xFFBFDBFE),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OfflinePrivacyBanner()
                }
            }
        }

        // Quick Navigation Pills (Recent Files Log & Flutter Specs)
        item {
            OutlinedCard(
                onClick = { onNavigate(AppDestination.HISTORY) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (historyList.isNotEmpty()) PrimaryIndigo.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_history_nav")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SecondaryTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = SecondaryTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Recent Files & History Log", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (historyList.isNotEmpty()) "${historyList.size} conversions saved in Room DB" else "View local conversion log & offline files",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ==========================================
        // PROMINENT RECENT FILES SECTION
        // ==========================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (historyList.isNotEmpty()) {
                    Text(
                        text = "View Log (${historyList.size}) →",
                        color = PrimaryIndigo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onNavigate(AppDestination.HISTORY) }
                            .padding(4.dp)
                    )
                }
            }
        }

        if (historyList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Room Database History Log",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Converted or edited PDFs and images automatically save here for instant 1-tap offline access.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onSeedSampleHistory,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Demo Files", fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            // Horizontal Quick-Access Carousel of Recent Files
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(historyList.take(6)) { item ->
                        RecentFileQuickCard(
                            entity = item,
                            onOpen = {
                                val file = File(item.filePath)
                                if (file.exists()) {
                                    FileOpener.openFile(
                                        context,
                                        file,
                                        if (item.fileType.equals("PDF", ignoreCase = true)) "application/pdf" else "image/*"
                                    )
                                }
                            },
                            onShare = {
                                val file = File(item.filePath)
                                if (file.exists()) {
                                    FileOpener.shareFile(
                                        context,
                                        file,
                                        if (item.fileType.equals("PDF", ignoreCase = true)) "application/pdf" else "image/*"
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Section Title: Core Utilities
        item {
            Text(
                text = "Core Utilities",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Utility 1: JPG to PDF
        item {
            FeatureCard(
                title = "JPG to PDF",
                description = "Convert multiple images to single or multi-page PDF. Custom margins, page size & orientation.",
                badgeText = "Multi-Image",
                icon = Icons.Default.PictureAsPdf,
                accentColor = PrimaryIndigo,
                bgColor = Color(0xFFEFF6FF),
                testTag = "card_jpg_to_pdf",
                onClick = { onNavigate(AppDestination.JPG_TO_PDF) }
            )
        }

        // Utility 2: PDF to JPG
        item {
            FeatureCard(
                title = "PDF to JPG / PNG",
                description = "Extract pages into crisp high-resolution images. Multi-page preview, DPI scaling & bulk share.",
                badgeText = "High DPI",
                icon = Icons.Default.Image,
                accentColor = AccentCoral,
                bgColor = Color(0xFFFFF1F2),
                testTag = "card_pdf_to_jpg",
                onClick = { onNavigate(AppDestination.PDF_TO_JPG) }
            )
        }

        // Utility 3: Custom Compressor
        item {
            FeatureCard(
                title = "Custom Image & PDF Compressor",
                description = "Shrink file size with live quality sliders, resolution scaling, and target file size calculation.",
                badgeText = "Up to 80% Smaller",
                icon = Icons.Default.Compress,
                accentColor = SecondaryTeal,
                bgColor = Color(0xFFF0FDFA),
                testTag = "card_compressor",
                onClick = { onNavigate(AppDestination.COMPRESSOR) }
            )
        }

        // Utility 4: PDF Page Editor
        item {
            FeatureCard(
                title = "PDF Page Editor",
                description = "Reorganize pages, rotate orientations (90°/180°), delete unwanted pages, and extract subsets.",
                badgeText = "Interactive",
                icon = Icons.Default.ViewCarousel,
                accentColor = AccentAmber,
                bgColor = Color(0xFFFFFBEB),
                testTag = "card_page_editor",
                onClick = { onNavigate(AppDestination.PAGE_EDITOR) }
            )
        }

        // Utility 5: Size & Quality Enhancer
        item {
            FeatureCard(
                title = "Size & Quality Enhancer",
                description = "Upscale resolution up to 4x, boost text/edge sharpness, enhance contrast, and satisfy portal minimum file size rules.",
                badgeText = "Super-Res & DPI",
                icon = Icons.Default.AutoAwesome,
                accentColor = Color(0xFF8B5CF6),
                bgColor = Color(0xFFF5F3FF),
                testTag = "card_enhancer",
                onClick = { onNavigate(AppDestination.ENHANCER) }
            )
        }

        // Utility 6: PDF Merge & PDF Split
        item {
            FeatureCard(
                title = "PDF Merge & PDF Split",
                description = "Combine multiple PDF documents in exact order, or extract selected pages and split into individual files.",
                badgeText = "Combine & Extract",
                icon = Icons.Default.CallMerge,
                accentColor = Color(0xFF0284C7),
                bgColor = Color(0xFFF0F9FF),
                testTag = "card_merge_split",
                onClick = { onNavigate(AppDestination.MERGE_SPLIT) }
            )
        }

        // Utility 7: PDF Lock & Unlock (Security / Encrypt & Decrypt)
        item {
            FeatureCard(
                title = "PDF Lock & Unlock",
                description = "Encrypt PDF files with open passwords & owner permission restrictions (print, copy, edit), or permanently remove passwords.",
                badgeText = "AES Encryption & Unlock",
                icon = Icons.Default.Lock,
                accentColor = PrimaryIndigo,
                bgColor = Color(0xFFEEF2FF),
                testTag = "card_security",
                onClick = { onNavigate(AppDestination.SECURITY) }
            )
        }
    }
}

@Composable
fun RecentFileQuickCard(
    entity: HistoryEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val isPdf = entity.fileType.equals("PDF", ignoreCase = true)
    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(entity.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(180.dp)
            .clickable { onOpen() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isPdf) PrimaryIndigo.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPdf) Icons.Default.Description else Icons.Default.Image,
                        contentDescription = null,
                        tint = if (isPdf) PrimaryIndigo else SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = entity.fileType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPdf) PrimaryIndigo else SuccessGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = entity.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )

            Text(
                text = "${ImageEngine.formatFileSize(entity.sizeBytes)} • $dateStr",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isPdf) PrimaryIndigo else SuccessGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    description: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
