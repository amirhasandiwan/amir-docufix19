package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun formatFileSize_formatsCorrectly() {
    assertEquals("0 B", com.example.utils.ImageEngine.formatFileSize(0))
    assertEquals("500 B", com.example.utils.ImageEngine.formatFileSize(500))
    assertTrue(com.example.utils.ImageEngine.formatFileSize(1024).contains("KB"))
    assertTrue(com.example.utils.ImageEngine.formatFileSize(1024 * 1024 * 2).contains("MB"))
  }
}
