package com.nshd.nurm3.focus

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Application-owned writer: ViewModel recreation never cancels an accepted checkpoint. */
class FocusPersistence private constructor(context: Context) {
    val dao = FocusDatabase.get(context).dao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private data class Write(val session: FocusSession?, val ack: CompletableDeferred<Unit>?)
    private val queue = Channel<Write>(Channel.UNLIMITED)
    private val ready = CompletableDeferred<Unit>()
    init {
        scope.launch {
            try {
                dao.pauseInterrupted()
                ready.complete(Unit)
                for (write in queue) {
                    try {
                        write.session?.let { dao.saveSession(it) }
                        write.ack?.complete(Unit)
                    } catch (error: Exception) { write.ack?.completeExceptionally(error) }
                }
            } catch (error: Exception) {
                if (!ready.isCompleted) ready.completeExceptionally(error)
                throw error
            }
        }
    }
    suspend fun awaitReady() = ready.await()
    fun submit(session: FocusSession) { check(queue.trySend(Write(session, null)).isSuccess) }
    suspend fun flush() {
        val ack = CompletableDeferred<Unit>()
        queue.send(Write(null, ack))
        ack.await()
    }
    suspend fun save(session: FocusSession) {
        val ack = CompletableDeferred<Unit>()
        queue.send(Write(session, ack))
        ack.await()
    }
    companion object {
        @Volatile private var instance: FocusPersistence? = null
        fun get(context: Context): FocusPersistence = instance ?: synchronized(this) {
            instance ?: FocusPersistence(context.applicationContext).also { instance = it }
        }
    }
}
