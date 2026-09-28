package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Amir DocuFix", appName)
  }

  @Test
  fun `verify list reordering by index works correctly`() {
    val list = mutableListOf("Doc1.pdf", "Doc2.pdf", "Doc3.pdf", "Doc4.pdf")
    // Move from index 3 (Doc4) to index 0 (Doc4 at top)
    val item = list.removeAt(3)
    list.add(0, item)
    assertEquals(listOf("Doc4.pdf", "Doc1.pdf", "Doc2.pdf", "Doc3.pdf"), list)
  }

  @Test
  fun `verify export security config validation`() {
    val defaultCfg = com.example.data.model.PdfExportSecurityConfig(isProtectionEnabled = false)
    org.junit.Assert.assertTrue(defaultCfg.isValid)
    org.junit.Assert.assertNull(defaultCfg.errorMessage)

    val invalidEmptyPass = com.example.data.model.PdfExportSecurityConfig(
        isProtectionEnabled = true,
        userPassword = "",
        confirmPassword = ""
    )
    org.junit.Assert.assertFalse(invalidEmptyPass.isValid)
    assertEquals("Password cannot be empty", invalidEmptyPass.errorMessage)

    val mismatchPass = com.example.data.model.PdfExportSecurityConfig(
        isProtectionEnabled = true,
        userPassword = "Secret123!",
        confirmPassword = "WrongPassword"
    )
    org.junit.Assert.assertFalse(mismatchPass.isValid)
    assertEquals("Passwords do not match", mismatchPass.errorMessage)

    val validPass = com.example.data.model.PdfExportSecurityConfig(
        isProtectionEnabled = true,
        userPassword = "SecurePass123!",
        confirmPassword = "SecurePass123!"
    )
    org.junit.Assert.assertTrue(validPass.isValid)
    org.junit.Assert.assertNull(validPass.errorMessage)
  }
}
