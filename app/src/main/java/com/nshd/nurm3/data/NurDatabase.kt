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
    suspend fun getAllEntries(): List<Entry>
    @Query("SELECT * FROM completions ORDER BY localDate DESC, completedAt DESC")
    suspend fun getAllCompletions(): List<Completion>
    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    suspend fun getEntry(id: String): Entry?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEntry(entry: Entry)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntryIgnoringConflict(entry: Entry): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCompletion(completion: Completion)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletionIgnoringConflict(completion: Completion): Long
    @Query("DELETE FROM completions WHERE entryId = :id AND localDate = :date")
    suspend fun removeCompletion(id: String, date: String)
    @Query("UPDATE entries SET archived = 1 WHERE id = :id AND kind != 'prayer'")
    suspend fun archiveEntry(id: String)
    @Query("DELETE FROM completions WHERE entryId = :id")
    suspend fun deleteCompletions(id: String)
    @Query("DELETE FROM completions")
    suspend fun clearCompletionsForRestore()
    @Query("DELETE FROM entries WHERE kind != 'prayer'")
    suspend fun clearNonPrayerEntriesForRestore()
    @Query("SELECT COUNT(*) FROM entries WHERE kind = 'prayer'")
    suspend fun prayerCount(): Int

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

@Database(entities = [Entry::class, Completion::class, DhikrPhrase::class, DhikrDay::class], version = 3, exportSchema = true)
abstract class NurDatabase : RoomDatabase() {
    abstract fun dao(): NurDao
    abstract fun dhikrDao(): DhikrDao
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
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS dhikr_phrases (id TEXT NOT NULL, title TEXT NOT NULL, target INTEGER NOT NULL, sessionCount INTEGER NOT NULL, position INTEGER NOT NULL, createdAt INTEGER NOT NULL, archived INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dhikr_phrases_position_createdAt ON dhikr_phrases (position, createdAt)")
                db.execSQL("CREATE TABLE IF NOT EXISTS dhikr_days (phraseId TEXT NOT NULL, localDate TEXT NOT NULL, count INTEGER NOT NULL, PRIMARY KEY(phraseId, localDate), FOREIGN KEY(phraseId) REFERENCES dhikr_phrases(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dhikr_days_phraseId ON dhikr_days (phraseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dhikr_days_localDate ON dhikr_days (localDate)")
            }
        }
        @Volatile private var instance: NurDatabase? = null
        fun get(context: Context): NurDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, NurDatabase::class.java, "nur-m3.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
        }
    }
}
