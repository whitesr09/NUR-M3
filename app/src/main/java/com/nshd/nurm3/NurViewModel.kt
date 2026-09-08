package com.nshd.nurm3

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nshd.nurm3.data.*
import com.nshd.nurm3.ui.JourneyLayout
import java.time.LocalDate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NurViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NurDatabase.get(application)
    private val repo = NurRepository(database.dao())
    private val settings = NurSettings(application)
    private val backupService = BackupService(application)
    val preferences = settings.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NurPreferences())
    val entries = repo.entries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allEntries = repo.allEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val completions = repo.completions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    init {
        viewModelScope.launch {
            val dao = database.dao()
            if (dao.prayerCount() == 0) {
                listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEachIndexed { index, title ->
                    dao.saveEntry(Entry("prayer-${index + 1}", NurKind.PRAYER, title, index, System.currentTimeMillis()))
                }
            }
        }
    }

    fun refreshDate() { _today.value = LocalDate.now() }
    fun add(kind: String, title: String) = viewModelScope.launch { repo.add(kind, title, entries.value.size, today.value) }
    fun addEntry(entry: Entry) = viewModelScope.launch { repo.saveNew(entry.copy(position = entries.value.size)) }
    fun updateEntry(entry: Entry) = viewModelScope.launch { repo.updateEntry(entry) }
    fun complete(id: String, date: LocalDate, checked: Boolean) = viewModelScope.launch { repo.setCompleted(id, date, checked) }
    fun delete(id: String) = viewModelScope.launch { repo.delete(id) }
    fun setting(key: String, value: Boolean) = viewModelScope.launch { settings.update(key, value) }
    fun choice(key: String, value: String) = viewModelScope.launch { settings.updateChoice(key, value) }
    fun typeScale(value: Float) = viewModelScope.launch { settings.updateScale(value) }
    fun journey(layout: JourneyLayout) = viewModelScope.launch { settings.saveJourney(layout) }
    fun resetAppearance() = viewModelScope.launch { settings.resetAppearance() }

    suspend fun exportBackup(): String = backupService.export()
    fun inspectBackup(raw: String): BackupSummary = BackupCodec.inspect(raw)
    suspend fun importBackup(raw: String, replace: Boolean): ImportResult = backupService.import(raw, replace)
}
