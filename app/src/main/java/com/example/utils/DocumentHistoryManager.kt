package com.example.utils

import android.content.Context
import java.io.File

data class DocumentItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val extension: String,
    val isPdf: Boolean
)

object DocumentHistoryManager {

    fun getAllDocuments(context: Context): List<DocumentItem> {
        val directories = listOf(
            File(context.cacheDir, "generated_pdfs"),
            File(context.cacheDir, "compressed_pdfs"),
            File(context.cacheDir, "compressed_images"),
            File(context.cacheDir, "merged_pdfs"),
            File(context.cacheDir, "split_pdfs"),
            File(context.cacheDir, "protected_pdfs"),
            File(context.cacheDir, "unlocked_pdfs"),
            File(context.cacheDir, "edited_pdfs"),
            File(context.filesDir, "sample_docs"),
            File(context.filesDir, "sample_images")
        )

        val files = mutableListOf<File>()

        for (dir in directories) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { f ->
                    if (f.isFile && f.length() > 0) {
                        files.add(f)
                    }
                }
            }
        }

        // Also scan dynamic timestamped directories
        context.cacheDir.listFiles()?.forEach { sub ->
            if (sub.isDirectory && (sub.name.startsWith("split_pages_") || sub.name.startsWith("extracted_pages_"))) {
                sub.listFiles()?.forEach { f ->
                    if (f.isFile && f.length() > 0) {
                        files.add(f)
                    }
                }
            }
        }

        return files
            .distinctBy { it.absolutePath }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val ext = file.extension.lowercase()
                val isPdf = ext == "pdf"
                DocumentItem(
                    file = file,
                    name = file.name,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified(),
                    extension = ext.uppercase(),
                    isPdf = isPdf
                )
            }
    }

    fun deleteDocument(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    fun clearAllDocuments(context: Context): Int {
        val docs = getAllDocuments(context)
        var count = 0
        docs.forEach {
            if (deleteDocument(it.file)) count++
        }
        return count
    }
}
