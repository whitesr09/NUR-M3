package com.nshd.nurm3.focus

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** A foreground-only timer. No absent/background time is ever credited. */
class FocusController(application: Application) : AndroidViewModel(application) {
    private val dao = FocusDatabase.get(application).dao()
    val sessions = dao.observeSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val routines = dao.observeRoutines().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val writes = Mutex()
    private val _active = MutableStateFlow<FocusSession?>(null)
    val active: StateFlow<FocusSession?> = _active.asStateFlow()
    private val _remaining = MutableStateFlow(0)
    val remaining: StateFlow<Int> = _remaining.asStateFlow()
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()
    private var ticker: Job? = null
    private var runningSince = 0L
    private var accruedMillis = 0L

    init {
        viewModelScope.launch {
            writes.withLock {
                // Do not reconstruct elapsed time from wall-clock timestamps after process death.
                dao.pauseInterrupted()
                val last = dao.observeSessions().stateIn(this, SharingStarted.Eagerly, emptyList()).value
                // Recovery is offered explicitly through the saved session list.
            }
        }
    }

    private fun elapsedNow(): Long = accruedMillis + if (runningSince != 0L) (SystemClock.elapsedRealtime() - runningSince).coerceAtLeast(0L) else 0L
    private fun snapshot(): FocusSession? = _active.value?.let { it.copy(elapsedSeconds = (elapsedNow() / 1000L).toInt().coerceAtMost(it.plannedSeconds)) }
    private fun persist(session: FocusSession) {
        viewModelScope.launch { writes.withLock { dao.saveSession(session) } }
    }
    private fun stopTicker() { ticker?.cancel(); ticker = null }
    private fun refresh() {
        val current = snapshot() ?: return
        _remaining.value = FocusRules.remaining(current.plannedSeconds, current.elapsedSeconds)
        if (current.elapsedSeconds >= current.plannedSeconds && current.status == "running") finish()
    }
    private fun runTicker() {
        stopTicker()
        ticker = viewModelScope.launch {
            while (true) {
                refresh()
                snapshot()?.let { persist(it) }
                delay(1000L)
            }
        }
    }
    fun start(label: String, minutes: Int, routineId: String? = null) {
        if (!FocusRules.validMinutes(minutes) || label.isBlank() || _active.value?.status == "running") return
        stopTicker()
        accruedMillis = 0L
        runningSince = SystemClock.elapsedRealtime()
        val session = FocusSession(UUID.randomUUID().toString(), routineId, label.trim().take(120), minutes * 60, startedAt = System.currentTimeMillis(), status = "running")
        _active.value = session
        _remaining.value = session.plannedSeconds
        persist(session)
        runTicker()
    }
    fun resume() {
        val current = _active.value ?: return
        if (current.status != "paused") return
        accruedMillis = current.elapsedSeconds * 1000L
        runningSince = SystemClock.elapsedRealtime()
        _active.value = current.copy(status = "running")
        persist(_active.value!!)
        runTicker()
    }
    fun pause() {
        val current = snapshot() ?: return
        if (current.status != "running") return
        stopTicker()
        accruedMillis = elapsedNow()
        runningSince = 0L
        val paused = current.copy(status = "paused")
        _active.value = paused
        _remaining.value = FocusRules.remaining(paused.plannedSeconds, paused.elapsedSeconds)
        persist(paused)
    }
    fun finish() {
        val current = snapshot() ?: return
        if (current.status != "running" || current.elapsedSeconds < current.plannedSeconds) return
        stopTicker()
        accruedMillis = current.plannedSeconds * 1000L
        runningSince = 0L
        val completed = current.copy(elapsedSeconds = current.plannedSeconds, status = "completed", finishedAt = System.currentTimeMillis())
        _active.value = completed
        _remaining.value = 0
        persist(completed)
        _message.value = "Focus session completed and saved."
    }
    fun cancel() {
        val current = snapshot() ?: return
        if (current.status == "completed") return
        stopTicker()
        accruedMillis = 0L
        runningSince = 0L
        val cancelled = current.copy(status = "cancelled", finishedAt = System.currentTimeMillis())
        _active.value = null
        _remaining.value = 0
        persist(cancelled)
    }
    fun restore(session: FocusSession) {
        if (_active.value?.status == "running" || session.status !in setOf("paused", "running")) return
        stopTicker()
        val paused = session.copy(status = "paused")
        accruedMillis = paused.elapsedSeconds * 1000L
        runningSince = 0L
        _active.value = paused
        _remaining.value = FocusRules.remaining(paused.plannedSeconds, paused.elapsedSeconds)
        persist(paused)
    }
    fun saveRoutine(name: String, work: Int, rest: Int) {
        if (name.isBlank() || !FocusRules.validMinutes(work) || rest !in 1..60) return
        viewModelScope.launch { writes.withLock { dao.saveRoutine(FocusRoutine(name = name.trim().take(100), workMinutes = work, restMinutes = rest)) } }
    }
    fun deleteRoutine(id: String) { viewModelScope.launch { writes.withLock { dao.deleteRoutine(id) } } }
    override fun onCleared() { stopTicker(); super.onCleared() }
}
