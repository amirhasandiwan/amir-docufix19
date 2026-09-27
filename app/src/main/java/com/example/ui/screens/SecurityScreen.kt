package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
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
fun SecurityScreen(
    viewModel: PdfUtilViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentTab by viewModel.securityTab.collectAsStateWithLifecycle()

    // Encrypt states
    val encryptFile by viewModel.securityEncryptFile.collectAsStateWithLifecycle()
    val encryptInfo by viewModel.securityEncryptInfo.collectAsStateWithLifecycle()
    val userPassword by viewModel.securityUserPassword.collectAsStateWithLifecycle()
    val ownerPassword by viewModel.securityOwnerPassword.collectAsStateWithLifecycle()
    val useSamePassword by viewModel.securityUseSamePassword.collectAsStateWithLifecycle()
    val keyLength by viewModel.securityKeyLength.collectAsStateWithLifecycle()
    val canPrint by viewModel.securityCanPrint.collectAsStateWithLifecycle()
    val canModify by viewModel.securityCanModify.collectAsStateWithLifecycle()
    val canExtractContent by viewModel.securityCanExtractContent.collectAsStateWithLifecycle()
    val canModifyAnnotations by viewModel.securityCanModifyAnnotations.collectAsStateWithLifecycle()
    val encryptResult by viewModel.securityEncryptResult.collectAsStateWithLifecycle()

    // Decrypt states
    val decryptFile by viewModel.securityDecryptFile.collectAsStateWithLifecycle()
    val decryptInfo by viewModel.securityDecryptInfo.collectAsStateWithLifecycle()
    val decryptPassword by viewModel.securityDecryptPassword.collectAsStateWithLifecycle()
    val decryptResult by viewModel.securityDecryptResult.collectAsStateWithLifecycle()
    val decryptError by viewModel.securityDecryptError.collectAsStateWithLifecycle()

    val pickPdfToEncryptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.setSecurityEncryptFile(it) }
    }

    val pickPdfToDecryptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.setSecurityDecryptFile(it) }
    }

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(
            title = "PDF Lock & Unlock",
            subtitle = "Encrypt with passwords & permissions, or unlock PDF",
            onBackClick = onBack
        )

        TabRow(
            selectedTabIndex = if (currentTab == PdfUtilViewModel.SecurityTab.ENCRYPT) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryIndigo
        ) {
            Tab(
                selected = currentTab == PdfUtilViewModel.SecurityTab.ENCRYPT,
                onClick = { viewModel.setSecurityTab(PdfUtilViewModel.SecurityTab.ENCRYPT) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lock / Encrypt PDF", fontWeight = FontWeight.SemiBold)
                    }
                },
                modifier = Modifier.testTag("tab_encrypt_pdf")
            )
            Tab(
                selected = currentTab == PdfUtilViewModel.SecurityTab.DECRYPT,
                onClick = { viewModel.setSecurityTab(PdfUtilViewModel.SecurityTab.DECRYPT) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock / Remove Pass", fontWeight = FontWeight.SemiBold)
                    }
                },
                modifier = Modifier.testTag("tab_decrypt_pdf")
            )
        }

        when (currentTab) {
            PdfUtilViewModel.SecurityTab.ENCRYPT -> {
                EncryptTabContent(
                    encryptFile = encryptFile,
                    encryptInfo = encryptInfo,
                    userPassword = userPassword,
                    ownerPassword = ownerPassword,
                    useSamePassword = useSamePassword,
                    keyLength = keyLength,
                    canPrint = canPrint,
                    canModify = canModify,
                    canExtractContent = canExtractContent,
                    canModifyAnnotations = canModifyAnnotations,
                    encryptResult = encryptResult,
                    onPickPdf = { pickPdfToEncryptLauncher.launch(arrayOf("application/pdf")) },
                    onLoadSample = { viewModel.loadSamplePdfForEncrypt() },
                    onClearFile = { viewModel.clearSecurityEncryptFile() },
                    onUserPasswordChange = { viewModel.setSecurityUserPassword(it) },
                    onOwnerPasswordChange = { viewModel.setSecurityOwnerPassword(it) },
                    onUseSamePasswordChange = { viewModel.setSecurityUseSamePassword(it) },
                    onKeyLengthChange = { viewModel.setSecurityKeyLength(it) },
                    onCanPrintChange = { viewModel.setSecurityCanPrint(it) },
                    onCanModifyChange = { viewModel.setSecurityCanModify(it) },
                    onCanExtractContentChange = { viewModel.setSecurityCanExtractContent(it) },
                    onCanModifyAnnotationsChange = { viewModel.setSecurityCanModifyAnnotations(it) },
                    onEncrypt = { viewModel.encryptPdfAction() },
                    onOpenResult = { file -> FileOpener.openFile(context, file, "application/pdf") },
                    onShareResult = { file -> FileOpener.shareFile(context, file, "application/pdf") }
                )
            }
            PdfUtilViewModel.SecurityTab.DECRYPT -> {
                DecryptTabContent(
                    decryptFile = decryptFile,
                    decryptInfo = decryptInfo,
                    decryptPassword = decryptPassword,
                    decryptResult = decryptResult,
                    decryptError = decryptError,
                    onPickPdf = { pickPdfToDecryptLauncher.launch(arrayOf("application/pdf")) },
                    onLoadSample = { viewModel.loadSamplePdfForDecrypt() },
                    onClearFile = { viewModel.clearSecurityDecryptFile() },
                    onPasswordChange = { viewModel.setSecurityDecryptPassword(it) },
                    onDecrypt = { viewModel.decryptPdfAction() },
                    onOpenResult = { file -> FileOpener.openFile(context, file, "application/pdf") },
                    onShareResult = { file -> FileOpener.shareFile(context, file, "application/pdf") }
                )
            }
        }
    }
}

@Composable
private fun EncryptTabContent(
    encryptFile: File?,
    encryptInfo: com.example.utils.PdfEngine.PdfSecurityInfo?,
    userPassword: String,
    ownerPassword: String,
    useSamePassword: Boolean,
    keyLength: Int,
    canPrint: Boolean,
    canModify: Boolean,
    canExtractContent: Boolean,
    canModifyAnnotations: Boolean,
    encryptResult: File?,
    onPickPdf: () -> Unit,
    onLoadSample: () -> Unit,
    onClearFile: () -> Unit,
    onUserPasswordChange: (String) -> Unit,
    onOwnerPasswordChange: (String) -> Unit,
    onUseSamePasswordChange: (Boolean) -> Unit,
    onKeyLengthChange: (Int) -> Unit,
    onCanPrintChange: (Boolean) -> Unit,
    onCanModifyChange: (Boolean) -> Unit,
    onCanExtractContentChange: (Boolean) -> Unit,
    onCanModifyAnnotationsChange: (Boolean) -> Unit,
    onEncrypt: () -> Unit,
    onOpenResult: (File) -> Unit,
    onShareResult: (File) -> Unit
) {
    var showUserPassword by remember { mutableStateOf(false) }
    var showOwnerPassword by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. File Selection Card
        item {
            if (encryptFile == null) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
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
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select PDF to Protect",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add open passwords and configure granular owner permissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onPickPdf,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("button_select_pdf_encrypt")
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose PDF")
                            }
                            OutlinedButton(
                                onClick = onLoadSample,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("button_load_sample_encrypt")
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sample PDF")
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryIndigo.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PrimaryIndigo)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = encryptFile.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${ImageEngine.formatFileSize(encryptFile.length())} • ${encryptInfo?.pageCount ?: 1} pages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (encryptInfo?.isEncrypted == true) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🔒 Currently Password Protected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        IconButton(onClick = onClearFile) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear PDF", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // 2. Password Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Password Credentials",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Open / User Password Field
                    OutlinedTextField(
                        value = userPassword,
                        onValueChange = onUserPasswordChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_open_password"),
                        label = { Text("Open Password (User Password)") },
                        placeholder = { Text("e.g. SecretDoc2026") },
                        supportingText = { Text("Required to unlock and view the document") },
                        singleLine = true,
                        visualTransformation = if (showUserPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showUserPassword = !showUserPassword }) {
                                Icon(
                                    imageVector = if (showUserPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showUserPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Use Same Password Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUseSamePasswordChange(!useSamePassword) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = useSamePassword,
                            onCheckedChange = onUseSamePasswordChange,
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryIndigo),
                            modifier = Modifier.testTag("checkbox_same_password")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Use same password as Owner Master Key", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Simpler setup with one shared password", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Distinct Owner / Permissions Password Field
                    if (!useSamePassword) {
                        OutlinedTextField(
                            value = ownerPassword,
                            onValueChange = onOwnerPasswordChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_owner_password"),
                            label = { Text("Owner Master Password (Permissions Key)") },
                            placeholder = { Text("e.g. MasterAdminPass") },
                            supportingText = { Text("Required to modify permissions or edit security") },
                            singleLine = true,
                            visualTransformation = if (showOwnerPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showOwnerPassword = !showOwnerPassword }) {
                                    Icon(
                                        imageVector = if (showOwnerPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showOwnerPassword) "Hide master password" else "Show master password"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Encryption Strength Selection
                    Text(
                        text = "Encryption Standard",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = keyLength == 128,
                            onClick = { onKeyLengthChange(128) },
                            label = { Text("128-bit AES (Standard)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryIndigo
                            )
                        )
                        FilterChip(
                            selected = keyLength == 256,
                            onClick = { onKeyLengthChange(256) },
                            label = { Text("256-bit AES (High)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryIndigo
                            )
                        )
                    }
                }
            }
        }

        // 3. Owner Permission Restrictions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Owner-Permission Restrictions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Controls permitted actions when opened with Open Password",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    PermissionSwitchItem(
                        icon = Icons.Default.Print,
                        title = "Allow Printing",
                        description = "Enables physical or virtual printing",
                        checked = canPrint,
                        onCheckedChange = onCanPrintChange,
                        testTag = "switch_can_print"
                    )

                    PermissionSwitchItem(
                        icon = Icons.Default.ContentCopy,
                        title = "Allow Copying Text & Images",
                        description = "Permits selecting & extracting content",
                        checked = canExtractContent,
                        onCheckedChange = onCanExtractContentChange,
                        testTag = "switch_can_extract"
                    )

                    PermissionSwitchItem(
                        icon = Icons.Default.Edit,
                        title = "Allow Modifying Document",
                        description = "Allows changing pages and content",
                        checked = canModify,
                        onCheckedChange = onCanModifyChange,
                        testTag = "switch_can_modify"
                    )

                    PermissionSwitchItem(
                        icon = Icons.Default.Comment,
                        title = "Allow Adding Comments & Forms",
                        description = "Enables annotations and filling form fields",
                        checked = canModifyAnnotations,
                        onCheckedChange = onCanModifyAnnotationsChange,
                        testTag = "switch_can_annotate"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Restrictions are enforced according to the official PDF security standard. An owner password is required to bypass restrictions.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4. Encrypt Action Button
        item {
            val isReady = encryptFile != null && (userPassword.isNotBlank() || ownerPassword.isNotBlank())
            Button(
                onClick = onEncrypt,
                enabled = isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("button_encrypt_pdf_action"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Encrypt & Protect PDF",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 5. Encrypt Result Card
        if (encryptResult != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_encrypt_result"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PDF Encrypted & Locked Successfully!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }

                        Text(
                            text = encryptResult.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Size: ${ImageEngine.formatFileSize(encryptResult.length())} • ${keyLength}-bit AES Encrypted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onOpenResult(encryptResult) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_open_encrypted_pdf"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open PDF")
                            }

                            OutlinedButton(
                                onClick = { onShareResult(encryptResult) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_share_encrypted_pdf"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }

                        Text(
                            text = "✓ Logged into local Room database conversion history",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DecryptTabContent(
    decryptFile: File?,
    decryptInfo: com.example.utils.PdfEngine.PdfSecurityInfo?,
    decryptPassword: String,
    decryptResult: File?,
    decryptError: String?,
    onPickPdf: () -> Unit,
    onLoadSample: () -> Unit,
    onClearFile: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onDecrypt: () -> Unit,
    onOpenResult: (File) -> Unit,
    onShareResult: (File) -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Decrypt File Selection Card
        item {
            if (decryptFile == null) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SecondaryTeal.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = SecondaryTeal,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select Locked PDF to Decrypt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Provide the password once to permanently remove encryption",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onPickPdf,
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("button_select_pdf_decrypt")
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose PDF")
                            }
                            OutlinedButton(
                                onClick = onLoadSample,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("button_load_sample_decrypt")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Locked Sample")
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SecondaryTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SecondaryTeal)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = decryptFile.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${ImageEngine.formatFileSize(decryptFile.length())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (decryptInfo?.isEncrypted == true) {
                                Text(
                                    text = "🔒 Password Protected (Encrypted)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "🔓 Unencrypted / Open PDF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        IconButton(onClick = onClearFile) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear PDF", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // 2. Unlock Password Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enter Document Password",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = decryptPassword,
                        onValueChange = onPasswordChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_decrypt_password"),
                        label = { Text("Password (Open or Owner Key)") },
                        placeholder = { Text("Enter password to unlock") },
                        supportingText = {
                            if (decryptFile?.name?.contains("Sample", ignoreCase = true) == true) {
                                Text("Test Sample Password is: 1234", color = SecondaryTeal, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Type the existing password to permanently remove protection")
                            }
                        },
                        singleLine = true,
                        isError = decryptError != null,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (decryptError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentCoral.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = decryptError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AccentCoral,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Decrypt Action Button
        item {
            val isReady = decryptFile != null && decryptPassword.isNotBlank()
            Button(
                onClick = onDecrypt,
                enabled = isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("button_decrypt_pdf_action"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlock & Remove Password",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. Decrypt Result Card
        if (decryptResult != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_decrypt_result"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Password Removed Successfully!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }

                        Text(
                            text = decryptResult.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Size: ${ImageEngine.formatFileSize(decryptResult.length())} • Unlocked PDF (No Password Required)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onOpenResult(decryptResult) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_open_unlocked_pdf"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open PDF")
                            }

                            OutlinedButton(
                                onClick = { onShareResult(decryptResult) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_share_unlocked_pdf"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }

                        Text(
                            text = "✓ Logged into local Room database history",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (checked) PrimaryIndigo.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryIndigo),
            modifier = Modifier.testTag(testTag)
        )
    }
}
