package com.example.data.model

data class PdfExportSecurityConfig(
    val isProtectionEnabled: Boolean = false,
    val userPassword: String = "",
    val confirmPassword: String = "",
    val ownerPassword: String = "",
    val keyLength: Int = 128, // 128 or 256
    val canPrint: Boolean = true,
    val canExtractContent: Boolean = true,
    val canModify: Boolean = false,
    val canModifyAnnotations: Boolean = true
) {
    val isValid: Boolean
        get() = !isProtectionEnabled || (userPassword.isNotEmpty() && userPassword == confirmPassword)

    val errorMessage: String?
        get() {
            if (!isProtectionEnabled) return null
            if (userPassword.isEmpty()) return "Password cannot be empty"
            if (userPassword != confirmPassword) return "Passwords do not match"
            return null
        }
}
