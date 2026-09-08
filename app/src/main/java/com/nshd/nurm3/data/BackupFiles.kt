package com.nshd.nurm3.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Storage Access Framework IO: no broad storage permissions and no external file paths. */
object BackupFiles {
    suspend fun read(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("Cannot open selected document")
        val bytes = input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count == -1) break
                total += count
                require(total <= BackupCodec.MAX_BYTES) { "Backup exceeds the 5 MB safety limit" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
    }

    suspend fun write(context: Context, uri: Uri, json: String) = withContext(Dispatchers.IO) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        require(bytes.size <= BackupCodec.MAX_BYTES) { "Backup exceeds the 5 MB safety limit" }
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw IllegalArgumentException("Cannot write selected document")
        output.use { it.write(bytes); it.flush() }
    }
}
