package com.nshd.nurm3.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.nshd.nurm3.focus.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/** Explicit portable export; credentials, AI history and private journal data are excluded. */
data class SecureSnapshot(
    val core: BackupPayload,
    val sessions: List<FocusSession>,
    val routines: List<FocusRoutine>,
    val fingerprint: String
)

class SecureBackupManager(private val context: Context) {
    private val main = NurDatabase.get(context)
    private val focus = FocusDatabase.get(context)
    private val legacy = BackupManager(context, main)
    private val vault = RecoveryVault(context)

    suspend fun export(password: CharArray): String = withContext(Dispatchers.IO) {
        val core = legacy.export()
        val (sessions, routines) = focus.withTransaction { focus.dao().allSessions() to focus.dao().allRoutines() }
        val root = JSONObject().put("format", "NUR-M3-portable-v1").put("createdAt", System.currentTimeMillis())
            .put("core", JSONObject(core)).put("focusSessions", encodeSessions(sessions)).put("focusRoutines", encodeRoutines(routines))
        val plain = root.toString().toByteArray(Charsets.UTF_8)
        try {
            require(plain.size <= SecureBackupCodec.MAX_PLAINTEXT) { "Backup is too large." }
            SecureBackupCodec.encrypt(plain, password)
        } finally { plain.fill(0) }
    }

    suspend fun preview(envelope: String, password: CharArray): SecureSnapshot = withContext(Dispatchers.IO) {
        val plain = SecureBackupCodec.decrypt(envelope, password)
        try {
            val root = JSONObject(String(plain, Charsets.UTF_8))
            require(root.getString("format") == "NUR-M3-portable-v1") { "Unsupported backup contents." }
            val snapshot = SecureSnapshot(BackupCodec.decode(root.getJSONObject("core").toString()),
                decodeSessions(root.getJSONArray("focusSessions")), decodeRoutines(root.getJSONArray("focusRoutines")), SecureBackupCodec.fingerprint(envelope))
            validate(snapshot)
            snapshot
        } finally { plain.fill(0) }
    }

    suspend fun merge(snapshot: SecureSnapshot): Int {
        validate(snapshot)
        val added = legacy.merge(snapshot.core)
        focus.withTransaction {
            val dao = focus.dao()
            val existingSessions = dao.allSessions().map { it.id }.toSet()
            val existingRoutines = dao.allRoutines().map { it.id }.toSet()
            snapshot.routines.filterNot { it.id in existingRoutines }.forEach { dao.saveRoutine(it) }
            snapshot.sessions.filterNot { it.id in existingSessions }.forEach { dao.saveSession(it) }
        }
        return added
    }

    /** Explicit replacement preserves encrypted recovery before either database is changed. */
    suspend fun replace(snapshot: SecureSnapshot) {
        validate(snapshot)
        val recovery = focus.withTransaction {
            JSONObject().put("format", "NUR-M3-focus-recovery-v1")
                .put("sessions", encodeSessions(focus.dao().allSessions()))
                .put("routines", encodeRoutines(focus.dao().allRoutines())).toString()
        }
        withContext(Dispatchers.IO) { vault.save("nur-focus", recovery.toByteArray(Charsets.UTF_8)) }
        legacy.replace(snapshot.core)
        focus.withTransaction {
            val dao = focus.dao()
            dao.clearSessionsForRestore()
            dao.clearRoutinesForRestore()
            snapshot.routines.forEach { dao.saveRoutine(it) }
            snapshot.sessions.forEach { dao.saveSession(it) }
        }
    }

    /** Recovery is read-only until the user explicitly confirms a restore. */
    suspend fun recovery(): SecureSnapshot? = withContext(Dispatchers.IO) {
        val core = legacy.recovery() ?: return@withContext null
        val bytes = vault.read("nur-focus")
        val focusRoot = bytes?.let { JSONObject(String(it, Charsets.UTF_8)) }
        if (focusRoot != null) require(focusRoot.getString("format") == "NUR-M3-focus-recovery-v1")
        val snapshot = SecureSnapshot(core,
            focusRoot?.let { decodeSessions(it.getJSONArray("sessions")) } ?: emptyList(),
            focusRoot?.let { decodeRoutines(it.getJSONArray("routines")) } ?: emptyList(), "Local recovery")
        validate(snapshot)
        snapshot
    }

    suspend fun write(uri: Uri, value: String) = withContext(Dispatchers.IO) {
        require(value.toByteArray(Charsets.UTF_8).size <= SecureBackupCodec.MAX_ENVELOPE)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(value.toByteArray(Charsets.UTF_8)) }
            ?: error("Unable to open destination.")
    }
    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                require(output.size() <= SecureBackupCodec.MAX_ENVELOPE) { "Backup is too large." }
            }
            output.toString("UTF-8")
        } ?: error("Unable to read backup.")
    }

    private fun validate(snapshot: SecureSnapshot) {
        BackupCodec.validate(snapshot.core)
        require(snapshot.sessions.size <= 20_000 && snapshot.routines.size <= 2_000)
        require(snapshot.sessions.map { it.id }.distinct().size == snapshot.sessions.size)
        require(snapshot.routines.map { it.id }.distinct().size == snapshot.routines.size)
        snapshot.sessions.forEach(::validateSession)
        snapshot.routines.forEach(::validateRoutine)
    }
    private fun validateId(id: String) { require(id.isNotBlank() && id.length <= 128) }
    private fun validateSession(s: FocusSession) {
        validateId(s.id)
        require(s.label.isNotBlank() && s.label.length <= 120 && s.plannedSeconds in 60..14_400 && s.elapsedSeconds in 0..s.plannedSeconds)
        require(s.status in setOf("paused", "completed", "cancelled"))
        require(s.startedAt > 0 && (s.finishedAt == null || s.finishedAt >= s.startedAt))
        require(s.status != "completed" || s.elapsedSeconds == s.plannedSeconds && s.finishedAt != null)
    }
    private fun validateRoutine(r: FocusRoutine) {
        validateId(r.id)
        require(r.name.isNotBlank() && r.name.length <= 100 && r.workMinutes in 1..240 && r.restMinutes in 1..60 && r.createdAt > 0)
    }
    private fun encodeSessions(items: List<FocusSession>): JSONArray = JSONArray().also { array ->
        items.forEach { s -> array.put(JSONObject().put("id", s.id).put("routineId", s.routineId ?: JSONObject.NULL)
            .put("label", s.label).put("plannedSeconds", s.plannedSeconds).put("elapsedSeconds", s.elapsedSeconds)
            .put("startedAt", s.startedAt).put("finishedAt", s.finishedAt ?: JSONObject.NULL)
            .put("status", if (s.status == "running") "paused" else s.status)) }
    }
    private fun encodeRoutines(items: List<FocusRoutine>): JSONArray = JSONArray().also { array ->
        items.forEach { r -> array.put(JSONObject().put("id", r.id).put("name", r.name).put("workMinutes", r.workMinutes)
            .put("restMinutes", r.restMinutes).put("createdAt", r.createdAt)) }
    }
    private fun decodeSessions(array: JSONArray): List<FocusSession> {
        require(array.length() <= 20_000)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            FocusSession(o.getString("id"), if (o.isNull("routineId")) null else o.getString("routineId"), o.getString("label"), o.getInt("plannedSeconds"), o.getInt("elapsedSeconds"), o.getLong("startedAt"), if (o.isNull("finishedAt")) null else o.getLong("finishedAt"), o.getString("status")).also(::validateSession)
        }.also { require(it.map { s -> s.id }.distinct().size == it.size) }
    }
    private fun decodeRoutines(array: JSONArray): List<FocusRoutine> {
        require(array.length() <= 2_000)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            FocusRoutine(o.getString("id"), o.getString("name"), o.getInt("workMinutes"), o.getInt("restMinutes"), o.getLong("createdAt")).also(::validateRoutine)
        }.also { require(it.map { r -> r.id }.distinct().size == it.size) }
    }
}
