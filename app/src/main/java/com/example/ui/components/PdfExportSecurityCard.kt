package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PdfExportSecurityConfig
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen

@Composable
fun PdfExportSecurityCard(
    config: PdfExportSecurityConfig,
    onConfigChange: (PdfExportSecurityConfig) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Export PDF Security",
    subtitle: String = "Apply password encryption to protect this document"
) {
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var showOwnerPassword by remember { mutableStateOf(false) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (config.isProtectionEnabled) {
                PrimaryIndigo.copy(alpha = 0.05f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        border = if (config.isProtectionEnabled) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(PrimaryIndigo.copy(alpha = 0.6f))
            )
        } else null,
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_pdf_export_security")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (config.isProtectionEnabled) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (config.isProtectionEnabled) Icons.Default.Lock else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (config.isProtectionEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (config.isProtectionEnabled) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${config.keyLength}-bit AES",
                                        color = SuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = config.isProtectionEnabled,
                    onCheckedChange = { enabled ->
                        onConfigChange(config.copy(isProtectionEnabled = enabled))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryIndigo
                    ),
                    modifier = Modifier.testTag("switch_export_password_protection")
                )
            }

            AnimatedVisibility(
                visible = config.isProtectionEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Privacy guarantee banner
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EnhancedEncryption,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PDF will be secured with standard PDF standard encryption. Anyone opening it must enter the password.",
                                fontSize = 11.sp,
                                color = Color(0xFF1E3A8A),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // 1. User / Open Password Field
                    OutlinedTextField(
                        value = config.userPassword,
                        onValueChange = {
                            onConfigChange(config.copy(userPassword = it))
                        },
                        label = { Text("Open Password (Required to view)") },
                        placeholder = { Text("Enter password to unlock PDF") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_export_user_password"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryIndigo)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Password Strength Indicator
                    if (config.userPassword.isNotEmpty()) {
                        val strength = calculatePasswordStrength(config.userPassword)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Password Strength: ${strength.label}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = strength.color
                                )
                                Text(
                                    text = "${config.userPassword.length} characters",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { strength.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = strength.color,
                                trackColor = strength.color.copy(alpha = 0.2f)
                            )
                        }
                    }

                    // 2. Confirm Password Field
                    val isPasswordMatching = config.userPassword.isNotEmpty() && config.userPassword == config.confirmPassword
                    val hasMismatch = config.confirmPassword.isNotEmpty() && config.userPassword != config.confirmPassword

                    OutlinedTextField(
                        value = config.confirmPassword,
                        onValueChange = {
                            onConfigChange(config.copy(confirmPassword = it))
                        },
                        label = { Text("Confirm Password") },
                        placeholder = { Text("Re-enter password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_export_confirm_password"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = if (isPasswordMatching) Icons.Default.CheckCircle else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isPasswordMatching) SuccessGreen else if (hasMismatch) AccentCoral else PrimaryIndigo
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isPasswordMatching) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Matched",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else if (hasMismatch) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Mismatch",
                                        tint = AccentCoral,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                    Icon(
                                        imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                                    )
                                }
                            }
                        },
                        isError = hasMismatch,
                        supportingText = {
                            if (hasMismatch) {
                                Text("Passwords do not match", color = AccentCoral, fontSize = 11.sp)
                            } else if (isPasswordMatching) {
                                Text("Passwords match securely", color = SuccessGreen, fontSize = 11.sp)
                            }
                        }
                    )

                    // 3. Advanced Security & Restrictions (Collapsible)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = PrimaryIndigo,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Advanced Permissions & Restrictions",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Icon(
                                    imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }

                            AnimatedVisibility(visible = isAdvancedExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Encryption Strength FilterChips
                                    Text(
                                        text = "Encryption Standard",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = config.keyLength == 128,
                                            onClick = { onConfigChange(config.copy(keyLength = 128)) },
                                            label = { Text("AES 128-bit (Standard)") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryIndigo,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                        FilterChip(
                                            selected = config.keyLength == 256,
                                            onClick = { onConfigChange(config.copy(keyLength = 256)) },
                                            label = { Text("AES 256-bit (Maximum)") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryIndigo,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Permission Toggles
                                    Text(
                                        text = "Document Access Restrictions",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onConfigChange(config.copy(canPrint = !config.canPrint))
                                            }
                                    ) {
                                        Checkbox(
                                            checked = config.canPrint,
                                            onCheckedChange = { onConfigChange(config.copy(canPrint = it)) },
                                            colors = CheckboxDefaults.colors(checkedColor = PrimaryIndigo)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text("Allow Printing", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("Recipients can print the document", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onConfigChange(config.copy(canExtractContent = !config.canExtractContent))
                                            }
                                    ) {
                                        Checkbox(
                                            checked = config.canExtractContent,
                                            onCheckedChange = { onConfigChange(config.copy(canExtractContent = it)) },
                                            colors = CheckboxDefaults.colors(checkedColor = PrimaryIndigo)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text("Allow Copying Text & Images", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("Recipients can copy content to clipboard", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onConfigChange(config.copy(canModify = !config.canModify))
                                            }
                                    ) {
                                        Checkbox(
                                            checked = config.canModify,
                                            onCheckedChange = { onConfigChange(config.copy(canModify = it)) },
                                            colors = CheckboxDefaults.colors(checkedColor = PrimaryIndigo)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text("Allow Document Modification", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("Allow editing pages, annotations and forms", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Optional Owner / Master Password
                                    OutlinedTextField(
                                        value = config.ownerPassword,
                                        onValueChange = { onConfigChange(config.copy(ownerPassword = it)) },
                                        label = { Text("Owner Master Password (Optional)") },
                                        placeholder = { Text("Separate password for master permissions") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        visualTransformation = if (showOwnerPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showOwnerPassword = !showOwnerPassword }) {
                                                Icon(
                                                    imageVector = if (showOwnerPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PasswordStrength(
    val label: String,
    val progress: Float,
    val color: Color
)

private fun calculatePasswordStrength(password: String): PasswordStrength {
    var score = 0
    if (password.length >= 6) score++
    if (password.length >= 10) score++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return when {
        score <= 2 -> PasswordStrength("Weak", 0.33f, Color(0xFFEF4444))
        score in 3..4 -> PasswordStrength("Moderate", 0.66f, Color(0xFFF59E0B))
        else -> PasswordStrength("Strong", 1.0f, Color(0xFF10B981))
    }
}
