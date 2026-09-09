package com.nshd.nurm3.data

import androidx.room.withTransaction
import java.time.LocalDate
import java.util.UUID
import org.json.JSONArray

/** Extra task records never replace the original entry or its dated completion history. */
class AmanahRepository(private val extras: NurExtrasDatabase, private val original: NurDatabase) {
    private val dao = extras.dao()
    val details = dao.observeTaskDetails()
    val templates = dao.observeTemplates()
    fun subtasks(entryId: String) = dao.observeSubtasks(entryId)
    fun checks(date: LocalDate) = dao.observeSubtaskChecks(date.toString())

    suspend fun saveDetails(value: TaskDetails): Boolean {
        val parent = original.dao().getEntry(value.entryId) ?: return false
        if (parent.kind != NurKind.AMANAH || parent.archived) return false
        require(value.priority in 1..3 && value.category.length <= 80 && value.notes.length <= 10_000)
        require(value.dueAt == null || value.dueAt >= 0L)
        dao.saveTaskDetails(value.copy(category = value.category.trim(), updatedAt = System.currentTimeMillis()))
        return true
    }

    suspend fun addSubtask(entryId: String, title: String): Boolean {
        val parent = original.dao().getEntry(entryId) ?: return false
        if (parent.kind != NurKind.AMANAH || parent.archived) return false
        require(title.isNotBlank() && title.length <= 200)
        val rows = dao.allSubtasks(entryId)
        require(rows.count { !it.archived } < 100) { "Maximum 100 active subtasks" }
        dao.saveSubtask(Subtask(UUID.randomUUID().toString(), entryId, title.trim(), (rows.maxOfOrNull { it.position } ?: -1) + 1))
        return true
    }

    suspend fun updateSubtask(id: String, title: String): Boolean {
        val current = dao.subtask(id) ?: return false
        if (current.archived || title.isBlank() || title.length > 200) return false
        val parent = original.dao().getEntry(current.entryId) ?: return false
        if (parent.archived) return false
        dao.saveSubtask(current.copy(title = title.trim()))
        return true
    }

    suspend fun archiveSubtask(id: String) = dao.archiveSubtask(id)

    suspend fun checkSubtask(id: String, date: LocalDate, completed: Boolean): Boolean {
        if (date != LocalDate.now()) return false
        return extras.withTransaction {
            val child = dao.subtask(id) ?: return@withTransaction false
            val parent = original.dao().getEntry(child.entryId) ?: return@withTransaction false
            if (child.archived || !EntrySchedule.isActive(parent, date)) return@withTransaction false
            if (completed) dao.insertSubtaskCheck(SubtaskCheck(id, date.toString(), System.currentTimeMillis()))
            else dao.removeSubtaskCheck(id, date.toString())
            true
        }
    }

    suspend fun saveTemplate(entryId: String, name: String): Boolean {
        val parent = original.dao().getEntry(entryId) ?: return false
        if (parent.kind != NurKind.AMANAH || name.isBlank() || name.length > 200) return false
        val details = dao.taskDetails(entryId) ?: TaskDetails(entryId)
        val children = dao.allSubtasks(entryId).filterNot { it.archived }
        val json = JSONArray().apply { children.forEach { put(it.title) } }.toString()
        dao.saveTemplate(TaskTemplate(UUID.randomUUID().toString(), name.trim(), details.priority, details.category, details.notes, json))
        return true
    }

    suspend fun applyTemplate(templateId: String, date: LocalDate): String? {
        val template = dao.template(templateId) ?: return null
        val children = JSONArray(template.subtasksJson)
        require(children.length() <= 100)
        val parent = Entry(UUID.randomUUID().toString(), NurKind.AMANAH, template.title, 0, System.currentTimeMillis(), startDate = date.toString())
        val saved = NurRepository(original.dao()).saveNew(parent)
        if (!saved) return null
        // saveNew generates its own stable ID; use the newly inserted row's identity instead.
        val created = original.dao().getAllEntries().filter { it.kind == NurKind.AMANAH && it.createdAt >= parent.createdAt }.maxByOrNull { it.createdAt } ?: return null
        extras.withTransaction {
            dao.saveTaskDetails(TaskDetails(created.id, template.priority, template.category, template.notes))
            for (i in 0 until children.length()) {
                val title = children.getString(i)
                if (title.isNotBlank() && title.length <= 200) dao.saveSubtask(Subtask(UUID.randomUUID().toString(), created.id, title, i))
            }
        }
        return created.id
    }
}
