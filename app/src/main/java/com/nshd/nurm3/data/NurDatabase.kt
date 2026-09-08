package com.nshd.nurm3.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries", indices = [Index(value = ["kind", "position"])])
data class Entry(
    @PrimaryKey val id: String,
    val kind: String,
    val title: String,
    val position: Int,
    val createdAt: Long,
    val archived: Boolean = false,
    @ColumnInfo(defaultValue = "'daily'") val schedule: String = "daily",
    @ColumnInfo(defaultValue = "127") val weekdaysMask: Int = 127,
    val startDate: String? = null,
    val endDate: String? = null
)

@Entity(tableName = "completions", primaryKeys = ["entryId", "localDate"], indices = [Index("localDate")])
data class Completion(
    val entryId: String,
    val localDate: String,
    val completedAt: Long,
    @ColumnInfo(defaultValue = "''") val titleSnapshot: String = "",
    @ColumnInfo(defaultValue = "''") val kindSnapshot: String = ""
)

@Dao
interface NurDao {
    @Query("SELECT * FROM entries WHERE archived = 0 ORDER BY position, createdAt")
    fun observeEntries(): Flow<List<Entry>>
    @Query("SELECT * FROM entries ORDER BY position, createdAt")
    fun observeAllEntries(): Flow<List<Entry>>
    @Query("SELECT * FROM completions ORDER BY localDate DESC, completedAt DESC")
    fun observeCompletions(): Flow<List<Completion>>
    @Query("SELECT * FROM entries ORDER BY position, createdAt")
    suspend fun snapshotEntries(): List<Entry>
    @Query("SELECT * FROM completions ORDER BY localDate, completedAt")
    suspend fun snapshotCompletions(): List<Completion>
    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    suspend fun getEntry(id: String): Entry?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEntry(entry: Entry)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCompletion(completion: Completion)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntriesIfAbsent(entries: List<Entry>): List<Long>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletionsIfAbsent(completions: List<Completion>): List<Long>
    @Query("DELETE FROM completions WHERE entryId = :id AND localDate = :date")
    suspend fun removeCompletion(id: String, date: String)
    @Query("UPDATE entries SET archived = 1 WHERE id = :id AND kind != 'prayer'")
    suspend fun archiveEntry(id: String)
    @Query("DELETE FROM completions WHERE entryId = :id")
    suspend fun deleteCompletions(id: String)
    @Query("SELECT COUNT(*) FROM entries WHERE kind = 'prayer'")
    suspend fun prayerCount(): Int
    @Query("DELETE FROM completions")
    suspend fun clearCompletionsForRestore()
    @Query("DELETE FROM entries")
    suspend fun clearEntriesForRestore()

    @Transaction
    suspend fun recordCompletion(id: String, date: String, completed: Boolean, timestamp: Long) {
        val entry = getEntry(id) ?: return
        if (entry.archived) return
        if (completed) saveCompletion(Completion(id, date, timestamp, entry.title, entry.kind))
        else removeCompletion(id, date)
    }

    /** Archive definitions without deleting historical evidence. */
    @Transaction
    suspend fun deleteEntry(id: String) = archiveEntry(id)
}

@Database(entities = [Entry::class, Completion::class], version = 2, exportSchema = true)
abstract class NurDatabase : RoomDatabase() {
    abstract fun dao(): NurDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN schedule TEXT NOT NULL DEFAULT 'daily'")
                db.execSQL("ALTER TABLE entries ADD COLUMN weekdaysMask INTEGER NOT NULL DEFAULT 127")
                db.execSQL("ALTER TABLE entries ADD COLUMN startDate TEXT")
                db.execSQL("ALTER TABLE entries ADD COLUMN endDate TEXT")
                db.execSQL("ALTER TABLE completions ADD COLUMN titleSnapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE completions ADD COLUMN kindSnapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE completions SET titleSnapshot = COALESCE((SELECT title FROM entries WHERE entries.id = completions.entryId), ''), kindSnapshot = COALESCE((SELECT kind FROM entries WHERE entries.id = completions.entryId), '')")
            }
        }
        @Volatile private var instance: NurDatabase? = null
        fun get(context: Context): NurDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, NurDatabase::class.java, "nur-m3.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }
    }
}
