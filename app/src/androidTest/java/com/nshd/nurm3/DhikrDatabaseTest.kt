package com.nshd.nurm3

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nshd.nurm3.data.*
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DhikrDatabaseTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun incrementsResetArchiveAndRestorePreserveHistory() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NurDatabase::class.java).build()
        try {
            val dao = db.dhikrDao()
            val repo = DhikrRepository(dao)
            val day = LocalDate.of(2026, 9, 9)
            repo.seedDefaults()
            assertEquals(3, dao.getAllPhrases().size)
            assertTrue(repo.increment("dhikr-1", day))
            assertTrue(repo.increment("dhikr-1", day))
            assertTrue(repo.increment("dhikr-1", day.minusDays(1)))
            assertEquals(3L, dao.getPhrase("dhikr-1")!!.sessionCount)
            assertEquals(2L, dao.getDay("dhikr-1", day.toString())!!.count)
            repo.update("dhikr-1", "My label", 100)
            assertEquals(2L, dao.getDay("dhikr-1", day.toString())!!.count)
            repo.resetSession("dhikr-1")
            assertEquals(0L, dao.getPhrase("dhikr-1")!!.sessionCount)
            assertEquals(3L, DhikrRules.total(dao.getAllDays()))
            repo.archive("dhikr-1")
            assertFalse(repo.increment("dhikr-1", day))
            assertEquals(3L, DhikrRules.total(dao.getAllDays()))
            repo.restore("dhikr-1")
            assertTrue(repo.increment("dhikr-1", day))
            assertEquals(3L, dao.getDay("dhikr-1", day.toString())!!.count)
            assertEquals(4L, DhikrRules.total(dao.getAllDays()))
            repo.seedDefaults()
            assertEquals(4L, DhikrRules.total(dao.getAllDays()))
            assertEquals("My label", dao.getPhrase("dhikr-1")!!.title)
        } finally { db.close() }
    }

    @Test fun migrationFromVersionTwoKeepsOldEntriesAndCompletionSnapshots() = runBlocking {
        val name = "nur-dhikr-migration-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS entries (id TEXT NOT NULL, kind TEXT NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL, createdAt INTEGER NOT NULL, archived INTEGER NOT NULL, schedule TEXT NOT NULL DEFAULT 'daily', weekdaysMask INTEGER NOT NULL DEFAULT 127, startDate TEXT, endDate TEXT, PRIMARY KEY(id))")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_entries_kind_position ON entries (kind, position)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS completions (entryId TEXT NOT NULL, localDate TEXT NOT NULL, completedAt INTEGER NOT NULL, titleSnapshot TEXT NOT NULL DEFAULT '', kindSnapshot TEXT NOT NULL DEFAULT '', PRIMARY KEY(entryId, localDate))")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_completions_localDate ON completions (localDate)")
                    db.execSQL("INSERT INTO entries (id, kind, title, position, createdAt, archived, schedule, weekdaysMask, startDate) VALUES ('old-task', 'amanah', 'Read', 0, 1, 0, 'daily', 127, '2026-09-08')")
                    db.execSQL("INSERT INTO completions (entryId, localDate, completedAt, titleSnapshot, kindSnapshot) VALUES ('old-task', '2026-09-08', 2, 'Read', 'amanah')")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("Unexpected upgrade")
            }).build()
        )
        helper.writableDatabase
        helper.close()
        val db = Room.databaseBuilder(context, NurDatabase::class.java, name)
            .addMigrations(NurDatabase.MIGRATION_1_2, NurDatabase.MIGRATION_2_3).build()
        try {
            assertEquals("Read", db.dao().getAllEntries().single().title)
            assertEquals("Read", db.dao().getAllCompletions().single().titleSnapshot)
            val dhikr = db.dhikrDao()
            dhikr.seedDefaults()
            assertEquals(3, dhikr.getAllPhrases().size)
            assertTrue(dhikr.increment("dhikr-1", "2026-09-09"))
            assertEquals(1L, dhikr.getDay("dhikr-1", "2026-09-09")!!.count)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
