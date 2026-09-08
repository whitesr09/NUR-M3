package com.nshd.nurm3

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nshd.nurm3.data.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NurViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = NurRepository(NurDatabase.get(application).dao())
    private val settings = NurSettings(application)
    val preferences = settings.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NurPreferences())
    val entries = repo.entries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val completions = repo.completions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    init {
        viewModelScope.launch {
            val dao = NurDatabase.get(application).dao()
            val existing = dao.observeEntries().first()
            if (existing.none { it.kind == NurKind.PRAYER }) {
                listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEachIndexed { index, title ->
                    dao.saveEntry(Entry("prayer-${index + 1}", NurKind.PRAYER, title, index, System.currentTimeMillis()))
                }
            }
        }
    }

    fun refreshDate() { _today.value = LocalDate.now() }
    fun add(kind: String, title: String) = viewModelScope.launch { repo.add(kind, title, entries.value.size) }
    fun complete(id: String, date: LocalDate, checked: Boolean) = viewModelScope.launch { repo.setCompleted(id, date, checked) }
    fun delete(id: String) = viewModelScope.launch { repo.delete(id) }
    fun setting(key: String, value: Boolean) = viewModelScope.launch { settings.update(key, value) }
}
