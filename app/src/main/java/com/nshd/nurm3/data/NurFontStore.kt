package com.nshd.nurm3.data

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Private, bounded font import. Never stores a temporary picker URI or downloads a font. */
class NurFontStore(private val context: Context) {
    data class ImportedFont(val id: String, val name: String)
    companion object {
        const val MAX_BYTES = 8L * 1024L * 1024L
        private val ID_PATTERN = Regex("[a-f0-9]{64}")
        fun file(context: Context, id: String): File? =
            id.takeIf { ID_PATTERN.matches(it) }?.let { File(File(context.filesDir, "fonts"), "$it.font") }
    }

    suspend fun import(uri: Uri): ImportedFont = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "fonts").apply { mkdirs() }
        val temporary = File.createTempFile("import-", ".tmp", context.cacheDir)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_BYTES) { "Font must be smaller than 8 MB" }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            } ?: error("Unable to read the selected font")
            require(total >= 12L) { "The selected font is empty or invalid" }
            val header = temporary.inputStream().use { input -> ByteArray(4).also { input.read(it) } }
            val signature = String(header, Charsets.US_ASCII)
            require((header.contentEquals(byteArrayOf(0, 1, 0, 0)) || signature == "OTTO" || signature == "true" || signature == "typ1")) {
                "Choose a valid TTF or OTF font file"
            }
            require(Typeface.Builder(temporary).build() != null) { "Android could not load this font" }
            val id = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            val destination = file(context, id) ?: error("Invalid font identifier")
            if (!destination.exists()) {
                val staging = File(directory, "$id.new")
                temporary.copyTo(staging, overwrite = true)
                try {
                    java.nio.file.Files.move(staging.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE)
                } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                    java.nio.file.Files.move(staging.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                } finally { staging.delete() }
            }
            require(Typeface.Builder(destination).build() != null) { "The imported font could not be reopened" }
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull()?.take(100)?.substringAfterLast('/')?.substringAfterLast('\\')?.ifBlank { null } ?: "Custom font"
            ImportedFont(id, name)
        } finally { temporary.delete() }
    }
}
