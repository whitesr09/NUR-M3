package com.nshd.nurm3.focus

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.util.UUID

/** Foreground-only timer; every persisted update is serialized in submission order. */
class FocusController(application: Application) : AndroidViewModel(application) {
    private val dao = FocusDatabase.get(application).dao()
    val sessions = dao.observeSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val routines = dao.observeRoutines().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val pendingWrites = Channel<FocusSession>(Channel.UNLIMITED)
    private val _active = MutableStateFlow<FocusSession?>(null)
    val active: StateFlow<FocusSession?> = _active.asStateFlow()
    private val _remaining = MutableStateFlow(0)
    val remaining: StateFlow<Int> = _remaining.asStateFlow()
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()
    private var ticker: Job? = null
    private var clock: FocusClock? = null

    init {
        viewModelScope.launch {
            // A previous process's running interval is restored as paused, never extrapolated.
            dao.pauseInterrupted()
            dao.recoverableSession()?.let { if (_active.value == null) restore(it) }
            for (session in pendingWrites) dao.saveSession(session)
        }
    }

    private fun now() = SystemClock.elapsedRealtime()
    private fun persist(session: FocusSession) { pendingWrites.trySend(session) }
    private fun stopTicker() { ticker?.cancel(); ticker = null }
    private fun snapshot(status: String? = null): FocusSession? {
        val current = _active.value ?: return null
        val elapsed = clock?.elapsed(now()) ?: current.elapsedSeconds * 1000L
        return current.copy(elapsedSeconds = (elapsed / 1000L).toInt().coerceAtMost(current.plannedSeconds), status = status ?: current.status)
    }
    private fun updateRemaining() {
        val current = _active.value ?: return
        _remaining.value = ((clock?.remaining(now()) ?: 0L) + 999L).div(1000L).toInt().coerceIn(0, current.plannedSeconds)
    }
    private fun runTicker() {
        stopTicker()
        ticker = viewModelScope.launch {
            while (true) {
                val current = _active.value ?: break
                val time = clock ?: break
                if (current.status != "running") break
                if (time.elapsed(now()) >= time.targetMillis) { finish(); break }
                updateRemaining()
                snapshot()?.let(::persist)
                delay(1000L)
            }
        }
    }
    fun start(label: String, minutes: Int, routineId: String? = null) {
        if (!FocusRules.validMinutes(minutes) || label.isBlank() || _active.value?.status in setOf("running", "paused")) return
        stopTicker()
        clock = FocusClock(minutes * 60_000L).start(now())
        val session = FocusSession(UUID.randomUUID().toString(), routineId, label.trim().take(120), minutes * 60, startedAt = System.currentTimeMillis(), status = "running")
        _active.value = session
        _message.value = ""
        updateRemaining()
        persist(session)
        runTicker()
    }
    fun resume() {
        val current = _active.value ?: return
        if (current.status != "paused") return
        clock = (clock ?: FocusClock.recover(current.plannedSeconds * 1000L, current.elapsedSeconds * 1000L)).start(now())
        _active.value = current.copy(status = "running")
        persist(_active.value!!)
        runTicker()
    }
    fun pause() {
        val current = _active.value ?: return
        if (current.status != "running") return
        stopTicker()
        val time = clock?.pause(now()) ?: return
        clock = time
        val paused = current.copy(elapsedSeconds = (time.accruedMillis / 1000L).toInt(), status = "paused")
        _active.value = paused
        updateRemaining()
        persist(paused)
    }
    fun finish() {
        val current = _active.value ?: return
        val time = clock ?: return
        if (current.status != "running" || time.elapsed(now()) < time.targetMillis) return
        stopTicker()
        clock = time.tick(now())
        val completed = current.copy(elapsedSeconds = current.plannedSeconds, status = "completed", finishedAt = System.currentTimeMillis())
        _active.value = completed
        _remaining.value = 0
        persist(completed)
        _message.value = "Focus session completed and saved."
    }
    fun cancel() {
        val current = _active.value ?: return
        if (current.status !in setOf("running", "paused")) return
        stopTicker()
        val cancelled = snapshot("cancelled")?.copy(finishedAt = System.currentTimeMillis()) ?: return
        clock = null
        _active.value = null
        _remaining.value = 0
        persist(cancelled)
    }
    fun restore(session: FocusSession) {
        if (_active.value?.status in setOf("running", "paused") || session.status !in setOf("paused", "running")) return
        stopTicker()
        val paused = session.copy(status = "paused")
        clock = FocusClock.recover(paused.plannedSeconds * 1000L, paused.elapsedSeconds * 1000L)
        _active.value = paused
        updateRemaining()
        persist(paused)
    }
    fun saveRoutine(name: String, work: Int, rest: Int) {
        if (name.isBlank() || !FocusRules.validMinutes(work) || rest !in 1..60) return
        viewModelScope.launch { dao.saveRoutine(FocusRoutine(name = name.trim().take(100), workMinutes = work, restMinutes = rest)) }
    }
    fun deleteRoutine(id: String) { viewModelScope.launch { dao.deleteRoutine(id) } }
    override fun onCleared() { pause(); stopTicker(); pendingWrites.close(); super.onCleared() }
}
