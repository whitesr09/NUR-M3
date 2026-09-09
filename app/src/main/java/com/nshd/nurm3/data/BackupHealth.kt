package com.nshd.nurm3.data

import android.content.Context
import androidx.room.withTransaction
import com.nshd.nurm3.focus.FocusDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reports actual local storage checks without changing or inventing records. */
data class BackupHealth(val coreIntegrity: String, val focusIntegrity: String, val coreRecovery: Boolean, val focusRecovery: Boolean, val coreEntries: Int, val focusSessions: Int)

class BackupHealthChecker(private val context: Context) {
    suspend fun inspect(): BackupHealth = withContext(Dispatchers.IO) {
        val core = NurDatabase.get(context)
        val focus = FocusDatabase.get(context)
        fun integrity(path: String): String = runCatching {
            val database = android.database.sqlite.SQLiteDatabase.openDatabase(path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY)
            try { database.rawQuery("PRAGMA quick_check(1)", null).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else "No result" } }
            finally { database.close() }
        }.getOrElse { "Check unavailable: ${it.javaClass.simpleName}" }
        val coreCount = core.withTransaction { core.dao().getAllEntries().size }
        val focusCount = focus.withTransaction { focus.dao().allSessions().size }
        val vault = RecoveryVault(context)
        BackupHealth(
            integrity(context.getDatabasePath("nur-m3.db").absolutePath),
            integrity(context.getDatabasePath("nur-focus.db").absolutePath),
            vault.exists("nur-core") || java.io.File(context.noBackupFilesDir, "nur-recovery.json").exists(),
            vault.exists("nur-focus"), coreCount, focusCount
        )
    }
}
