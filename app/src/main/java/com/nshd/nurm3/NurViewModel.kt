package com.nshd.nurm3

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nshd.nurm3.data.*
import com.nshd.nurm3.ui.JourneyLayout
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NurViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NurDatabase.get(application)
    private val repo = NurRepository(database.dao())
    private val dhikrRepo = DhikrRepository(database.dhikrDao())
    private val settings = NurSettings(application)
    val preferences = settings.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NurPreferences())
    val entries = repo.entries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allEntries = repo.allEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val completions = repo.completions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()
    val dhikrPhrases = dhikrRepo.phrases.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    @OptIn(ExperimentalCoroutinesApi::class)
    val dhikrSnapshots = today.flatMapLatest { dhikrRepo.snapshots(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val completionGate = CompletionGate()
    private val _completionPending = MutableStateFlow<Set<String>>(emptySet())
    val completionPending: StateFlow<Set<String>> = _completionPending.asStateFlow()
    private val _uiEvents = MutableSharedFlow<String>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            val dao = database.dao()
            if (dao.prayerCount() == 0) {
                listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEachIndexed { index, title ->
                    dao.saveEntry(Entry("prayer-${index + 1}", NurKind.PRAYER, title, index, System.currentTimeMillis()))
                }
            }
            dhikrRepo.seedDefaults()
        }
    }

    fun refreshDate() { _today.value = LocalDate.now() }
    fun add(kind: String, title: String) = viewModelScope.launch { repo.add(kind, title, entries.value.size, today.value) }
    fun addEntry(entry: Entry) = viewModelScope.launch { repo.saveNew(entry.copy(position = entries.value.size)) }
    fun updateEntry(entry: Entry) = viewModelScope.launch { repo.updateEntry(entry) }

    /** Persist first; the database flow is the only authority for a checked state. */
    fun complete(id: String, date: LocalDate, checked: Boolean) {
        val key = completionGate.key(id, date)
        if (!completionGate.acquire(key)) return
        _completionPending.update { it + key }
        viewModelScope.launch {
            try {
                if (date != LocalDate.now()) {
                    refreshDate()
                    _uiEvents.emit("The day changed. Refreshing today's checklist.")
                } else if (!repo.setCompleted(id, date, checked)) {
                    _uiEvents.emit("This entry is no longer scheduled for today.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiEvents.emit("Could not save. Your previous completion is unchanged.")
            } finally {
                completionGate.release(key)
                _completionPending.update { it - key }
            }
        }
    }

    fun delete(id: String) = viewModelScope.launch { repo.delete(id) }
    fun setting(key: String, value: Boolean) = viewModelScope.launch { settings.update(key, value) }
    fun choice(key: String, value: String) = viewModelScope.launch { settings.updateChoice(key, value) }
    fun typeScale(value: Float) = viewModelScope.launch { settings.updateScale(value) }
    fun journey(layout: JourneyLayout) = viewModelScope.launch { settings.saveJourney(layout) }
    fun resetAppearance() = viewModelScope.launch { settings.resetAppearance() }

    /** The actual local date is captured when the user taps, not when a screen was opened. */
    suspend fun incrementDhikr(id: String): Boolean {
        val saved = dhikrRepo.increment(id, LocalDate.now())
        if (saved) refreshDate()
        return saved
    }
    suspend fun addDhikr(title: String, target: Int) = dhikrRepo.add(title, target)
    suspend fun updateDhikr(id: String, title: String, target: Int) = dhikrRepo.update(id, title, target)
    suspend fun resetDhikrSession(id: String) = dhikrRepo.resetSession(id)
    suspend fun archiveDhikr(id: String) = dhikrRepo.archive(id)
    suspend fun restoreDhikr(id: String) = dhikrRepo.restore(id)
}
