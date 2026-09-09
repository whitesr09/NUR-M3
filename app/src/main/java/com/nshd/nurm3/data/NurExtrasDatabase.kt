package com.nshd.nurm3.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** New feature storage. The existing v3 database and its migration history remain untouched. */
@Entity(tableName = "task_details")
data class TaskDetails(
    @PrimaryKey val entryId: String,
    val priority: Int = 2,
    val category: String = "",
    val notes: String = "",
    val dueAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subtasks", indices = [Index("entryId")])
data class Subtask(
    @PrimaryKey val id: String,
    val entryId: String,
    val title: String,
    val position: Int,
    val archived: Boolean = false
)

@Entity(tableName = "subtask_checks", primaryKeys = ["subtaskId", "localDate"],
    foreignKeys = [ForeignKey(entity = Subtask::class, parentColumns = ["id"], childColumns = ["subtaskId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("subtaskId"), Index("localDate")])
data class SubtaskCheck(val subtaskId: String, val localDate: String, val completedAt: Long)

@Entity(tableName = "task_templates")
data class TaskTemplate(
    @PrimaryKey val id: String,
    val title: String,
    val priority: Int = 2,
    val category: String = "",
    val notes: String = "",
    val subtasksJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_reminders", indices = [Index("entryId"), Index("triggerAt")])
data class LocalReminder(
    @PrimaryKey val id: String,
    val entryId: String? = null,
    val title: String,
    val triggerAt: Long,
    val zoneId: String,
    val repeatRule: String = "none",
    val enabled: Boolean = true,
    val snoozedUntil: Long? = null,
    val lastDeliveredAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "prayer_profile")
data class PrayerProfile(
    @PrimaryKey val id: String = "default",
    val locationLabel: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String,
    val method: String,
    val madhhab: String = "shafi",
    val offsetsJson: String = "[0,0,0,0,0,0]",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_marks", indices = [Index("verseId")])
data class ReadingMark(
    @PrimaryKey val id: String,
    val verseId: String,
    val kind: String,
    val collection: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface NurExtrasDao {
    @Query("SELECT * FROM task_details") fun observeTaskDetails(): Flow<List<TaskDetails>>
    @Query("SELECT * FROM task_details WHERE entryId = :id LIMIT 1") suspend fun taskDetails(id: String): TaskDetails?
    @Upsert suspend fun saveTaskDetails(value: TaskDetails)
    @Query("SELECT * FROM subtasks WHERE entryId = :id AND archived = 0 ORDER BY position, id") fun observeSubtasks(id: String): Flow<List<Subtask>>
    @Query("SELECT * FROM subtasks WHERE entryId = :id ORDER BY position, id") suspend fun allSubtasks(id: String): List<Subtask>
    @Query("SELECT * FROM subtasks WHERE id = :id LIMIT 1") suspend fun subtask(id: String): Subtask?
    @Upsert suspend fun saveSubtask(value: Subtask)
    @Query("UPDATE subtasks SET archived = 1 WHERE id = :id") suspend fun archiveSubtask(id: String)
    @Query("SELECT * FROM subtask_checks WHERE localDate = :date") fun observeSubtaskChecks(date: String): Flow<List<SubtaskCheck>>
    @Query("SELECT * FROM subtask_checks WHERE subtaskId = :id AND localDate = :date LIMIT 1") suspend fun subtaskCheck(id: String, date: String): SubtaskCheck?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSubtaskCheck(value: SubtaskCheck): Long
    @Query("DELETE FROM subtask_checks WHERE subtaskId = :id AND localDate = :date") suspend fun removeSubtaskCheck(id: String, date: String)
    @Query("SELECT * FROM task_templates ORDER BY createdAt DESC") fun observeTemplates(): Flow<List<TaskTemplate>>
    @Query("SELECT * FROM task_templates WHERE id = :id LIMIT 1") suspend fun template(id: String): TaskTemplate?
    @Upsert suspend fun saveTemplate(value: TaskTemplate)
    @Query("DELETE FROM task_templates WHERE id = :id") suspend fun deleteTemplate(id: String)

    @Query("SELECT * FROM local_reminders ORDER BY triggerAt") fun observeReminders(): Flow<List<LocalReminder>>
    @Query("SELECT * FROM local_reminders WHERE id = :id LIMIT 1") suspend fun reminder(id: String): LocalReminder?
    @Upsert suspend fun saveReminder(value: LocalReminder)
    @Query("DELETE FROM local_reminders WHERE id = :id") suspend fun deleteReminder(id: String)
    @Query("UPDATE local_reminders SET lastDeliveredAt = :deliveredAt WHERE id = :id AND enabled = 1 AND triggerAt = :expected AND (lastDeliveredAt IS NULL OR lastDeliveredAt != :expected)")
    suspend fun claimReminder(id: String, expected: Long, deliveredAt: Long): Int

    @Query("SELECT * FROM prayer_profile WHERE id = 'default' LIMIT 1") fun observePrayerProfile(): Flow<PrayerProfile?>
    @Query("SELECT * FROM prayer_profile WHERE id = 'default' LIMIT 1") suspend fun prayerProfile(): PrayerProfile?
    @Upsert suspend fun savePrayerProfile(value: PrayerProfile)
    @Query("DELETE FROM prayer_profile WHERE id = 'default'") suspend fun clearPrayerProfile()

    @Query("SELECT * FROM reading_marks ORDER BY updatedAt DESC") fun observeReadingMarks(): Flow<List<ReadingMark>>
    @Query("SELECT * FROM reading_marks WHERE id = :id LIMIT 1") suspend fun readingMark(id: String): ReadingMark?
    @Upsert suspend fun saveReadingMark(value: ReadingMark)
    @Query("DELETE FROM reading_marks WHERE id = :id") suspend fun removeReadingMark(id: String)
}

@Database(entities = [TaskDetails::class, Subtask::class, SubtaskCheck::class, TaskTemplate::class,
    LocalReminder::class, PrayerProfile::class, ReadingMark::class], version = 1, exportSchema = true)
abstract class NurExtrasDatabase : RoomDatabase() {
    abstract fun dao(): NurExtrasDao
    companion object {
        @Volatile private var instance: NurExtrasDatabase? = null
        fun get(context: Context): NurExtrasDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, NurExtrasDatabase::class.java, "nur-m3-extras.db")
                .build().also { instance = it }
        }
    }
}
