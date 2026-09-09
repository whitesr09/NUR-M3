package com.nshd.nurm3.focus

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "focus_sessions", indices = [Index("startedAt"), Index("routineId")])
data class FocusSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val routineId: String? = null,
    val label: String,
    val plannedSeconds: Int,
    val elapsedSeconds: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val status: String = "paused"
)

@Entity(tableName = "focus_routines")
data class FocusRoutine(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val workMinutes: Int = 25,
    val restMinutes: Int = 5,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<FocusSession>>
    @Query("SELECT * FROM focus_routines ORDER BY createdAt, name")
    fun observeRoutines(): Flow<List<FocusRoutine>>
    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun session(id: String): FocusSession?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: FocusSession)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRoutine(routine: FocusRoutine)
    @Query("DELETE FROM focus_routines WHERE id = :id")
    suspend fun deleteRoutine(id: String)
    @Query("SELECT * FROM focus_sessions WHERE status = 'running' ORDER BY startedAt DESC")
    suspend fun interruptedSessions(): List<FocusSession>
    @Query("UPDATE focus_sessions SET status = 'paused' WHERE status = 'running'")
    suspend fun pauseInterrupted()
}

@Database(entities = [FocusSession::class, FocusRoutine::class], version = 1, exportSchema = true)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun dao(): FocusDao
    companion object {
        @Volatile private var instance: FocusDatabase? = null
        fun get(context: Context): FocusDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, FocusDatabase::class.java, "nur-focus.db")
                .build().also { instance = it }
        }
    }
}

/** Pure rules shared by the timer, persistence layer and unit tests. */
object FocusRules {
    fun validMinutes(minutes: Int) = minutes in 1..240
    fun remaining(planned: Int, elapsed: Int) = (planned - elapsed).coerceAtLeast(0)
    fun advance(elapsed: Int, delta: Int, planned: Int): Int =
        (elapsed.toLong() + delta.coerceAtLeast(0)).coerceIn(0L, planned.toLong()).toInt()
    fun completed(session: FocusSession): Boolean =
        session.status == "completed" && session.finishedAt != null && session.elapsedSeconds >= session.plannedSeconds
}
