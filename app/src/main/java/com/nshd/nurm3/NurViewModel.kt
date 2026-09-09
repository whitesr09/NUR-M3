package com.nshd.nurm3

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nshd.nurm3.data.*
import com.nshd.nurm3.ui.JourneyLayout
import com.nshd.nurm3.ui.JourneyOptions
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NurViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NurDatabase.get(application)
    private val repo = NurRepository(database.dao())
    private val dhikrRepo = DhikrRepository(database.dhikrDao())
    private val settings = NurSettings(application)
    private val fontStore = NurFontStore(application)
    val preferences = settings.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NurPreferences())
    val entries = repo.entries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allEntries = repo.allEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val completions = repo.completions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()
    val dhikrPhrases = dhikrRepo.phrases.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val dhikrDays = dhikrRepo.days.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    @OptIn(ExperimentalCoroutinesApi::class)
    val dhikrSnapshots = today.flatMapLatest { dhikrRepo.snapshots(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val completionGate = CompletionGate()
    private val _completionPending = MutableStateFlow<Set<String>>(emptySet())
    val completionPending: StateFlow<Set<String>> = _completionPending.asStateFlow()
    private val _uiEvents = MutableSharedFlow<String>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()
    private val dhikrMutationMutex = Mutex()
    private val _dhikrPending = MutableStateFlow<Map<String, Int>>(emptyMap())
    val dhikrPending: StateFlow<Map<String, Int>> = _dhikrPending.asStateFlow()

    init {
        viewModelScope.launch {
            val dao = database.dao()
            if (dao.prayerCount() == 0) {
                listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEachIndexed { index, title ->
                    dao.saveEntry(Entry("prayer-${index + 1}", NurKind.PRAYER, title, index, System.currentTimeMillis()))
                }
            }
            dhikrMutationMutex.withLock { dhikrRepo.seedDefaults() }
        }
    }

    fun refreshDate() { _today.value = LocalDate.now() }
    fun add(kind: String, title: String) = viewModelScope.launch { repo.add(kind, title, entries.value.size, today.value) }
    fun addEntry(entry: Entry) = viewModelScope.launch { repo.saveNew(entry.copy(position = entries.value.size)) }
    fun updateEntry(entry: Entry) = viewModelScope.launch { repo.updateEntry(entry) }
    suspend fun persistEntry(entry: Entry, isNew: Boolean): Boolean =
        if (isNew) repo.saveNew(entry.copy(position = entries.value.size)) else repo.updateEntry(entry)
    suspend fun archiveEntry(id: String): Boolean = repo.delete(id)
    suspend fun restoreEntry(id: String): Boolean = repo.restore(id)

    /** Persist first; the database flow is the only authority for a checked state. */
    fun complete(id: String, date: LocalDate, checked: Boolean) {
        val key = CompletionGate.key(id, date)
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
    fun journeyOptions(transform: (JourneyOptions) -> JourneyOptions) = viewModelScope.launch { settings.updateJourneyOptions(transform) }
    fun journeyPreset(name: String) = viewModelScope.launch {
        try { settings.saveJourneyPreset(name); _uiEvents.emit("Journey preset saved.") }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { _uiEvents.emit(error.message ?: "Could not save the preset.") }
    }
    fun applyJourneyPreset() = viewModelScope.launch { settings.applyJourneyPreset() }
    fun applyJourneyLayout(layout: JourneyLayout) = viewModelScope.launch { settings.applyJourneyLayout(layout) }
    fun resetAppearance() = viewModelScope.launch { settings.resetAppearance() }

    /** Import into app-private storage before changing the selected font. */
    suspend fun importFont(uri: Uri): String {
        val imported = fontStore.import(uri)
        settings.selectCustomFont(imported.id, imported.name)
        return imported.name
    }

    /** Capture the tap's local date before waiting for other writes. */
    suspend fun incrementDhikr(id: String): Boolean {
        val date = LocalDate.now()
        _dhikrPending.update { current -> current + (id to ((current[id] ?: 0) + 1)) }
        try {
            return dhikrMutationMutex.withLock {
                val saved = dhikrRepo.increment(id, date)
                if (saved) refreshDate()
                saved
            }
        } finally {
            _dhikrPending.update { current ->
                val remaining = (current[id] ?: 1) - 1
                if (remaining <= 0) current - id else current + (id to remaining)
            }
        }
    }
    suspend fun addDhikr(title: String, target: Int) = dhikrMutationMutex.withLock { dhikrRepo.add(title, target) }
    suspend fun updateDhikr(id: String, title: String, target: Int) = dhikrMutationMutex.withLock { dhikrRepo.update(id, title, target) }
    suspend fun resetDhikrSession(id: String) = dhikrMutationMutex.withLock { dhikrRepo.resetSession(id) }
    suspend fun archiveDhikr(id: String) = dhikrMutationMutex.withLock { dhikrRepo.archive(id) }
    suspend fun restoreDhikr(id: String) = dhikrMutationMutex.withLock { dhikrRepo.restore(id) }
}
