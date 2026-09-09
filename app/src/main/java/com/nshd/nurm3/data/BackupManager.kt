package com.nshd.nurm3.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** SAF-based import/export. All database mutations remain transactional. */
class BackupManager(private val context: Context, private val database: NurDatabase) {
    private val dao = database.dao()
    private val dhikr = database.dhikrDao()

    private suspend fun snapshot(): String {
        val raw = BackupCodec.export(dao.getAllEntries(), dao.getAllCompletions(),
            dhikrPhrases = dhikr.getAllPhrases(), dhikrDays = dhikr.getAllDays())
        require(raw.toByteArray(Charsets.UTF_8).size <= BackupCodec.MAX_BYTES) { "Backup is too large" }
        return raw
    }

    suspend fun export(): String = database.withTransaction { snapshot() }

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

    /** Merge keeps local definitions and records when identifiers conflict. */
    suspend fun merge(payload: BackupPayload): Int {
        BackupCodec.validate(payload)
        return database.withTransaction {
            val existing = dao.getAllEntries().map { it.id }.toSet()
            val existingRecords = dao.getAllCompletions().map { it.entryId to it.localDate }.toSet()
            payload.entries.filterNot { it.id in existing }.forEach { dao.insertEntryIgnoringConflict(it) }
            payload.completions.filterNot { (it.entryId to it.localDate) in existingRecords }.forEach { dao.insertCompletionIgnoringConflict(it) }
            val localPhrases = dhikr.getAllPhrases().map { it.id }.toSet()
            val localDays = dhikr.getAllDays().map { it.phraseId to it.localDate }.toSet()
            payload.dhikrPhrases.filterNot { it.id in localPhrases }.forEach { dhikr.insertPhraseIgnoringConflict(it) }
            payload.dhikrDays.filterNot { (it.phraseId to it.localDate) in localDays }.forEach { dhikr.insertDayIgnoringConflict(it) }
            payload.entries.count { it.id !in existing }
        }
    }

    private suspend fun preserveRecovery(recovery: String) = withContext(Dispatchers.IO) {
        val file = File(context.noBackupFilesDir, "nur-recovery.json")
        val temporary = File(context.noBackupFilesDir, "nur-recovery.tmp")
        FileOutputStream(temporary).use { stream ->
            stream.write(recovery.toByteArray(Charsets.UTF_8))
            stream.fd.sync()
        }
        try {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /** A single transaction prevents new counts arriving between the recovery snapshot and replacement. */
    suspend fun replace(payload: BackupPayload) {
        BackupCodec.validate(payload)
        database.withTransaction {
            preserveRecovery(snapshot())
            dao.clearCompletionsForRestore()
            dao.clearNonPrayerEntriesForRestore()
            payload.entries.filter { it.kind != NurKind.PRAYER }.forEach { dao.saveEntry(it) }
            payload.completions.forEach { dao.saveCompletion(it) }
            // Older backups did not contain Dhikr. Never erase newer counters when restoring one.
            if (payload.schema >= 3) {
                dhikr.clearDaysForRestore()
                dhikr.clearPhrasesForRestore()
                payload.dhikrPhrases.forEach { dhikr.savePhrase(it) }
                payload.dhikrDays.forEach { dhikr.saveDay(it) }
            }
        }
    }

    suspend fun recovery(): BackupPayload? = withContext(Dispatchers.IO) {
        val file = File(context.noBackupFilesDir, "nur-recovery.json")
        if (file.exists()) BackupCodec.decode(file.readText()) else null
    }
}
