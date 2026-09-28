package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PdfExportSecurityConfig
import com.example.utils.FileOpener
import com.example.utils.ImageEngine
import com.example.utils.PdfEngine
import com.example.utils.SampleFilesProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class AppDestination {
    HOME,
    JPG_TO_PDF,
    PDF_TO_JPG,
    COMPRESSOR,
    MERGE_SPLIT,
    SECURITY,
    TEXT_TO_PDF
}

class PdfUtilViewModel(application: Application) : AndroidViewModel(application) {

    // Navigation & General UI State
    private val _currentDestination = MutableStateFlow(AppDestination.HOME)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _progressMessage = MutableStateFlow("")
    val progressMessage: StateFlow<String> = _progressMessage.asStateFlow()

    private val _progressRatio = MutableStateFlow(0f)
    val progressRatio: StateFlow<Float> = _progressRatio.asStateFlow()

    private val _statusNotification = MutableStateFlow<String?>(null)
    val statusNotification: StateFlow<String?> = _statusNotification.asStateFlow()

    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
    }

    fun clearNotification() {
        _statusNotification.value = null
    }

    // ==========================================
    // 1. JPG TO PDF STATE & ACTIONS
    // ==========================================
    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

    private val _jpgToPdfPageFormat = MutableStateFlow(PdfEngine.PageFormat.A4)
    val jpgToPdfPageFormat: StateFlow<PdfEngine.PageFormat> = _jpgToPdfPageFormat.asStateFlow()

    private val _jpgToPdfOrientation = MutableStateFlow(PdfEngine.OrientationMode.AUTO)
    val jpgToPdfOrientation: StateFlow<PdfEngine.OrientationMode> = _jpgToPdfOrientation.asStateFlow()

    private val _jpgToPdfMargin = MutableStateFlow(15) // points
    val jpgToPdfMargin: StateFlow<Int> = _jpgToPdfMargin.asStateFlow()

    private val _jpgToPdfQuality = MutableStateFlow(100) // 100% Original Quality
    val jpgToPdfQuality: StateFlow<Int> = _jpgToPdfQuality.asStateFlow()

    private val _jpgToPdfSecurityConfig = MutableStateFlow(PdfExportSecurityConfig())
    val jpgToPdfSecurityConfig: StateFlow<PdfExportSecurityConfig> = _jpgToPdfSecurityConfig.asStateFlow()

    private val _generatedPdfResult = MutableStateFlow<File?>(null)
    val generatedPdfResult: StateFlow<File?> = _generatedPdfResult.asStateFlow()

    fun setJpgToPdfSecurityConfig(config: PdfExportSecurityConfig) {
        _jpgToPdfSecurityConfig.value = config
    }

    fun addImageUris(uris: List<Uri>) {
        val cr = getApplication<Application>().contentResolver
        val filtered = uris.filter { uri ->
            val type = cr.getType(uri)?.lowercase() ?: ""
            val name = uri.lastPathSegment?.lowercase() ?: ""
            type.contains("jpeg") || type.contains("jpg") || name.endsWith(".jpg") || name.endsWith(".jpeg") || type.startsWith("image/")
        }
        _selectedImages.value = _selectedImages.value + (if (filtered.isNotEmpty()) filtered else uris)
    }

    fun removeImageAt(index: Int) {
        val current = _selectedImages.value.toMutableList()
        if (index in 0 until current.size) {
            current.removeAt(index)
            _selectedImages.value = current
        }
    }

    fun moveImage(from: Int, to: Int) {
        val current = _selectedImages.value.toMutableList()
        if (from in current.indices && to in current.indices) {
            val item = current.removeAt(from)
            current.add(to, item)
            _selectedImages.value = current
        }
    }

    fun clearSelectedImages() {
        _selectedImages.value = emptyList()
        _generatedPdfResult.value = null
    }

    fun setJpgToPdfFormat(format: PdfEngine.PageFormat) { _jpgToPdfPageFormat.value = format }
    fun setJpgToPdfOrientation(orientation: PdfEngine.OrientationMode) { _jpgToPdfOrientation.value = orientation }
    fun setJpgToPdfMargin(margin: Int) { _jpgToPdfMargin.value = margin }
    fun setJpgToPdfQuality(quality: Int) { _jpgToPdfQuality.value = quality }

    fun loadSampleImagesForJpgToPdf() {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Preparing sample test images..."
            val files = SampleFilesProvider.getOrCreateSampleImages(getApplication())
            val uris = files.map { SampleFilesProvider.getUriForFile(getApplication(), it) }
            _selectedImages.value = _selectedImages.value + uris
            _isLoading.value = false
            _statusNotification.value = "Loaded ${files.size} sample images for conversion"
        }
    }

    fun convertImagesToPdf() {
        val uris = _selectedImages.value
        if (uris.isEmpty()) return

        val security = _jpgToPdfSecurityConfig.value
        if (security.isProtectionEnabled && !security.isValid) {
            _statusNotification.value = security.errorMessage ?: "Please verify password configuration"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _progressRatio.value = 0f
            _progressMessage.value = "Converting ${uris.size} images to PDF..."

            try {
                val basePdfFile = PdfEngine.convertImagesToPdf(
                    context = getApplication(),
                    imageUris = uris,
                    pageFormat = _jpgToPdfPageFormat.value,
                    orientation = _jpgToPdfOrientation.value,
                    marginPt = _jpgToPdfMargin.value,
                    imageQuality = _jpgToPdfQuality.value,
                    outputFileName = "Doc_${System.currentTimeMillis()}.pdf",
                    onProgress = { current, total ->
                        _progressRatio.value = current.toFloat() / total.toFloat()
                        _progressMessage.value = "Processing image $current of $total..."
                    }
                )

                val finalPdfFile = if (security.isProtectionEnabled && security.userPassword.isNotEmpty()) {
                    _progressMessage.value = "Applying AES encryption & password lock..."
                    val perms = PdfEngine.PdfPermissions(
                        canPrint = security.canPrint,
                        canModify = security.canModify,
                        canExtractContent = security.canExtractContent,
                        canModifyAnnotations = security.canModifyAnnotations
                    )
                    PdfEngine.encryptPdf(
                        context = getApplication(),
                        inputPdf = basePdfFile,
                        userPassword = security.userPassword,
                        ownerPassword = security.ownerPassword.ifEmpty { security.userPassword },
                        permissions = perms,
                        keyLength = security.keyLength,
                        outputFileName = "Doc_${System.currentTimeMillis()}_protected.pdf"
                    )
                } else {
                    basePdfFile
                }

                _generatedPdfResult.value = finalPdfFile

                val badge = if (security.isProtectionEnabled) " 🔒 [Encrypted]" else ""
                _statusNotification.value = "Successfully generated ${finalPdfFile.name}$badge (${ImageEngine.formatFileSize(finalPdfFile.length())})"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Error converting images: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==========================================
    // 2. PDF TO JPG STATE & ACTIONS
    // ==========================================
    private val _pdfToJpgFile = MutableStateFlow<File?>(null)
    val pdfToJpgFile: StateFlow<File?> = _pdfToJpgFile.asStateFlow()

    private val _pdfPageThumbnails = MutableStateFlow<List<Bitmap>>(emptyList())
    val pdfPageThumbnails: StateFlow<List<Bitmap>> = _pdfPageThumbnails.asStateFlow()

    private val _pdfToJpgFormat = MutableStateFlow(ImageEngine.OutputFormat.JPEG)
    val pdfToJpgFormat: StateFlow<ImageEngine.OutputFormat> = _pdfToJpgFormat.asStateFlow()

    private val _pdfToJpgDpiScale = MutableStateFlow(4.167f) // 300 DPI (100% Original Print Fidelity)
    val pdfToJpgDpiScale: StateFlow<Float> = _pdfToJpgDpiScale.asStateFlow()

    private val _extractedImagesResult = MutableStateFlow<List<File>>(emptyList())
    val extractedImagesResult: StateFlow<List<File>> = _extractedImagesResult.asStateFlow()

    fun setPdfToJpgFormat(format: ImageEngine.OutputFormat) { _pdfToJpgFormat.value = format }
    fun setPdfToJpgDpiScale(scale: Float) { _pdfToJpgDpiScale.value = scale }

    fun selectPdfForExtract(file: File) {
        _pdfToJpgFile.value = file
        _extractedImagesResult.value = emptyList()
        loadPdfThumbnails(file)
    }

    fun selectPdfUriForExtract(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Reading PDF..."
            val file = copyUriToTempFile(uri, "input_${System.currentTimeMillis()}.pdf")
            _isLoading.value = false
            if (file != null) {
                selectPdfForExtract(file)
            }
        }
    }

    fun loadSamplePdfForExtract() {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Loading sample business report..."
            val sample = SampleFilesProvider.getOrCreateSamplePdf(getApplication())
            _isLoading.value = false
            selectPdfForExtract(sample)
            _statusNotification.value = "Loaded Sample Business Report (3 pages)"
        }
    }

    private fun loadPdfThumbnails(file: File) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Rendering page thumbnails..."
            val thumbs = PdfEngine.renderPdfPageThumbnails(file)
            _pdfPageThumbnails.value = thumbs
            _isLoading.value = false
        }
    }

    fun extractImagesFromPdf() {
        val file = _pdfToJpgFile.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _progressRatio.value = 0f
            _progressMessage.value = "Rendering pages to images..."

            try {
                val images = PdfEngine.convertPdfToImages(
                    context = getApplication(),
                    pdfFile = file,
                    format = _pdfToJpgFormat.value,
                    dpiScale = _pdfToJpgDpiScale.value,
                    quality = 100,
                    onProgress = { current, total ->
                        _progressRatio.value = current.toFloat() / total.toFloat()
                        _progressMessage.value = "Rendering page $current of $total (100% Original Quality)..."
                    }
                )

                _extractedImagesResult.value = images

                _statusNotification.value = "Extracted ${images.size} pages in 100% original quality as ${_pdfToJpgFormat.value.extension.uppercase()}"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Failed to render PDF: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==========================================
    // 3. COMPRESSOR STATE & ACTIONS
    // ==========================================
    enum class CompressorTab { IMAGE, PDF }
    private val _compressorTab = MutableStateFlow(CompressorTab.IMAGE)
    val compressorTab: StateFlow<CompressorTab> = _compressorTab.asStateFlow()

    fun setCompressorTab(tab: CompressorTab) { _compressorTab.value = tab }

    // Image Compressor
    // Compressor Common Enums
    enum class CompressSizeMode { TARGET_SIZE, MANUAL_SLIDERS }
    enum class SizeUnit(val label: String, val multiplier: Long) {
        KB("KB", 1024L),
        MB("MB", 1024L * 1024L)
    }

    private val _sourceImageBitmap = MutableStateFlow<Bitmap?>(null)
    val sourceImageBitmap: StateFlow<Bitmap?> = _sourceImageBitmap.asStateFlow()

    private val _sourceImageOriginalSize = MutableStateFlow(0L)
    val sourceImageOriginalSize: StateFlow<Long> = _sourceImageOriginalSize.asStateFlow()

    private val _imageCompressMode = MutableStateFlow(CompressSizeMode.TARGET_SIZE)
    val imageCompressMode: StateFlow<CompressSizeMode> = _imageCompressMode.asStateFlow()

    private val _imageCustomTargetSizeText = MutableStateFlow("500")
    val imageCustomTargetSizeText: StateFlow<String> = _imageCustomTargetSizeText.asStateFlow()

    private val _imageCustomTargetUnit = MutableStateFlow(SizeUnit.KB)
    val imageCustomTargetUnit: StateFlow<SizeUnit> = _imageCustomTargetUnit.asStateFlow()

    private val _imageCompressQuality = MutableStateFlow(70)
    val imageCompressQuality: StateFlow<Int> = _imageCompressQuality.asStateFlow()

    private val _imageCompressScale = MutableStateFlow(1.0f)
    val imageCompressScale: StateFlow<Float> = _imageCompressScale.asStateFlow()

    private val _imageCompressFormat = MutableStateFlow(ImageEngine.OutputFormat.JPEG)
    val imageCompressFormat: StateFlow<ImageEngine.OutputFormat> = _imageCompressFormat.asStateFlow()

    private val _compressedImageResult = MutableStateFlow<File?>(null)
    val compressedImageResult: StateFlow<File?> = _compressedImageResult.asStateFlow()

    fun setImageCompressMode(mode: CompressSizeMode) { _imageCompressMode.value = mode }
    fun setImageCustomTargetSizeText(text: String) { _imageCustomTargetSizeText.value = text.filter { it.isDigit() || it == '.' } }
    fun setImageCustomTargetUnit(unit: SizeUnit) { _imageCustomTargetUnit.value = unit }

    fun setQuickImageTarget(value: String, unit: SizeUnit) {
        _imageCompressMode.value = CompressSizeMode.TARGET_SIZE
        _imageCustomTargetSizeText.value = value
        _imageCustomTargetUnit.value = unit
    }

    fun setImageCompressQuality(q: Int) { _imageCompressQuality.value = q }
    fun setImageCompressScale(s: Float) { _imageCompressScale.value = s }
    fun setImageCompressFormat(f: ImageEngine.OutputFormat) { _imageCompressFormat.value = f }

    fun selectImageForCompression(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Analyzing image..."
            val pfd = getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")
            val size = pfd?.statSize ?: 0L
            pfd?.close()

            val bitmap = ImageEngine.decodeBitmapFromUri(getApplication(), uri, maxDimension = 3000)
            _sourceImageBitmap.value = bitmap
            _sourceImageOriginalSize.value = if (size > 0) size else (bitmap?.byteCount?.toLong() ?: 0L)
            _compressedImageResult.value = null
            _isLoading.value = false
        }
    }

    fun clearSourceImageForCompress() {
        _sourceImageBitmap.value = null
        _sourceImageOriginalSize.value = 0L
        _compressedImageResult.value = null
    }

    fun loadSampleImageForCompressor() {
        viewModelScope.launch {
            val samples = SampleFilesProvider.getOrCreateSampleImages(getApplication())
            if (samples.isNotEmpty()) {
                val uri = SampleFilesProvider.getUriForFile(getApplication(), samples.first())
                selectImageForCompression(uri)
                _statusNotification.value = "Loaded sample document image for compression test"
            }
        }
    }

    fun compressImage() {
        val bitmap = _sourceImageBitmap.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Compressing image..."
            try {
                val file: File
                if (_imageCompressMode.value == CompressSizeMode.TARGET_SIZE) {
                    val num = _imageCustomTargetSizeText.value.toDoubleOrNull() ?: 500.0
                    val unit = _imageCustomTargetUnit.value
                    val targetBytes = (num * unit.multiplier).toLong().coerceAtLeast(1024L)
                    val (out, _) = ImageEngine.compressToTargetSize(
                        context = getApplication(),
                        sourceBitmap = bitmap,
                        targetSizeBytes = targetBytes,
                        format = _imageCompressFormat.value
                    )
                    file = out
                } else {
                    file = ImageEngine.compressImage(
                        context = getApplication(),
                        sourceBitmap = bitmap,
                        quality = _imageCompressQuality.value,
                        scaleRatio = _imageCompressScale.value,
                        format = _imageCompressFormat.value
                    )
                }

                _compressedImageResult.value = file

                val isIncreased = file.length() > _sourceImageOriginalSize.value
                val status = if (isIncreased) {
                    "Image size increased to ${ImageEngine.formatFileSize(file.length())} (100% Quality Preserved)"
                } else {
                    val diff = _sourceImageOriginalSize.value - file.length()
                    val pct = if (_sourceImageOriginalSize.value > 0) ((diff.toDouble() / _sourceImageOriginalSize.value.toDouble()) * 100).toInt() else 0
                    "Image size reduced to ${ImageEngine.formatFileSize(file.length())} (${pct}% saved, Quality Preserved)"
                }

                _statusNotification.value = status
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Error compressing image: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // PDF Compressor
    private val _sourcePdfForCompress = MutableStateFlow<File?>(null)
    val sourcePdfForCompress: StateFlow<File?> = _sourcePdfForCompress.asStateFlow()

    private val _pdfCompressMode = MutableStateFlow(CompressSizeMode.TARGET_SIZE)
    val pdfCompressMode: StateFlow<CompressSizeMode> = _pdfCompressMode.asStateFlow()

    private val _pdfCustomTargetSizeText = MutableStateFlow("500")
    val pdfCustomTargetSizeText: StateFlow<String> = _pdfCustomTargetSizeText.asStateFlow()

    private val _pdfCustomTargetUnit = MutableStateFlow(SizeUnit.KB)
    val pdfCustomTargetUnit: StateFlow<SizeUnit> = _pdfCustomTargetUnit.asStateFlow()

    fun setPdfCompressMode(mode: CompressSizeMode) { _pdfCompressMode.value = mode }
    fun setPdfCustomTargetSizeText(text: String) { _pdfCustomTargetSizeText.value = text.filter { it.isDigit() || it == '.' } }
    fun setPdfCustomTargetUnit(unit: SizeUnit) { _pdfCustomTargetUnit.value = unit }

    fun setQuickPdfTarget(value: String, unit: SizeUnit) {
        _pdfCompressMode.value = CompressSizeMode.TARGET_SIZE
        _pdfCustomTargetSizeText.value = value
        _pdfCustomTargetUnit.value = unit
    }

    enum class PdfCompressPreset(val title: String, val desc: String, val dpiScale: Float, val quality: Int) {
        EXTREME("Compact Size", "Clear text (~144 DPI, 80% Quality)", 2.0f, 80),
        RECOMMENDED("Recommended", "Sharp print fidelity (~180 DPI, 88% Quality)", 2.5f, 88),
        HIGH_QUALITY("High Quality", "Maximum crystal clarity (~240 DPI, 95% Quality)", 3.33f, 95)
    }

    private val _selectedPdfCompressPreset = MutableStateFlow(PdfCompressPreset.RECOMMENDED)
    val selectedPdfCompressPreset: StateFlow<PdfCompressPreset> = _selectedPdfCompressPreset.asStateFlow()

    private val _compressPdfSecurityConfig = MutableStateFlow(PdfExportSecurityConfig())
    val compressPdfSecurityConfig: StateFlow<PdfExportSecurityConfig> = _compressPdfSecurityConfig.asStateFlow()

    fun setCompressPdfSecurityConfig(config: PdfExportSecurityConfig) {
        _compressPdfSecurityConfig.value = config
    }

    private val _compressedPdfResult = MutableStateFlow<File?>(null)
    val compressedPdfResult: StateFlow<File?> = _compressedPdfResult.asStateFlow()

    fun setPdfCompressPreset(preset: PdfCompressPreset) { _selectedPdfCompressPreset.value = preset }

    fun selectPdfForCompression(file: File) {
        _sourcePdfForCompress.value = file
        _compressedPdfResult.value = null
    }

    fun clearSourcePdfForCompress() {
        _sourcePdfForCompress.value = null
        _compressedPdfResult.value = null
    }

    fun selectPdfUriForCompression(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Reading PDF..."
            val file = copyUriToTempFile(uri, "compress_in_${System.currentTimeMillis()}.pdf")
            _isLoading.value = false
            if (file != null) {
                selectPdfForCompression(file)
            }
        }
    }

    fun loadSamplePdfForCompress() {
        viewModelScope.launch {
            val sample = SampleFilesProvider.getOrCreateSamplePdf(getApplication())
            selectPdfForCompression(sample)
            _statusNotification.value = "Loaded sample PDF for compression"
        }
    }

    fun compressPdf() {
        val file = _sourcePdfForCompress.value ?: return

        val security = _compressPdfSecurityConfig.value
        if (security.isProtectionEnabled && !security.isValid) {
            _statusNotification.value = security.errorMessage ?: "Please verify password configuration"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _progressRatio.value = 0f
            _progressMessage.value = "Optimizing and compressing PDF..."

            try {
                val rawCompressedFile: File
                if (_pdfCompressMode.value == CompressSizeMode.TARGET_SIZE) {
                    val num = _pdfCustomTargetSizeText.value.toDoubleOrNull() ?: 500.0
                    val unit = _pdfCustomTargetUnit.value
                    val targetBytes = (num * unit.multiplier).toLong().coerceAtLeast(1024L)
                    rawCompressedFile = PdfEngine.compressPdfToTargetSize(
                        context = getApplication(),
                        inputPdf = file,
                        targetSizeBytes = targetBytes,
                        onProgress = { cur, tot ->
                            _progressRatio.value = cur.toFloat() / tot.toFloat()
                            _progressMessage.value = "Compressing to target size ($cur/$tot)..."
                        }
                    )
                } else {
                    val preset = _selectedPdfCompressPreset.value
                    rawCompressedFile = PdfEngine.compressPdf(
                        context = getApplication(),
                        inputPdf = file,
                        dpiScale = preset.dpiScale,
                        imageQuality = preset.quality,
                        onProgress = { cur, tot ->
                            _progressRatio.value = cur.toFloat() / tot.toFloat()
                            _progressMessage.value = "Compressing page $cur of $tot..."
                        }
                    )
                }

                val finalCompressedFile = if (security.isProtectionEnabled && security.userPassword.isNotEmpty()) {
                    _progressMessage.value = "Applying AES encryption & password lock..."
                    val perms = PdfEngine.PdfPermissions(
                        canPrint = security.canPrint,
                        canModify = security.canModify,
                        canExtractContent = security.canExtractContent,
                        canModifyAnnotations = security.canModifyAnnotations
                    )
                    PdfEngine.encryptPdf(
                        context = getApplication(),
                        inputPdf = rawCompressedFile,
                        userPassword = security.userPassword,
                        ownerPassword = security.ownerPassword.ifEmpty { security.userPassword },
                        permissions = perms,
                        keyLength = security.keyLength,
                        outputFileName = "Compressed_${System.currentTimeMillis()}_protected.pdf"
                    )
                } else {
                    rawCompressedFile
                }

                _compressedPdfResult.value = finalCompressedFile

                val isIncreased = finalCompressedFile.length() > file.length()
                val badge = if (security.isProtectionEnabled) " 🔒 [Encrypted]" else ""
                val status = if (isIncreased) {
                    "PDF size increased to ${ImageEngine.formatFileSize(finalCompressedFile.length())}$badge (100% Quality Preserved)"
                } else {
                    val diff = file.length() - finalCompressedFile.length()
                    val pct = if (file.length() > 0) ((diff.toDouble() / file.length().toDouble()) * 100).toInt() else 0
                    "PDF size reduced to ${ImageEngine.formatFileSize(finalCompressedFile.length())}$badge (${pct}% saved, Quality Preserved)"
                }
                _statusNotification.value = status
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Error compressing PDF: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==========================================
    // 7. PDF MERGE & PDF SPLIT STATE & ACTIONS
    // ==========================================
    enum class MergeSplitTab { MERGE, SPLIT }
    private val _mergeSplitTab = MutableStateFlow(MergeSplitTab.MERGE)
    val mergeSplitTab: StateFlow<MergeSplitTab> = _mergeSplitTab.asStateFlow()

    fun setMergeSplitTab(tab: MergeSplitTab) { _mergeSplitTab.value = tab }

    // --- PDF MERGE ---
    private val _mergePdfList = MutableStateFlow<List<File>>(emptyList())
    val mergePdfList: StateFlow<List<File>> = _mergePdfList.asStateFlow()

    private val _mergePdfSecurityConfig = MutableStateFlow(PdfExportSecurityConfig())
    val mergePdfSecurityConfig: StateFlow<PdfExportSecurityConfig> = _mergePdfSecurityConfig.asStateFlow()

    private val _mergedPdfResult = MutableStateFlow<File?>(null)
    val mergedPdfResult: StateFlow<File?> = _mergedPdfResult.asStateFlow()

    fun setMergePdfSecurityConfig(config: PdfExportSecurityConfig) {
        _mergePdfSecurityConfig.value = config
    }

    fun addPdfUrisForMerge(uris: List<Uri>) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Importing PDFs for merging..."
            val files = mutableListOf<File>()
            uris.forEachIndexed { idx, uri ->
                val f = copyUriToTempFile(uri, "merge_${System.currentTimeMillis()}_$idx.pdf")
                if (f != null) files.add(f)
            }
            _mergePdfList.value = _mergePdfList.value + files
            _mergedPdfResult.value = null
            _isLoading.value = false
            _statusNotification.value = "Added ${files.size} PDF(s) to merge list"
        }
    }

    fun loadSamplePdfsForMerge() {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Loading sample business PDFs..."
            val samples = SampleFilesProvider.getOrCreateMultipleSamplePdfs(getApplication())
            _mergePdfList.value = samples
            _mergedPdfResult.value = null
            _isLoading.value = false
            _statusNotification.value = "Loaded ${samples.size} sample PDFs ready to merge"
        }
    }

    fun removePdfFromMerge(index: Int) {
        val list = _mergePdfList.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _mergePdfList.value = list
            _mergedPdfResult.value = null
        }
    }

    fun movePdfInMerge(from: Int, to: Int) {
        val list = _mergePdfList.value.toMutableList()
        if (from in list.indices && to in list.indices && from != to) {
            val item = list.removeAt(from)
            list.add(to, item)
            _mergePdfList.value = list
            _mergedPdfResult.value = null
        }
    }

    fun setPdfPositionInMerge(fromIndex: Int, targetPosition: Int) {
        // targetPosition is 0-indexed
        val list = _mergePdfList.value.toMutableList()
        if (fromIndex in list.indices && targetPosition in list.indices && fromIndex != targetPosition) {
            val item = list.removeAt(fromIndex)
            list.add(targetPosition, item)
            _mergePdfList.value = list
            _mergedPdfResult.value = null
        }
    }

    fun reverseMergePdfList() {
        val list = _mergePdfList.value.reversed()
        _mergePdfList.value = list
        _mergedPdfResult.value = null
    }

    fun sortMergePdfsByName(ascending: Boolean = true) {
        val list = if (ascending) {
            _mergePdfList.value.sortedBy { it.name.lowercase() }
        } else {
            _mergePdfList.value.sortedByDescending { it.name.lowercase() }
        }
        _mergePdfList.value = list
        _mergedPdfResult.value = null
    }

    fun sortMergePdfsBySize(ascending: Boolean = true) {
        val list = if (ascending) {
            _mergePdfList.value.sortedBy { it.length() }
        } else {
            _mergePdfList.value.sortedByDescending { it.length() }
        }
        _mergePdfList.value = list
        _mergedPdfResult.value = null
    }

    fun clearMergePdfs() {
        _mergePdfList.value = emptyList()
        _mergedPdfResult.value = null
    }

    fun runMergePdfs() {
        val pdfs = _mergePdfList.value
        if (pdfs.size < 2) {
            _statusNotification.value = "Please select at least 2 PDF files to merge"
            return
        }

        val security = _mergePdfSecurityConfig.value
        if (security.isProtectionEnabled && !security.isValid) {
            _statusNotification.value = security.errorMessage ?: "Please verify password configuration"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _progressRatio.value = 0f
            _progressMessage.value = "Merging ${pdfs.size} PDFs..."

            try {
                val rawMergedFile = PdfEngine.mergePdfs(
                    context = getApplication(),
                    inputPdfs = pdfs,
                    onProgress = { cur, tot, pages ->
                        _progressRatio.value = cur.toFloat() / tot.toFloat()
                        _progressMessage.value = "Merging document $cur of $tot..."
                    }
                )

                val finalMergedFile = if (security.isProtectionEnabled && security.userPassword.isNotEmpty()) {
                    _progressMessage.value = "Applying AES encryption & password lock..."
                    val perms = PdfEngine.PdfPermissions(
                        canPrint = security.canPrint,
                        canModify = security.canModify,
                        canExtractContent = security.canExtractContent,
                        canModifyAnnotations = security.canModifyAnnotations
                    )
                    PdfEngine.encryptPdf(
                        context = getApplication(),
                        inputPdf = rawMergedFile,
                        userPassword = security.userPassword,
                        ownerPassword = security.ownerPassword.ifEmpty { security.userPassword },
                        permissions = perms,
                        keyLength = security.keyLength,
                        outputFileName = "Merged_${System.currentTimeMillis()}_protected.pdf"
                    )
                } else {
                    rawMergedFile
                }

                _mergedPdfResult.value = finalMergedFile

                val badge = if (security.isProtectionEnabled) " 🔒 [Encrypted]" else ""
                _statusNotification.value = "Successfully merged ${pdfs.size} PDFs$badge (${ImageEngine.formatFileSize(finalMergedFile.length())})"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Merge failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Applies password protection to any exported PDF file directly.
     */
    fun protectAnyExportedPdf(
        file: File,
        config: PdfExportSecurityConfig,
        onSuccess: (File) -> Unit = {}
    ) {
        if (!config.isValid || config.userPassword.isEmpty()) {
            _statusNotification.value = config.errorMessage ?: "Please enter a valid password"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Encrypting PDF with password..."

            try {
                val perms = PdfEngine.PdfPermissions(
                    canPrint = config.canPrint,
                    canModify = config.canModify,
                    canExtractContent = config.canExtractContent,
                    canModifyAnnotations = config.canModifyAnnotations
                )
                val protectedFile = PdfEngine.encryptPdf(
                    context = getApplication(),
                    inputPdf = file,
                    userPassword = config.userPassword,
                    ownerPassword = config.ownerPassword.ifEmpty { config.userPassword },
                    permissions = perms,
                    keyLength = config.keyLength,
                    outputFileName = "${file.nameWithoutExtension}_protected.pdf"
                )

                _statusNotification.value = "Successfully password protected ${protectedFile.name}"
                onSuccess(protectedFile)
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Protection failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- PDF SPLIT ---
    private val _splitSourcePdf = MutableStateFlow<File?>(null)
    val splitSourcePdf: StateFlow<File?> = _splitSourcePdf.asStateFlow()

    private val _splitPageThumbnails = MutableStateFlow<List<Bitmap>>(emptyList())
    val splitPageThumbnails: StateFlow<List<Bitmap>> = _splitPageThumbnails.asStateFlow()

    private val _splitSelectedPageIndices = MutableStateFlow<Set<Int>>(emptySet())
    val splitSelectedPageIndices: StateFlow<Set<Int>> = _splitSelectedPageIndices.asStateFlow()

    private val _splitPdfResult = MutableStateFlow<File?>(null)
    val splitPdfResult: StateFlow<File?> = _splitPdfResult.asStateFlow()

    private val _splitMultipleResults = MutableStateFlow<List<File>>(emptyList())
    val splitMultipleResults: StateFlow<List<File>> = _splitMultipleResults.asStateFlow()

    fun selectPdfForSplit(file: File) {
        _splitSourcePdf.value = file
        _splitPdfResult.value = null
        _splitMultipleResults.value = emptyList()
        _splitSelectedPageIndices.value = emptySet()
        loadSplitPdfThumbnails(file)
    }

    fun selectPdfUriForSplit(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Opening PDF for splitting..."
            val file = copyUriToTempFile(uri, "split_in_${System.currentTimeMillis()}.pdf")
            _isLoading.value = false
            if (file != null) {
                selectPdfForSplit(file)
            }
        }
    }

    fun loadSamplePdfForSplit() {
        viewModelScope.launch {
            val sample = SampleFilesProvider.getOrCreateSamplePdf(getApplication())
            selectPdfForSplit(sample)
            _statusNotification.value = "Loaded Sample Business Report (3 pages) for splitting"
        }
    }

    private fun loadSplitPdfThumbnails(file: File) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Rendering page thumbnails..."
            try {
                val thumbs = PdfEngine.renderPdfPageThumbnails(file, maxPages = 50)
                _splitPageThumbnails.value = thumbs
                if (thumbs.isNotEmpty()) {
                    _splitSelectedPageIndices.value = setOf(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleSplitPageSelection(pageIndex: Int) {
        val current = _splitSelectedPageIndices.value.toMutableSet()
        if (current.contains(pageIndex)) {
            current.remove(pageIndex)
        } else {
            current.add(pageIndex)
        }
        _splitSelectedPageIndices.value = current
    }

    fun selectAllSplitPages() {
        val total = _splitPageThumbnails.value.size
        _splitSelectedPageIndices.value = (0 until total).toSet()
    }

    fun clearSplitPageSelection() {
        _splitSelectedPageIndices.value = emptySet()
    }

    fun clearSplitPdf() {
        _splitSourcePdf.value = null
        _splitPageThumbnails.value = emptyList()
        _splitSelectedPageIndices.value = emptySet()
        _splitPdfResult.value = null
        _splitMultipleResults.value = emptyList()
    }

    fun splitSelectedPages() {
        val file = _splitSourcePdf.value ?: return
        val indices = _splitSelectedPageIndices.value.toList().sorted()
        if (indices.isEmpty()) {
            _statusNotification.value = "Please select at least 1 page to extract"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _progressRatio.value = 0f
            _progressMessage.value = "Extracting ${indices.size} pages..."

            try {
                val extracted = PdfEngine.splitPdf(
                    context = getApplication(),
                    inputPdf = file,
                    selectedPageIndices = indices,
                    onProgress = { cur, tot ->
                        _progressRatio.value = cur.toFloat() / tot.toFloat()
                        _progressMessage.value = "Extracting page $cur of $tot..."
                    }
                )

                _splitPdfResult.value = extracted
                _splitMultipleResults.value = emptyList()

                _statusNotification.value = "Extracted ${indices.size} pages into ${extracted.name} (${ImageEngine.formatFileSize(extracted.length())})"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Split failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun splitAllPagesToIndividualPdfs() {
        val file = _splitSourcePdf.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _progressRatio.value = 0f
            _progressMessage.value = "Splitting into individual single-page PDFs..."

            try {
                val files = PdfEngine.splitPdfAllPages(
                    context = getApplication(),
                    inputPdf = file,
                    onProgress = { cur, tot ->
                        _progressRatio.value = cur.toFloat() / tot.toFloat()
                        _progressMessage.value = "Creating page $cur of $tot..."
                    }
                )

                _splitMultipleResults.value = files
                _splitPdfResult.value = null

                _statusNotification.value = "Split into ${files.size} single-page PDFs"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Split failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==========================================
    // 8. PDF SECURITY: ENCRYPTION & DECRYPTION
    // ==========================================

    enum class SecurityTab {
        ENCRYPT,
        DECRYPT
    }

    private val _securityTab = MutableStateFlow(SecurityTab.ENCRYPT)
    val securityTab: StateFlow<SecurityTab> = _securityTab.asStateFlow()

    // Encrypt State
    private val _securityEncryptFile = MutableStateFlow<File?>(null)
    val securityEncryptFile: StateFlow<File?> = _securityEncryptFile.asStateFlow()

    private val _securityEncryptInfo = MutableStateFlow<PdfEngine.PdfSecurityInfo?>(null)
    val securityEncryptInfo: StateFlow<PdfEngine.PdfSecurityInfo?> = _securityEncryptInfo.asStateFlow()

    private val _securityUserPassword = MutableStateFlow("")
    val securityUserPassword: StateFlow<String> = _securityUserPassword.asStateFlow()

    private val _securityOwnerPassword = MutableStateFlow("")
    val securityOwnerPassword: StateFlow<String> = _securityOwnerPassword.asStateFlow()

    private val _securityUseSamePassword = MutableStateFlow(true)
    val securityUseSamePassword: StateFlow<Boolean> = _securityUseSamePassword.asStateFlow()

    private val _securityKeyLength = MutableStateFlow(128)
    val securityKeyLength: StateFlow<Int> = _securityKeyLength.asStateFlow()

    private val _securityCanPrint = MutableStateFlow(true)
    val securityCanPrint: StateFlow<Boolean> = _securityCanPrint.asStateFlow()

    private val _securityCanModify = MutableStateFlow(false)
    val securityCanModify: StateFlow<Boolean> = _securityCanModify.asStateFlow()

    private val _securityCanExtractContent = MutableStateFlow(true)
    val securityCanExtractContent: StateFlow<Boolean> = _securityCanExtractContent.asStateFlow()

    private val _securityCanModifyAnnotations = MutableStateFlow(true)
    val securityCanModifyAnnotations: StateFlow<Boolean> = _securityCanModifyAnnotations.asStateFlow()

    private val _securityEncryptResult = MutableStateFlow<File?>(null)
    val securityEncryptResult: StateFlow<File?> = _securityEncryptResult.asStateFlow()

    // Decrypt / Remove Password State
    private val _securityDecryptFile = MutableStateFlow<File?>(null)
    val securityDecryptFile: StateFlow<File?> = _securityDecryptFile.asStateFlow()

    private val _securityDecryptInfo = MutableStateFlow<PdfEngine.PdfSecurityInfo?>(null)
    val securityDecryptInfo: StateFlow<PdfEngine.PdfSecurityInfo?> = _securityDecryptInfo.asStateFlow()

    private val _securityDecryptPassword = MutableStateFlow("")
    val securityDecryptPassword: StateFlow<String> = _securityDecryptPassword.asStateFlow()

    private val _securityDecryptResult = MutableStateFlow<File?>(null)
    val securityDecryptResult: StateFlow<File?> = _securityDecryptResult.asStateFlow()

    private val _securityDecryptError = MutableStateFlow<String?>(null)
    val securityDecryptError: StateFlow<String?> = _securityDecryptError.asStateFlow()

    fun setSecurityTab(tab: SecurityTab) {
        _securityTab.value = tab
    }

    fun setSecurityUserPassword(password: String) {
        _securityUserPassword.value = password
    }

    fun setSecurityOwnerPassword(password: String) {
        _securityOwnerPassword.value = password
    }

    fun setSecurityUseSamePassword(useSame: Boolean) {
        _securityUseSamePassword.value = useSame
    }

    fun setSecurityKeyLength(bits: Int) {
        _securityKeyLength.value = bits
    }

    fun setSecurityCanPrint(allowed: Boolean) {
        _securityCanPrint.value = allowed
    }

    fun setSecurityCanModify(allowed: Boolean) {
        _securityCanModify.value = allowed
    }

    fun setSecurityCanExtractContent(allowed: Boolean) {
        _securityCanExtractContent.value = allowed
    }

    fun setSecurityCanModifyAnnotations(allowed: Boolean) {
        _securityCanModifyAnnotations.value = allowed
    }

    fun setSecurityEncryptFile(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Analyzing PDF document..."
            try {
                val tempFile = copyUriToTempFile(uri, "encrypt_target_${System.currentTimeMillis()}.pdf")
                if (tempFile != null) {
                    _securityEncryptFile.value = tempFile
                    _securityEncryptResult.value = null
                    val info = PdfEngine.checkPdfSecurity(getApplication(), tempFile)
                    _securityEncryptInfo.value = info
                } else {
                    _statusNotification.value = "Failed to load selected PDF"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Error reading PDF: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSamplePdfForEncrypt() {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Loading sample business report..."
            try {
                val sample = SampleFilesProvider.getOrCreateSamplePdf(getApplication())
                _securityEncryptFile.value = sample
                _securityEncryptResult.value = null
                val info = PdfEngine.checkPdfSecurity(getApplication(), sample)
                _securityEncryptInfo.value = info
                _statusNotification.value = "Loaded: ${sample.name}"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Failed to load sample: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSecurityEncryptFile() {
        _securityEncryptFile.value = null
        _securityEncryptInfo.value = null
        _securityEncryptResult.value = null
    }

    fun encryptPdfAction() {
        val file = _securityEncryptFile.value ?: run {
            _statusNotification.value = "Please select a PDF file first"
            return
        }
        val userPass = _securityUserPassword.value
        val ownerPass = if (_securityUseSamePassword.value) userPass else _securityOwnerPassword.value

        if (userPass.isBlank() && ownerPass.isBlank()) {
            _statusNotification.value = "Please enter an open password or owner password"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Applying cryptographic protection..."
            _progressRatio.value = 0.3f
            try {
                val permissions = PdfEngine.PdfPermissions(
                    canPrint = _securityCanPrint.value,
                    canModify = _securityCanModify.value,
                    canExtractContent = _securityCanExtractContent.value,
                    canModifyAnnotations = _securityCanModifyAnnotations.value,
                    canAssembleDocument = _securityCanModify.value
                )

                _progressRatio.value = 0.6f
                val protectedFile = PdfEngine.encryptPdf(
                    context = getApplication(),
                    inputPdf = file,
                    userPassword = userPass,
                    ownerPassword = ownerPass,
                    permissions = permissions,
                    keyLength = _securityKeyLength.value
                )

                _progressRatio.value = 0.9f
                _securityEncryptResult.value = protectedFile

                _statusNotification.value = "PDF encrypted & locked successfully!"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Encryption failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _progressRatio.value = 0f
            }
        }
    }

    // Decrypt / Remove Password Methods
    fun setSecurityDecryptPassword(password: String) {
        _securityDecryptPassword.value = password
        _securityDecryptError.value = null
    }

    fun setSecurityDecryptFile(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Analyzing PDF encryption..."
            try {
                val tempFile = copyUriToTempFile(uri, "decrypt_target_${System.currentTimeMillis()}.pdf")
                if (tempFile != null) {
                    _securityDecryptFile.value = tempFile
                    _securityDecryptResult.value = null
                    _securityDecryptError.value = null
                    val info = PdfEngine.checkPdfSecurity(getApplication(), tempFile)
                    _securityDecryptInfo.value = info
                } else {
                    _statusNotification.value = "Failed to load PDF file"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Error reading PDF: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSamplePdfForDecrypt() {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Generating encrypted sample PDF (pass: 1234)..."
            try {
                val sample = SampleFilesProvider.getOrCreateEncryptedSamplePdf(getApplication(), "1234")
                _securityDecryptFile.value = sample
                _securityDecryptResult.value = null
                _securityDecryptError.value = null
                _securityDecryptPassword.value = "1234"
                val info = PdfEngine.checkPdfSecurity(getApplication(), sample)
                _securityDecryptInfo.value = info
                _statusNotification.value = "Loaded encrypted sample (Password: 1234)"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Failed to create encrypted sample: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSecurityDecryptFile() {
        _securityDecryptFile.value = null
        _securityDecryptInfo.value = null
        _securityDecryptResult.value = null
        _securityDecryptPassword.value = ""
        _securityDecryptError.value = null
    }

    fun decryptPdfAction() {
        val file = _securityDecryptFile.value ?: run {
            _statusNotification.value = "Please select a password-protected PDF"
            return
        }
        val pass = _securityDecryptPassword.value
        if (pass.isBlank()) {
            _securityDecryptError.value = "Password cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Unlocking and removing encryption..."
            _securityDecryptError.value = null
            _progressRatio.value = 0.4f
            try {
                val unlockedFile = PdfEngine.decryptPdf(
                    context = getApplication(),
                    inputPdf = file,
                    password = pass
                )

                _progressRatio.value = 0.8f
                _securityDecryptResult.value = unlockedFile

                _statusNotification.value = "Password removed! Unlocked PDF is ready."
            } catch (e: IllegalArgumentException) {
                _securityDecryptError.value = e.message ?: "Incorrect password. Unable to unlock PDF."
                _statusNotification.value = e.message ?: "Incorrect password"
            } catch (e: Exception) {
                e.printStackTrace()
                _securityDecryptError.value = "Decryption error: ${e.localizedMessage}"
                _statusNotification.value = "Failed to decrypt: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _progressRatio.value = 0f
            }
        }
    }

    // ==========================================
    // 8. TEXT TO PDF STATE & ACTIONS
    // ==========================================
    private val _textToPdfTitle = MutableStateFlow("")
    val textToPdfTitle: StateFlow<String> = _textToPdfTitle.asStateFlow()

    private val _textToPdfContent = MutableStateFlow("")
    val textToPdfContent: StateFlow<String> = _textToPdfContent.asStateFlow()

    private val _textToPdfResult = MutableStateFlow<File?>(null)
    val textToPdfResult: StateFlow<File?> = _textToPdfResult.asStateFlow()

    fun setTextToPdfTitle(title: String) {
        _textToPdfTitle.value = title
    }

    fun setTextToPdfContent(content: String) {
        _textToPdfContent.value = content
    }

    fun loadTextFromUri(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Reading text file..."
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val text = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                _textToPdfContent.value = text
                if (_textToPdfTitle.value.isBlank()) {
                    var name = "Document"
                    val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (idx != -1) name = it.getString(idx)
                        }
                    }
                    _textToPdfTitle.value = name.substringBeforeLast(".")
                }
                _statusNotification.value = "Text file loaded successfully"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Error reading text file: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearTextToPdf() {
        _textToPdfTitle.value = ""
        _textToPdfContent.value = ""
        _textToPdfResult.value = null
    }

    fun convertTextToPdfAction() {
        val content = _textToPdfContent.value
        if (content.isBlank()) {
            _statusNotification.value = "Please enter text or upload a text file"
            return
        }

        val title = _textToPdfTitle.value.ifBlank { "Document" }

        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Generating formatted PDF..."
            _progressRatio.value = 0.5f
            try {
                val file = PdfEngine.convertTextToPdf(
                    context = getApplication(),
                    title = title,
                    content = content
                )
                _textToPdfResult.value = file
                _statusNotification.value = "PDF created successfully: ${file.name}"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Error creating PDF: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _progressRatio.value = 0f
            }
        }
    }

    // Helper: copy content uri to temp file
    private suspend fun copyUriToTempFile(uri: Uri, tempName: String): File? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val file = File(context.cacheDir, tempName)
                FileOutputStream(file).use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()
                file
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}
