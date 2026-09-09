package com.nshd.nurm3.focus

/** Pure timer state machine. The caller supplies monotonic milliseconds only while foregrounded. */
data class FocusClock(
    val targetMillis: Long,
    val accruedMillis: Long = 0L,
    val runningSince: Long? = null,
    val completed: Boolean = false
) {
    init { require(targetMillis in 1_000L..14_400_000L); require(accruedMillis in 0L..targetMillis) }
    fun elapsed(now: Long): Long = (accruedMillis + (runningSince?.let { (now - it).coerceAtLeast(0L) } ?: 0L)).coerceAtMost(targetMillis)
    fun remaining(now: Long): Long = targetMillis - elapsed(now)
    fun start(now: Long): FocusClock = if (completed || runningSince != null) this else copy(runningSince = now)
    fun pause(now: Long): FocusClock = copy(accruedMillis = elapsed(now), runningSince = null)
    fun tick(now: Long): FocusClock = if (runningSince != null && elapsed(now) >= targetMillis) copy(accruedMillis = targetMillis, runningSince = null, completed = true) else this
    companion object {
        /** Recovery never credits time since a previous process's last checkpoint. */
        fun recover(targetMillis: Long, savedMillis: Long): FocusClock = FocusClock(targetMillis, savedMillis.coerceIn(0L, targetMillis))
    }
}
