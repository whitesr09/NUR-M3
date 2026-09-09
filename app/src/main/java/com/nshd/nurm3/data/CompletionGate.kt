package com.nshd.nurm3.data

import java.time.LocalDate

/** Prevents overlapping writes for the same checkbox without changing stored truth. */
class CompletionGate {
    private val active = mutableSetOf<String>()

    companion object {
        fun key(id: String, date: LocalDate): String = "$id|$date"
    }

    @Synchronized
    fun acquire(key: String): Boolean = active.add(key)

    @Synchronized
    fun release(key: String) { active.remove(key) }

    @Synchronized
    fun contains(key: String): Boolean = key in active
}
