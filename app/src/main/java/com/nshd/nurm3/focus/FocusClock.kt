package com.nshd.nurm3.focus

/** Pure monotonic timer. No wall-clock or background interval is credited. */
data class FocusClock(
    val targetMillis: Long,
    val accruedMillis: Long = 0L,
    val runningSince: Long? = null,
    val completed: Boolean = false
) {
    init { require(targetMillis in 1_000L..14_400_000L); require(accruedMillis in 0L..targetMillis) }
    fun elapsed(now: Long): Long {
        val delta = runningSince?.let { since -> if (now <= since) 0L else now - since } ?: 0L
        return accruedMillis + delta.coerceIn(0L, targetMillis - accruedMillis)
    }
    fun remaining(now: Long): Long = targetMillis - elapsed(now)
    fun start(now: Long): FocusClock = if (completed || runningSince != null) this else copy(runningSince = now)
    fun pause(now: Long): FocusClock = copy(accruedMillis = elapsed(now), runningSince = null)
    fun tick(now: Long): FocusClock = if (runningSince != null && elapsed(now) >= targetMillis) copy(accruedMillis = targetMillis, runningSince = null, completed = true) else this
    companion object {
        fun recover(targetMillis: Long, savedMillis: Long): FocusClock = FocusClock(targetMillis, savedMillis.coerceIn(0L, targetMillis))
    }
}
