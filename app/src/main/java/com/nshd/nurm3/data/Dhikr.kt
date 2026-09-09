package com.nshd.nurm3.data

import androidx.room.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Personal counter goals are user choices, not prescribed religious counts. */
@Entity(tableName = "dhikr_phrases", indices = [Index(value = ["position", "createdAt"])])
data class DhikrPhrase(
    @PrimaryKey val id: String,
    val title: String,
    val target: Int,
    val sessionCount: Long,
    val position: Int,
    val createdAt: Long,
    val archived: Boolean = false
)

@Entity(
    tableName = "dhikr_days",
    primaryKeys = ["phraseId", "localDate"],
    foreignKeys = [ForeignKey(entity = DhikrPhrase::class, parentColumns = ["id"], childColumns = ["phraseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("phraseId"), Index("localDate")]
)
data class DhikrDay(val phraseId: String, val localDate: String, val count: Long)

data class DhikrSnapshot(val phrase: DhikrPhrase, val todayCount: Long, val lifetimeCount: Long)

object DhikrRules {
    const val MAX_COUNT = 1_000_000_000_000L
    const val MAX_TARGET = 100_000
    fun valid(phrase: DhikrPhrase): Boolean = phrase.id.isNotBlank() && phrase.id.length <= 128 &&
        phrase.title.isNotBlank() && phrase.title.length <= 200 && phrase.target in 1..MAX_TARGET &&
        phrase.sessionCount in 0..MAX_COUNT && phrase.createdAt >= 0
    fun progress(count: Long, target: Int): Float =
        if (target <= 0) 0f else (count.coerceAtLeast(0).coerceAtMost(target.toLong()).toFloat() / target).coerceIn(0f, 1f)
    fun total(days: List<DhikrDay>): Long = days.fold(0L) { total, day -> Math.addExact(total, day.count) }
    fun canIncrement(session: Long, daily: Long): Boolean = session in 0 until MAX_COUNT && daily in 0 until MAX_COUNT
    fun next(session: Long, daily: Long): Pair<Long, Long> {
        require(canIncrement(session, daily)) { "Counter limit reached" }
        return session + 1 to daily + 1
    }
}

@Dao
interface DhikrDao {
    @Query("SELECT * FROM dhikr_phrases ORDER BY position, createdAt")
    fun observePhrases(): Flow<List<DhikrPhrase>>
    @Query("SELECT * FROM dhikr_days ORDER BY localDate DESC")
    fun observeDays(): Flow<List<DhikrDay>>
    @Query("SELECT * FROM dhikr_phrases ORDER BY position, createdAt")
    suspend fun getAllPhrases(): List<DhikrPhrase>
    @Query("SELECT * FROM dhikr_days ORDER BY localDate DESC")
    suspend fun getAllDays(): List<DhikrDay>
    @Query("SELECT * FROM dhikr_phrases WHERE id = :id LIMIT 1")
    suspend fun getPhrase(id: String): DhikrPhrase?
    @Query("SELECT * FROM dhikr_days WHERE phraseId = :id AND localDate = :date LIMIT 1")
    suspend fun getDay(id: String, date: String): DhikrDay?
    @Upsert
    suspend fun savePhrase(phrase: DhikrPhrase)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhraseIgnoringConflict(phrase: DhikrPhrase): Long
    @Upsert
    suspend fun saveDay(day: DhikrDay)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDayIgnoringConflict(day: DhikrDay): Long
    @Query("UPDATE dhikr_phrases SET sessionCount = 0 WHERE id = :id AND archived = 0")
    suspend fun resetSession(id: String)
    @Query("UPDATE dhikr_phrases SET archived = 1 WHERE id = :id")
    suspend fun archivePhrase(id: String)
    @Query("DELETE FROM dhikr_days")
    suspend fun clearDaysForRestore()
    @Query("DELETE FROM dhikr_phrases")
    suspend fun clearPhrasesForRestore()

    @Transaction
    suspend fun increment(id: String, date: String): Boolean {
        val phrase = getPhrase(id) ?: return false
        if (phrase.archived) return false
        val current = getDay(id, date)?.count ?: 0L
        if (!DhikrRules.canIncrement(phrase.sessionCount, current)) return false
        val (session, daily) = DhikrRules.next(phrase.sessionCount, current)
        savePhrase(phrase.copy(sessionCount = session))
        saveDay(DhikrDay(id, date, daily))
        return true
    }

    @Transaction
    suspend fun seedDefaults() {
        listOf("SubhanAllah", "Alhamdulillah", "Allahu Akbar").forEachIndexed { index, title ->
            val id = "dhikr-${index + 1}"
            if (getPhrase(id) == null) savePhrase(DhikrPhrase(id, title, 33, 0, index, System.currentTimeMillis()))
        }
    }
}

class DhikrRepository(private val dao: DhikrDao) {
    val phrases: Flow<List<DhikrPhrase>> = dao.observePhrases()
    val days: Flow<List<DhikrDay>> = dao.observeDays()

    fun snapshots(date: LocalDate): Flow<List<DhikrSnapshot>> = combine(phrases, days) { phrases, days ->
        val daily = days.filter { it.localDate == date.toString() }.associate { it.phraseId to it.count }
        val totals = days.groupBy { it.phraseId }.mapValues { DhikrRules.total(it.value) }
        phrases.filterNot { it.archived }.map { DhikrSnapshot(it, daily[it.id] ?: 0, totals[it.id] ?: 0) }
    }

    suspend fun add(title: String, target: Int) {
        val phrase = DhikrPhrase(UUID.randomUUID().toString(), title.trim(), target, 0, dao.getAllPhrases().size, System.currentTimeMillis())
        require(DhikrRules.valid(phrase)) { "Enter a phrase and a valid personal target" }
        dao.savePhrase(phrase)
    }

    suspend fun update(id: String, title: String, target: Int) {
        val existing = dao.getPhrase(id) ?: return
        if (existing.archived) return
        val updated = existing.copy(title = title.trim(), target = target)
        require(DhikrRules.valid(updated)) { "Enter a phrase and a valid personal target" }
        dao.savePhrase(updated)
    }

    suspend fun increment(id: String, date: LocalDate): Boolean = dao.increment(id, date.toString())
    suspend fun resetSession(id: String) = dao.resetSession(id)
    suspend fun archive(id: String) = dao.archivePhrase(id)
    suspend fun restore(id: String) {
        val existing = dao.getPhrase(id) ?: return
        if (existing.archived) dao.savePhrase(existing.copy(archived = false))
    }
    suspend fun seedDefaults() = dao.seedDefaults()
}
