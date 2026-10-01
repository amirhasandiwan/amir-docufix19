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
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.components.AllDocumentHistorySheet
import com.example.ui.components.OfflinePrivacyBanner
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.AppDestination
import com.example.utils.ImageEngine

@Composable
fun HomeScreen(
    onNavigate: (AppDestination) -> Unit
) {
    val context = LocalContext.current
    var showHistorySheet by remember { mutableStateOf(false) }

    if (showHistorySheet) {
        AllDocumentHistorySheet(
            onDismiss = { showHistorySheet = false }
        )
    }

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

                        // Right side of header: All Document History
                        Surface(
                            onClick = { showHistorySheet = true },
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .testTag("btn_all_document_history")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "All Document History",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "All Document",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 13.sp
                                    )
                                    Text(
                                        text = "History",
                                        color = Color(0xFFBFDBFE),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OfflinePrivacyBanner()
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
                description = "Upload JPG images and convert to high-quality PDF with 100% original quality.",
                badgeText = "100% Original Quality",
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
                title = "PDF to JPG",
                description = "Upload PDF and extract pages into crystal-clear JPG with 100% original quality.",
                badgeText = "100% Original Quality",
                icon = Icons.Default.Image,
                accentColor = AccentCoral,
                bgColor = Color(0xFFFFF1F2),
                testTag = "card_pdf_to_jpg",
                onClick = { onNavigate(AppDestination.PDF_TO_JPG) }
            )
        }

        // Utility: Text to PDF
        item {
            FeatureCard(
                title = "Text to PDF Converter",
                description = "Text type/paste karein ya .txt file upload karke high-quality PDF generate karein.",
                badgeText = "Text to PDF",
                icon = Icons.Default.TextFields,
                accentColor = Color(0xFF7C3AED),
                bgColor = Color(0xFFF5F3FF),
                testTag = "card_text_to_pdf",
                onClick = { onNavigate(AppDestination.TEXT_TO_PDF) }
            )
        }

        // Utility: Photo BG Remover & Color Changer
        item {
            FeatureCard(
                title = "Photo BG Remover & Color",
                description = "Photo upload karke uska background remove karein aur koi bhi custom color choose karein.",
                badgeText = "Remove & Color BG",
                icon = Icons.Default.AutoAwesome,
                accentColor = Color(0xFFE11D48),
                bgColor = Color(0xFFFFF1F2),
                testTag = "card_bg_remover",
                onClick = { onNavigate(AppDestination.BG_REMOVER) }
            )
        }

        // Utility: OCR Text Extractor & Searchable PDF
        item {
            FeatureCard(
                title = "OCR Text Extractor",
                description = "Image se text extract karein aur Searchable PDF ya text file (.txt) me save karein.",
                badgeText = "Searchable PDF & OCR",
                icon = Icons.Default.DocumentScanner,
                accentColor = Color(0xFF0284C7),
                bgColor = Color(0xFFF0F9FF),
                testTag = "card_ocr",
                onClick = { onNavigate(AppDestination.OCR) }
            )
        }

        // Utility 3: Custom Resizer
        item {
            FeatureCard(
                title = "Custom Image & PDF Resizer",
                description = "Image ya PDF upload karke file size optimize aur resize karein.",
                badgeText = "Image & PDF",
                icon = Icons.Default.Compress,
                accentColor = SecondaryTeal,
                bgColor = Color(0xFFF0FDFA),
                testTag = "card_compressor",
                onClick = { onNavigate(AppDestination.COMPRESSOR) }
            )
        }

        // Utility 4: PDF Merge & PDF Split
        item {
            FeatureCard(
                title = "PDF Merge & PDF Split",
                description = "Multiple PDF files upload karke merge karein ya PDF upload karke pages split karein.",
                badgeText = "Merge & Split",
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
                description = "PDF me password lock lagayein ya locked PDF me se password lock hatayein.",
                badgeText = "Lock / Unlock",
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
