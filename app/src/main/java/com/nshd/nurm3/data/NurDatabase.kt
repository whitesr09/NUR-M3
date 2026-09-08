package com.nshd.nurm3.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries", indices = [Index(value = ["kind", "position"])])
data class Entry(
    @PrimaryKey val id: String,
    val kind: String,
    val title: String,
    val position: Int,
    val createdAt: Long,
    val archived: Boolean = false
)

@Entity(tableName = "completions", primaryKeys = ["entryId", "localDate"], indices = [Index("localDate")])
data class Completion(
    val entryId: String,
    val localDate: String,
    val completedAt: Long
)

@Dao
interface NurDao {
    @Query("SELECT * FROM entries WHERE archived = 0 ORDER BY position, createdAt")
    fun observeEntries(): Flow<List<Entry>>

    @Query("SELECT * FROM completions ORDER BY localDate DESC")
    fun observeCompletions(): Flow<List<Completion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEntry(entry: Entry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCompletion(completion: Completion)

    @Query("DELETE FROM completions WHERE entryId = :id AND localDate = :date")
    suspend fun removeCompletion(id: String, date: String)

    @Query("UPDATE entries SET archived = 1 WHERE id = :id")
    suspend fun archiveEntry(id: String)

    @Query("DELETE FROM completions WHERE entryId = :id")
    suspend fun deleteCompletions(id: String)

    @Transaction
    suspend fun deleteEntry(id: String) {
        archiveEntry(id)
        deleteCompletions(id)
    }
}

@Database(entities = [Entry::class, Completion::class], version = 1, exportSchema = true)
abstract class NurDatabase : RoomDatabase() {
    abstract fun dao(): NurDao
    companion object {
        @Volatile private var instance: NurDatabase? = null
        fun get(context: Context): NurDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, NurDatabase::class.java, "nur-m3.db")
                .build().also { instance = it }
        }
    }
}
