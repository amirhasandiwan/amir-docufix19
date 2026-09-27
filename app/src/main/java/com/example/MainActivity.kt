package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.LoadingOverlay
import com.example.ui.screens.CompressorScreen
import com.example.ui.screens.EnhancerScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JpgToPdfScreen
import com.example.ui.screens.MergeSplitScreen
import com.example.ui.screens.PdfEditorScreen
import com.example.ui.screens.PdfToJpgScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.PdfUtilViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: PdfUtilViewModel = viewModel()) {
    val destination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val progressMessage by viewModel.progressMessage.collectAsStateWithLifecycle()
    val progressRatio by viewModel.progressRatio.collectAsStateWithLifecycle()
    val statusNotification by viewModel.statusNotification.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusNotification) {
        statusNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    // BackHandler to navigate back to Home if in any subscreen
    BackHandler(enabled = destination != AppDestination.HOME) {
        viewModel.navigateTo(AppDestination.HOME)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        if (isWideScreen) {
            // Adaptive Tablet Layout: Side NavigationRail + Content Pane
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.testTag("nav_rail")
                ) {
                    NavigationRailItem(
                        selected = destination == AppDestination.HOME,
                        onClick = { viewModel.navigateTo(AppDestination.HOME) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationRailItem(
                        selected = destination == AppDestination.JPG_TO_PDF,
                        onClick = { viewModel.navigateTo(AppDestination.JPG_TO_PDF) },
                        icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "JPG to PDF") },
                        label = { Text("JPG→PDF") }
                    )
                    NavigationRailItem(
                        selected = destination == AppDestination.PDF_TO_JPG,
                        onClick = { viewModel.navigateTo(AppDestination.PDF_TO_JPG) },
                        icon = { Icon(Icons.Default.Image, contentDescription = "PDF to JPG") },
                        label = { Text("PDF→JPG") }
                    )
                    NavigationRailItem(
                        selected = destination == AppDestination.COMPRESSOR,
                        onClick = { viewModel.navigateTo(AppDestination.COMPRESSOR) },
                        icon = { Icon(Icons.Default.Compress, contentDescription = "Compress") },
                        label = { Text("Compress") }
                    )
                    NavigationRailItem(
                        selected = destination == AppDestination.PAGE_EDITOR,
                        onClick = { viewModel.navigateTo(AppDestination.PAGE_EDITOR) },
                        icon = { Icon(Icons.Default.ViewCarousel, contentDescription = "Editor") },
                        label = { Text("Editor") }
                    )
                    NavigationRailItem(
                        selected = destination == AppDestination.ENHANCER,
                        onClick = { viewModel.navigateTo(AppDestination.ENHANCER) },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Enhance") },
                        label = { Text("Enhance") }
                    )
                    NavigationRailItem(
                        selected = destination == AppDestination.MERGE_SPLIT,
                        onClick = { viewModel.navigateTo(AppDestination.MERGE_SPLIT) },
                        icon = { Icon(Icons.Default.CallMerge, contentDescription = "Merge & Split") },
                        label = { Text("Merge/Split") }
                    )
                    NavigationRailItem(
                        selected = destination == AppDestination.SECURITY,
                        onClick = { viewModel.navigateTo(AppDestination.SECURITY) },
                        icon = { Icon(Icons.Default.Lock, contentDescription = "Security") },
                        label = { Text("Lock/Unlock") }
                    )
                    NavigationRailItem(
                        selected = destination == AppDestination.HISTORY,
                        onClick = { viewModel.navigateTo(AppDestination.HISTORY) },
                        icon = { Icon(Icons.Default.History, contentDescription = "Files") },
                        label = { Text("Files") }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ScreenSwitcher(destination, viewModel, historyList)
                }
            }
        } else {
            // Compact Phone Layout: Bottom NavigationBar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = destination == AppDestination.HOME,
                            onClick = { viewModel.navigateTo(AppDestination.HOME) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = destination == AppDestination.JPG_TO_PDF,
                            onClick = { viewModel.navigateTo(AppDestination.JPG_TO_PDF) },
                            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "JPG to PDF") },
                            label = { Text("JPG→PDF") }
                        )
                        NavigationBarItem(
                            selected = destination == AppDestination.PDF_TO_JPG,
                            onClick = { viewModel.navigateTo(AppDestination.PDF_TO_JPG) },
                            icon = { Icon(Icons.Default.Image, contentDescription = "PDF to JPG") },
                            label = { Text("PDF→JPG") }
                        )
                        NavigationBarItem(
                            selected = destination == AppDestination.COMPRESSOR,
                            onClick = { viewModel.navigateTo(AppDestination.COMPRESSOR) },
                            icon = { Icon(Icons.Default.Compress, contentDescription = "Compress") },
                            label = { Text("Compress") }
                        )
                        NavigationBarItem(
                            selected = destination == AppDestination.HISTORY,
                            onClick = { viewModel.navigateTo(AppDestination.HISTORY) },
                            icon = {
                                if (historyList.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text("${historyList.size}")
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.History, contentDescription = "Recent Files")
                                    }
                                } else {
                                    Icon(Icons.Default.History, contentDescription = "Recent Files")
                                }
                            },
                            label = { Text("Recent Files") }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ScreenSwitcher(destination, viewModel, historyList)
                }
            }
        }
    }

    if (isLoading) {
        LoadingOverlay(
            message = progressMessage,
            progressRatio = progressRatio
        )
    }
}

@Composable
fun ScreenSwitcher(
    destination: AppDestination,
    viewModel: PdfUtilViewModel,
    historyList: List<com.example.data.local.HistoryEntity>
) {
    val selectedImages by viewModel.selectedImages.collectAsStateWithLifecycle()
    val jpgFormat by viewModel.jpgToPdfPageFormat.collectAsStateWithLifecycle()
    val jpgOrientation by viewModel.jpgToPdfOrientation.collectAsStateWithLifecycle()
    val jpgMargin by viewModel.jpgToPdfMargin.collectAsStateWithLifecycle()
    val jpgQuality by viewModel.jpgToPdfQuality.collectAsStateWithLifecycle()
    val generatedPdf by viewModel.generatedPdfResult.collectAsStateWithLifecycle()

    val pdfToJpgFile by viewModel.pdfToJpgFile.collectAsStateWithLifecycle()
    val pdfThumbs by viewModel.pdfPageThumbnails.collectAsStateWithLifecycle()
    val pdfToJpgFormat by viewModel.pdfToJpgFormat.collectAsStateWithLifecycle()
    val pdfToJpgDpi by viewModel.pdfToJpgDpiScale.collectAsStateWithLifecycle()
    val extractedImages by viewModel.extractedImagesResult.collectAsStateWithLifecycle()

    val compressorTab by viewModel.compressorTab.collectAsStateWithLifecycle()
    val sourceBitmap by viewModel.sourceImageBitmap.collectAsStateWithLifecycle()
    val sourceImgSize by viewModel.sourceImageOriginalSize.collectAsStateWithLifecycle()
    val imgQuality by viewModel.imageCompressQuality.collectAsStateWithLifecycle()
    val imgScale by viewModel.imageCompressScale.collectAsStateWithLifecycle()
    val imgFormat by viewModel.imageCompressFormat.collectAsStateWithLifecycle()
    val compressedImg by viewModel.compressedImageResult.collectAsStateWithLifecycle()

    val sourcePdfForCompress by viewModel.sourcePdfForCompress.collectAsStateWithLifecycle()
    val pdfCompressPreset by viewModel.selectedPdfCompressPreset.collectAsStateWithLifecycle()
    val compressedPdf by viewModel.compressedPdfResult.collectAsStateWithLifecycle()

    val editorPdfFile by viewModel.editorPdfFile.collectAsStateWithLifecycle()
    val editorPageThumbs by viewModel.editorPageThumbnails.collectAsStateWithLifecycle()
    val editorPagesPlan by viewModel.editorPagesPlan.collectAsStateWithLifecycle()
    val editedPdfResult by viewModel.editedPdfResult.collectAsStateWithLifecycle()

    when (destination) {
        AppDestination.HOME -> HomeScreen(
            historyList = historyList,
            onNavigate = { viewModel.navigateTo(it) },
            onDeleteHistory = { viewModel.deleteHistoryItem(it) },
            onSeedSampleHistory = { viewModel.seedSampleHistory() }
        )
        AppDestination.JPG_TO_PDF -> JpgToPdfScreen(
            viewModel = viewModel,
            selectedImages = selectedImages,
            pageFormat = jpgFormat,
            orientation = jpgOrientation,
            marginPt = jpgMargin,
            quality = jpgQuality,
            resultFile = generatedPdf,
            onBack = { viewModel.navigateTo(AppDestination.HOME) }
        )
        AppDestination.PDF_TO_JPG -> PdfToJpgScreen(
            viewModel = viewModel,
            selectedPdfFile = pdfToJpgFile,
            pageThumbnails = pdfThumbs,
            outputFormat = pdfToJpgFormat,
            dpiScale = pdfToJpgDpi,
            extractedImages = extractedImages,
            onBack = { viewModel.navigateTo(AppDestination.HOME) }
        )
        AppDestination.COMPRESSOR -> CompressorScreen(
            viewModel = viewModel,
            currentTab = compressorTab,
            sourceBitmap = sourceBitmap,
            sourceImageOriginalSize = sourceImgSize,
            imageQuality = imgQuality,
            imageScale = imgScale,
            imageFormat = imgFormat,
            compressedImageResult = compressedImg,
            sourcePdfFile = sourcePdfForCompress,
            pdfCompressPreset = pdfCompressPreset,
            compressedPdfResult = compressedPdf,
            onBack = { viewModel.navigateTo(AppDestination.HOME) }
        )
        AppDestination.PAGE_EDITOR -> PdfEditorScreen(
            viewModel = viewModel,
            sourcePdfFile = editorPdfFile,
            pageThumbnails = editorPageThumbs,
            pagesPlan = editorPagesPlan,
            editedResultFile = editedPdfResult,
            onBack = { viewModel.navigateTo(AppDestination.HOME) }
        )
        AppDestination.HISTORY -> HistoryScreen(
            viewModel = viewModel,
            historyList = historyList,
            onBack = { viewModel.navigateTo(AppDestination.HOME) }
        )
        AppDestination.ENHANCER -> EnhancerScreen(
            viewModel = viewModel,
            onBack = { viewModel.navigateTo(AppDestination.HOME) }
        )
        AppDestination.MERGE_SPLIT -> MergeSplitScreen(
            viewModel = viewModel,
            onBack = { viewModel.navigateTo(AppDestination.HOME) }
        )
        AppDestination.SECURITY -> SecurityScreen(
            viewModel = viewModel,
            onBack = { viewModel.navigateTo(AppDestination.HOME) }
        )
    }
}
