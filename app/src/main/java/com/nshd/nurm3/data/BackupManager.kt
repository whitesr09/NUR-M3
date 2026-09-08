package com.nshd.nurm3.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** SAF-based import/export. Database mutations are transactional. */
class BackupManager(private val context: Context, private val database: NurDatabase) {
    private val dao = database.dao()

    suspend fun export(): String = database.withTransaction {
        BackupCodec.export(dao.getAllEntries(), dao.getAllCompletions())
    }

    suspend fun write(uri: Uri, data: String) = withContext(Dispatchers.IO) {
        require(data.toByteArray(Charsets.UTF_8).size <= BackupCodec.MAX_BYTES)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(data.toByteArray(Charsets.UTF_8)) }
            ?: error("Unable to open destination")
    }

    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                require(output.size() <= BackupCodec.MAX_BYTES) { "Backup is too large" }
            }
            output.toString("UTF-8")
        } ?: error("Unable to read backup")
    }

    /** Merge keeps local definitions and completions when identifiers conflict. */
    suspend fun merge(payload: BackupPayload): Int = database.withTransaction {
        val existing = dao.getAllEntries().map { it.id }.toSet()
        val existingRecords = dao.getAllCompletions().map { it.entryId to it.localDate }.toSet()
        payload.entries.filterNot { it.id in existing }.forEach { dao.insertEntryIgnoringConflict(it) }
        payload.completions.filterNot { (it.entryId to it.localDate) in existingRecords }.forEach { dao.insertCompletionIgnoringConflict(it) }
        payload.entries.count { it.id !in existing }
    }

    /** Preserve a recovery snapshot before replacing user data. */
    suspend fun replace(payload: BackupPayload) {
        val recovery = export()
        withContext(Dispatchers.IO) {
            val file = File(context.noBackupFilesDir, "nur-recovery.json")
            val temporary = File(context.noBackupFilesDir, "nur-recovery.tmp")
            temporary.writeText(recovery)
            if (file.exists()) file.delete()
            check(temporary.renameTo(file)) { "Could not preserve recovery snapshot" }
        }
        database.withTransaction {
            dao.clearCompletionsForRestore()
            dao.clearNonPrayerEntriesForRestore()
            payload.entries.filter { it.kind != NurKind.PRAYER }.forEach { dao.saveEntry(it) }
            payload.completions.forEach { dao.saveCompletion(it) }
        }
    }

    suspend fun recovery(): BackupPayload? = withContext(Dispatchers.IO) {
        val file = File(context.noBackupFilesDir, "nur-recovery.json")
        if (file.exists()) BackupCodec.decode(file.readText()) else null
    }
}
