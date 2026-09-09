package com.nshd.nurm3.data

/** Saved visual choices. Unknown or corrupt values fall back to a supported option. */
object AppearanceChoices {
    val modes = setOf("system", "light", "dark", "amoled")
    val progressStyles = setOf("slim", "thick", "wavy", "squiggly")
    fun mode(value: String?): String = value?.takeIf { it in modes } ?: "dark"
    fun progress(value: String?): String = value?.takeIf { it in progressStyles } ?: "slim"
}
