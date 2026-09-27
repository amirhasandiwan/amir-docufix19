package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.HistoryEntity
import com.example.data.repository.HistoryRepository
import com.example.utils.FileOpener
import com.example.utils.ImageEngine
import com.example.utils.PdfEngine
import com.example.utils.SampleFilesProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class AppDestination {
    HOME,
    JPG_TO_PDF,
    PDF_TO_JPG,
    COMPRESSOR,
    PAGE_EDITOR,
    ENHANCER,
    MERGE_SPLIT,
    SECURITY,
    HISTORY
}

class PdfUtilViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HistoryRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = HistoryRepository(db.historyDao())
    }

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

    // Room History
    val historyList: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalSizeBytes: StateFlow<Long> = repository.totalSizeBytes
        .map { it ?: 0L }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

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

    private val _jpgToPdfQuality = MutableStateFlow(85) // %
    val jpgToPdfQuality: StateFlow<Int> = _jpgToPdfQuality.asStateFlow()

    private val _generatedPdfResult = MutableStateFlow<File?>(null)
    val generatedPdfResult: StateFlow<File?> = _generatedPdfResult.asStateFlow()

    fun addImageUris(uris: List<Uri>) {
        _selectedImages.value = _selectedImages.value + uris
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

        viewModelScope.launch {
            _isLoading.value = true
            _progressRatio.value = 0f
            _progressMessage.value = "Converting ${uris.size} images to PDF..."

            try {
                val pdfFile = PdfEngine.convertImagesToPdf(
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

                _generatedPdfResult.value = pdfFile

                // Save to Room
                repository.insert(
                    HistoryEntity(
                        title = pdfFile.name,
                        filePath = pdfFile.absolutePath,
                        fileType = "PDF",
                        operationType = "JPG to PDF",
                        sizeBytes = pdfFile.length(),
                        pageCount = uris.size
                    )
                )

                _statusNotification.value = "Successfully generated ${pdfFile.name} (${ImageEngine.formatFileSize(pdfFile.length())})"
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

    private val _pdfToJpgDpiScale = MutableStateFlow(2.0f) // 144 DPI
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
                    onProgress = { current, total ->
                        _progressRatio.value = current.toFloat() / total.toFloat()
                        _progressMessage.value = "Rendering page $current of $total..."
                    }
                )

                _extractedImagesResult.value = images

                if (images.isNotEmpty()) {
                    val totalSize = images.sumOf { it.length() }
                    repository.insert(
                        HistoryEntity(
                            title = "${file.nameWithoutExtension}_pages",
                            filePath = images.first().absolutePath,
                            fileType = _pdfToJpgFormat.value.extension.uppercase(),
                            operationType = "PDF to JPG",
                            sizeBytes = totalSize,
                            pageCount = images.size
                        )
                    )
                }

                _statusNotification.value = "Extracted ${images.size} pages as ${_pdfToJpgFormat.value.extension.uppercase()}"
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

                repository.insert(
                    HistoryEntity(
                        title = file.name,
                        filePath = file.absolutePath,
                        fileType = _imageCompressFormat.value.extension.uppercase(),
                        operationType = "Compressed",
                        sizeBytes = file.length(),
                        pageCount = 1
                    )
                )

                val savedPercent = if (_sourceImageOriginalSize.value > 0) {
                    val diff = _sourceImageOriginalSize.value - file.length()
                    val pct = (diff.toDouble() / _sourceImageOriginalSize.value.toDouble() * 100).toInt()
                    "$pct% size reduction"
                } else ""

                _statusNotification.value = "Image compressed: ${ImageEngine.formatFileSize(file.length())} ($savedPercent)"
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
        EXTREME("Extreme Compression", "Smallest size (~96 DPI, 50% Quality)", 1.33f, 50),
        RECOMMENDED("Recommended", "Good balance (~144 DPI, 70% Quality)", 2.0f, 70),
        HIGH_QUALITY("High Quality", "Crisp text (~200 DPI, 85% Quality)", 2.77f, 85)
    }

    private val _selectedPdfCompressPreset = MutableStateFlow(PdfCompressPreset.RECOMMENDED)
    val selectedPdfCompressPreset: StateFlow<PdfCompressPreset> = _selectedPdfCompressPreset.asStateFlow()

    private val _compressedPdfResult = MutableStateFlow<File?>(null)
    val compressedPdfResult: StateFlow<File?> = _compressedPdfResult.asStateFlow()

    fun setPdfCompressPreset(preset: PdfCompressPreset) { _selectedPdfCompressPreset.value = preset }

    fun selectPdfForCompression(file: File) {
        _sourcePdfForCompress.value = file
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

        viewModelScope.launch {
            _isLoading.value = true
            _progressRatio.value = 0f
            _progressMessage.value = "Optimizing and compressing PDF..."

            try {
                val compressedFile: File
                if (_pdfCompressMode.value == CompressSizeMode.TARGET_SIZE) {
                    val num = _pdfCustomTargetSizeText.value.toDoubleOrNull() ?: 500.0
                    val unit = _pdfCustomTargetUnit.value
                    val targetBytes = (num * unit.multiplier).toLong().coerceAtLeast(1024L)
                    compressedFile = PdfEngine.compressPdfToTargetSize(
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
                    compressedFile = PdfEngine.compressPdf(
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

                _compressedPdfResult.value = compressedFile

                repository.insert(
                    HistoryEntity(
                        title = compressedFile.name,
                        filePath = compressedFile.absolutePath,
                        fileType = "PDF",
                        operationType = "Compressed",
                        sizeBytes = compressedFile.length(),
                        pageCount = 1
                    )
                )

                val diff = file.length() - compressedFile.length()
                val pct = if (file.length() > 0) ((diff.toDouble() / file.length().toDouble()) * 100).toInt() else 0
                _statusNotification.value = "PDF Compressed! New size: ${ImageEngine.formatFileSize(compressedFile.length())} (${pct}% saved)"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Error compressing PDF: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==========================================
    // 4. PDF PAGE EDITOR STATE & ACTIONS
    // ==========================================
    private val _editorPdfFile = MutableStateFlow<File?>(null)
    val editorPdfFile: StateFlow<File?> = _editorPdfFile.asStateFlow()

    private val _editorPageThumbnails = MutableStateFlow<List<Bitmap>>(emptyList())
    val editorPageThumbnails: StateFlow<List<Bitmap>> = _editorPageThumbnails.asStateFlow()

    private val _editorPagesPlan = MutableStateFlow<List<PdfEngine.PageEditInfo>>(emptyList())
    val editorPagesPlan: StateFlow<List<PdfEngine.PageEditInfo>> = _editorPagesPlan.asStateFlow()

    private val _editedPdfResult = MutableStateFlow<File?>(null)
    val editedPdfResult: StateFlow<File?> = _editedPdfResult.asStateFlow()

    fun selectPdfForEditor(file: File) {
        _editorPdfFile.value = file
        _editedPdfResult.value = null
        loadEditorPages(file)
    }

    fun selectPdfUriForEditor(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Loading PDF for editor..."
            val file = copyUriToTempFile(uri, "editor_${System.currentTimeMillis()}.pdf")
            _isLoading.value = false
            if (file != null) {
                selectPdfForEditor(file)
            }
        }
    }

    fun loadSamplePdfForEditor() {
        viewModelScope.launch {
            val sample = SampleFilesProvider.getOrCreateSamplePdf(getApplication())
            selectPdfForEditor(sample)
            _statusNotification.value = "Loaded 3-page sample report into PDF Editor"
        }
    }

    private fun loadEditorPages(file: File) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Loading page layout..."
            val thumbs = PdfEngine.renderPdfPageThumbnails(file)
            _editorPageThumbnails.value = thumbs
            _editorPagesPlan.value = thumbs.indices.map {
                PdfEngine.PageEditInfo(originalPageIndex = it, rotationDegrees = 0, isDeleted = false)
            }
            _isLoading.value = false
        }
    }

    fun rotateEditorPage(planIndex: Int) {
        val list = _editorPagesPlan.value.toMutableList()
        if (planIndex in list.indices) {
            val item = list[planIndex]
            list[planIndex] = item.copy(rotationDegrees = (item.rotationDegrees + 90) % 360)
            _editorPagesPlan.value = list
        }
    }

    fun toggleDeleteEditorPage(planIndex: Int) {
        val list = _editorPagesPlan.value.toMutableList()
        if (planIndex in list.indices) {
            val item = list[planIndex]
            list[planIndex] = item.copy(isDeleted = !item.isDeleted)
            _editorPagesPlan.value = list
        }
    }

    fun moveEditorPage(from: Int, to: Int) {
        val list = _editorPagesPlan.value.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from)
            list.add(to, item)
            _editorPagesPlan.value = list
        }
    }

    fun exportEditedPdf() {
        val file = _editorPdfFile.value ?: return
        val plan = _editorPagesPlan.value
        val activeCount = plan.count { !it.isDeleted }
        if (activeCount == 0) {
            _statusNotification.value = "Cannot export: All pages are marked as deleted"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _progressRatio.value = 0f
            _progressMessage.value = "Exporting reorganized PDF ($activeCount pages)..."

            try {
                val exported = PdfEngine.exportEditedPdf(
                    context = getApplication(),
                    inputPdf = file,
                    pagesPlan = plan,
                    onProgress = { cur, tot ->
                        _progressRatio.value = cur.toFloat() / tot.toFloat()
                        _progressMessage.value = "Saving page $cur of $tot..."
                    }
                )

                _editedPdfResult.value = exported

                repository.insert(
                    HistoryEntity(
                        title = exported.name,
                        filePath = exported.absolutePath,
                        fileType = "PDF",
                        operationType = "Page Editor",
                        sizeBytes = exported.length(),
                        pageCount = activeCount
                    )
                )

                _statusNotification.value = "Exported ${exported.name} ($activeCount pages, ${ImageEngine.formatFileSize(exported.length())})"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Error exporting PDF: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==========================================
    // 5. HISTORY & FILE MANAGEMENT
    // ==========================================
    fun toggleFavorite(entity: HistoryEntity) {
        viewModelScope.launch {
            repository.update(entity.copy(isFavorite = !entity.isFavorite))
        }
    }

    fun deleteHistoryItem(entity: HistoryEntity) {
        viewModelScope.launch {
            repository.delete(entity)
            _statusNotification.value = "Deleted ${entity.title}"
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _statusNotification.value = "History cleared"
        }
    }

    fun seedSampleHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Creating sample history records..."
            try {
                val samplePdf = SampleFilesProvider.getOrCreateSamplePdf(getApplication())
                val sampleImages = SampleFilesProvider.getOrCreateSampleImages(getApplication())

                val pdfEntry = HistoryEntity(
                    title = samplePdf.name,
                    filePath = samplePdf.absolutePath,
                    fileType = "PDF",
                    operationType = "JPG to PDF",
                    sizeBytes = samplePdf.length(),
                    pageCount = 3,
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 15,
                    isFavorite = true
                )

                val imgEntries = sampleImages.mapIndexed { idx, file ->
                    HistoryEntity(
                        title = file.name,
                        filePath = file.absolutePath,
                        fileType = "JPG",
                        operationType = if (idx == 0) "PDF to JPG" else "Compressed",
                        sizeBytes = file.length(),
                        pageCount = 1,
                        timestamp = System.currentTimeMillis() - 1000 * 60 * (30 * (idx + 1)),
                        isFavorite = false
                    )
                }

                repository.insert(pdfEntry)
                repository.insertAll(imgEntries)
                _statusNotification.value = "Sample conversion files added to Room history"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Failed to populate history: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==========================================
    // 6. IMAGE & PDF SIZE ENHANCER STATE & ACTIONS
    // ==========================================
    enum class EnhancerTab { IMAGE, PDF }
    private val _enhancerTab = MutableStateFlow(EnhancerTab.IMAGE)
    val enhancerTab: StateFlow<EnhancerTab> = _enhancerTab.asStateFlow()

    fun setEnhancerTab(tab: EnhancerTab) { _enhancerTab.value = tab }

    // Image Enhancer State
    private val _enhanceSourceImage = MutableStateFlow<Bitmap?>(null)
    val enhanceSourceImage: StateFlow<Bitmap?> = _enhanceSourceImage.asStateFlow()

    private val _enhanceSourceImageOriginalSize = MutableStateFlow(0L)
    val enhanceSourceImageOriginalSize: StateFlow<Long> = _enhanceSourceImageOriginalSize.asStateFlow()

    private val _enhanceImageScale = MutableStateFlow(2.0f) // 2x upscale
    val enhanceImageScale: StateFlow<Float> = _enhanceImageScale.asStateFlow()

    private val _enhanceImageSharpness = MutableStateFlow(0.5f)
    val enhanceImageSharpness: StateFlow<Float> = _enhanceImageSharpness.asStateFlow()

    private val _enhanceImageContrast = MutableStateFlow(1.15f)
    val enhanceImageContrast: StateFlow<Float> = _enhanceImageContrast.asStateFlow()

    private val _enhanceImageTargetMinKb = MutableStateFlow(0) // 0 = no min, e.g. 500 = 500 KB
    val enhanceImageTargetMinKb: StateFlow<Int> = _enhanceImageTargetMinKb.asStateFlow()

    private val _enhanceImageFormat = MutableStateFlow(ImageEngine.OutputFormat.JPEG)
    val enhanceImageFormat: StateFlow<ImageEngine.OutputFormat> = _enhanceImageFormat.asStateFlow()

    private val _enhancedImageResult = MutableStateFlow<File?>(null)
    val enhancedImageResult: StateFlow<File?> = _enhancedImageResult.asStateFlow()

    private val _enhancedImageResultBitmap = MutableStateFlow<Bitmap?>(null)
    val enhancedImageResultBitmap: StateFlow<Bitmap?> = _enhancedImageResultBitmap.asStateFlow()

    fun setEnhanceImageScale(scale: Float) { _enhanceImageScale.value = scale }
    fun setEnhanceImageSharpness(s: Float) { _enhanceImageSharpness.value = s }
    fun setEnhanceImageContrast(c: Float) { _enhanceImageContrast.value = c }
    fun setEnhanceImageTargetMinKb(kb: Int) { _enhanceImageTargetMinKb.value = kb }
    fun setEnhanceImageFormat(f: ImageEngine.OutputFormat) { _enhanceImageFormat.value = f }

    fun selectImageForEnhance(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Reading image for enhancement..."
            val pfd = getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")
            val size = pfd?.statSize ?: 0L
            pfd?.close()

            val bitmap = ImageEngine.decodeBitmapFromUri(getApplication(), uri, maxDimension = 3000)
            _enhanceSourceImage.value = bitmap
            _enhanceSourceImageOriginalSize.value = if (size > 0) size else (bitmap?.byteCount?.toLong() ?: 0L)
            _enhancedImageResult.value = null
            _enhancedImageResultBitmap.value = null
            _isLoading.value = false
        }
    }

    fun loadSampleImageForEnhance() {
        viewModelScope.launch {
            val samples = SampleFilesProvider.getOrCreateSampleImages(getApplication())
            if (samples.isNotEmpty()) {
                val file = samples.first()
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                _enhanceSourceImage.value = bitmap
                _enhanceSourceImageOriginalSize.value = file.length()
                _enhancedImageResult.value = null
                _enhancedImageResultBitmap.value = null
                _statusNotification.value = "Loaded sample image (${file.name})"
            }
        }
    }

    fun runEnhanceImage() {
        val src = _enhanceSourceImage.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _progressRatio.value = 0f
            _progressMessage.value = "Upscaling and enhancing image details..."

            try {
                val targetMinBytes = _enhanceImageTargetMinKb.value * 1024L
                val (file, enhancedBm) = ImageEngine.enhanceImage(
                    context = getApplication(),
                    sourceBitmap = src,
                    scaleFactor = _enhanceImageScale.value,
                    sharpnessStrength = _enhanceImageSharpness.value,
                    contrastBoost = _enhanceImageContrast.value,
                    targetMinSizeBytes = targetMinBytes,
                    format = _enhanceImageFormat.value,
                    quality = 95
                )

                _enhancedImageResult.value = file
                _enhancedImageResultBitmap.value = enhancedBm

                repository.insert(
                    HistoryEntity(
                        title = file.name,
                        filePath = file.absolutePath,
                        fileType = _enhanceImageFormat.value.extension.uppercase(),
                        operationType = "Enhanced",
                        sizeBytes = file.length(),
                        pageCount = 1
                    )
                )

                _statusNotification.value = "Image Enhanced: ${file.name} (${ImageEngine.formatFileSize(file.length())}, ${enhancedBm.width}×${enhancedBm.height}px)"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Enhance failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // PDF Enhancer State
    private val _enhanceSourcePdf = MutableStateFlow<File?>(null)
    val enhanceSourcePdf: StateFlow<File?> = _enhanceSourcePdf.asStateFlow()

    private val _enhancePdfDpiScale = MutableStateFlow(3.5f) // ~250-300 DPI
    val enhancePdfDpiScale: StateFlow<Float> = _enhancePdfDpiScale.asStateFlow()

    private val _enhancePdfSharpness = MutableStateFlow(0.4f)
    val enhancePdfSharpness: StateFlow<Float> = _enhancePdfSharpness.asStateFlow()

    private val _enhancePdfContrast = MutableStateFlow(1.15f)
    val enhancePdfContrast: StateFlow<Float> = _enhancePdfContrast.asStateFlow()

    private val _enhancePdfTargetMinKb = MutableStateFlow(0)
    val enhancePdfTargetMinKb: StateFlow<Int> = _enhancePdfTargetMinKb.asStateFlow()

    private val _enhancedPdfResult = MutableStateFlow<File?>(null)
    val enhancedPdfResult: StateFlow<File?> = _enhancedPdfResult.asStateFlow()

    fun setEnhancePdfDpiScale(dpi: Float) { _enhancePdfDpiScale.value = dpi }
    fun setEnhancePdfSharpness(s: Float) { _enhancePdfSharpness.value = s }
    fun setEnhancePdfContrast(c: Float) { _enhancePdfContrast.value = c }
    fun setEnhancePdfTargetMinKb(kb: Int) { _enhancePdfTargetMinKb.value = kb }

    fun selectPdfForEnhance(file: File) {
        _enhanceSourcePdf.value = file
        _enhancedPdfResult.value = null
    }

    fun selectPdfUriForEnhance(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _progressMessage.value = "Reading PDF..."
            val file = copyUriToTempFile(uri, "enhance_in_${System.currentTimeMillis()}.pdf")
            _isLoading.value = false
            if (file != null) {
                selectPdfForEnhance(file)
            }
        }
    }

    fun loadSamplePdfForEnhance() {
        viewModelScope.launch {
            val sample = SampleFilesProvider.getOrCreateSamplePdf(getApplication())
            selectPdfForEnhance(sample)
            _statusNotification.value = "Loaded sample PDF for enhancement"
        }
    }

    fun runEnhancePdf() {
        val file = _enhanceSourcePdf.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _progressRatio.value = 0f
            _progressMessage.value = "Upscaling and enhancing PDF pages..."

            try {
                val targetMinBytes = _enhancePdfTargetMinKb.value * 1024L
                val enhancedFile = PdfEngine.enhancePdf(
                    context = getApplication(),
                    inputPdf = file,
                    dpiScale = _enhancePdfDpiScale.value,
                    sharpnessStrength = _enhancePdfSharpness.value,
                    contrastBoost = _enhancePdfContrast.value,
                    targetMinSizeBytes = targetMinBytes,
                    onProgress = { cur, tot ->
                        _progressRatio.value = cur.toFloat() / tot.toFloat()
                        _progressMessage.value = "Upscaling page $cur of $tot..."
                    }
                )

                _enhancedPdfResult.value = enhancedFile

                repository.insert(
                    HistoryEntity(
                        title = enhancedFile.name,
                        filePath = enhancedFile.absolutePath,
                        fileType = "PDF",
                        operationType = "Enhanced",
                        sizeBytes = enhancedFile.length(),
                        pageCount = 1
                    )
                )

                _statusNotification.value = "PDF Enhanced: ${enhancedFile.name} (${ImageEngine.formatFileSize(enhancedFile.length())})"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "PDF Enhancement failed: ${e.localizedMessage}"
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

    private val _mergedPdfResult = MutableStateFlow<File?>(null)
    val mergedPdfResult: StateFlow<File?> = _mergedPdfResult.asStateFlow()

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
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from)
            list.add(to, item)
            _mergePdfList.value = list
        }
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

        viewModelScope.launch {
            _isLoading.value = true
            _progressRatio.value = 0f
            _progressMessage.value = "Merging ${pdfs.size} PDFs..."

            try {
                val mergedFile = PdfEngine.mergePdfs(
                    context = getApplication(),
                    inputPdfs = pdfs,
                    onProgress = { cur, tot, pages ->
                        _progressRatio.value = cur.toFloat() / tot.toFloat()
                        _progressMessage.value = "Merging document $cur of $tot..."
                    }
                )

                _mergedPdfResult.value = mergedFile

                repository.insert(
                    HistoryEntity(
                        title = mergedFile.name,
                        filePath = mergedFile.absolutePath,
                        fileType = "PDF",
                        operationType = "PDF Merge",
                        sizeBytes = mergedFile.length(),
                        pageCount = pdfs.size
                    )
                )

                _statusNotification.value = "Successfully merged ${pdfs.size} PDFs (${ImageEngine.formatFileSize(mergedFile.length())})"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusNotification.value = "Merge failed: ${e.localizedMessage}"
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

                repository.insert(
                    HistoryEntity(
                        title = extracted.name,
                        filePath = extracted.absolutePath,
                        fileType = "PDF",
                        operationType = "PDF Split",
                        sizeBytes = extracted.length(),
                        pageCount = indices.size
                    )
                )

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

                files.forEach { singleFile ->
                    repository.insert(
                        HistoryEntity(
                            title = singleFile.name,
                            filePath = singleFile.absolutePath,
                            fileType = "PDF",
                            operationType = "PDF Split",
                            sizeBytes = singleFile.length(),
                            pageCount = 1
                        )
                    )
                }

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

                // Log into Room database
                val info = _securityEncryptInfo.value
                val pageCount = info?.pageCount?.takeIf { it > 0 } ?: 1
                repository.insert(
                    HistoryEntity(
                        title = protectedFile.name,
                        filePath = protectedFile.absolutePath,
                        fileType = "PDF",
                        operationType = "PDF Password Protected",
                        sizeBytes = protectedFile.length(),
                        pageCount = pageCount
                    )
                )

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

                // Read clean security info to get page count
                val cleanInfo = PdfEngine.checkPdfSecurity(getApplication(), unlockedFile)
                val pageCount = cleanInfo.pageCount.takeIf { it > 0 } ?: 1

                repository.insert(
                    HistoryEntity(
                        title = unlockedFile.name,
                        filePath = unlockedFile.absolutePath,
                        fileType = "PDF",
                        operationType = "PDF Password Removed",
                        sizeBytes = unlockedFile.length(),
                        pageCount = pageCount
                    )
                )

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
