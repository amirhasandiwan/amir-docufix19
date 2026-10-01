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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ViewCarousel
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
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JpgToPdfScreen
import com.example.ui.screens.MergeSplitScreen
import com.example.ui.screens.PdfToJpgScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.TextToPdfScreen
import com.example.ui.screens.BgRemoverScreen
import com.example.ui.screens.OcrScreen
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
                        icon = { Icon(Icons.Default.Compress, contentDescription = "Resize") },
                        label = { Text("Resize") }
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
                }

                Box(modifier = Modifier.weight(1f)) {
                    ScreenSwitcher(destination, viewModel)
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
                            icon = { Icon(Icons.Default.Compress, contentDescription = "Resize") },
                            label = { Text("Resize") }
                        )
                        NavigationBarItem(
                            selected = destination == AppDestination.SECURITY,
                            onClick = { viewModel.navigateTo(AppDestination.SECURITY) },
                            icon = { Icon(Icons.Default.Lock, contentDescription = "Security") },
                            label = { Text("Security") }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ScreenSwitcher(destination, viewModel)
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
    viewModel: PdfUtilViewModel
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

    when (destination) {
        AppDestination.HOME -> HomeScreen(
            onNavigate = { viewModel.navigateTo(it) }
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
        AppDestination.TEXT_TO_PDF -> TextToPdfScreen(
            viewModel = viewModel,
            onBack = { viewModel.navigateTo(AppDestination.HOME) }
        )
        AppDestination.BG_REMOVER -> BgRemoverScreen(
            viewModel = viewModel,
            onBack = { viewModel.navigateTo(AppDestination.HOME) }
        )
        AppDestination.OCR -> OcrScreen(
            viewModel = viewModel,
            onBack = { viewModel.navigateTo(AppDestination.HOME) }
        )
    }
}
