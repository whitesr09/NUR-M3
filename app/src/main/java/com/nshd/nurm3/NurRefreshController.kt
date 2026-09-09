package com.nshd.nurm3

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Window
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** An app-window refresh-rate request, not a global or privileged display override. */
class NurRefreshController(private val window: Window, context: Context) : DefaultLifecycleObserver {
    private val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val handler = Handler(Looper.getMainLooper())
    private val originalMode = window.attributes.preferredDisplayModeId
    private val originalRate = window.attributes.preferredRefreshRate
    private var requested = false
    private var resumed = false
    private val _status = MutableStateFlow("System controlled")
    val status = _status.asStateFlow()
    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = updateStatus()
        override fun onDisplayRemoved(displayId: Int) = updateStatus()
        override fun onDisplayChanged(displayId: Int) = updateStatus()
    }
    init { manager.registerDisplayListener(listener, handler) }
    fun setEnabled(enabled: Boolean) { requested = enabled; apply() }
    private fun apply() {
        if (!resumed || !requested) { restore(); updateStatus(); return }
        val display = window.decorView.display
        if (display == null) { _status.value = "Display information unavailable"; return }
        val choice = RefreshRatePolicy.choose(display.supportedModes, display.mode)
        if (choice == null) { restore(); _status.value = "No compatible display mode reported"; return }
        val attributes = window.attributes
        attributes.preferredDisplayModeId = choice.modeId
        attributes.preferredRefreshRate = choice.refreshRate
        window.attributes = attributes
        updateStatus()
    }
    private fun restore() {
        val attributes = window.attributes
        attributes.preferredDisplayModeId = originalMode
        attributes.preferredRefreshRate = originalRate
        window.attributes = attributes
    }
    private fun updateStatus() {
        val display = window.decorView.display ?: return
        val current = display.refreshRate
        val choice = RefreshRatePolicy.choose(display.supportedModes, display.mode)
        val available = choice?.refreshRate ?: current
        _status.value = if (requested && resumed) {
            "Requested ${format(available)} Hz · Display reports ${format(current)} Hz"
        } else "System controlled · Display reports ${format(current)} Hz"
    }
    private fun format(value: Float) = String.format(Locale.US, "%.1f", value)
    override fun onResume(owner: LifecycleOwner) { resumed = true; apply() }
    override fun onPause(owner: LifecycleOwner) { resumed = false; restore(); updateStatus() }
    override fun onDestroy(owner: LifecycleOwner) { restore(); manager.unregisterDisplayListener(listener) }
}
