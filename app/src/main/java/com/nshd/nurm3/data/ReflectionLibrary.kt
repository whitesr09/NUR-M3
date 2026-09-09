package com.nshd.nurm3.data

import java.time.LocalDate

/** A small curated reading collection. Meanings are original summaries, not translations. */
data class QuranReflection(
    val id: String,
    val reference: String,
    val title: String,
    val arabic: String,
    val meaning: String,
    val sourceUrl: String
)

object ReflectionLibrary {
    val items = listOf(
        QuranReflection("94:5", "Ash-Sharh 94:5", "Ease and difficulty", "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", "Allah reminds us that ease accompanies hardship.", "https://quran.com/94/5"),
        QuranReflection("94:6", "Ash-Sharh 94:6", "A repeated reassurance", "إِنَّ مَعَ الْعُسْرِ يُسْرًا", "The reassurance that ease accompanies hardship is repeated.", "https://quran.com/94/6"),
        QuranReflection("13:28", "Ar-Ra'd 13:28", "Remembering Allah", "الَّذِينَ آمَنُوا وَتَطْمَئِنُّ قُلُوبُهُم بِذِكْرِ اللَّهِ ۗ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ", "Believers find tranquility in remembering Allah; remembrance brings tranquility to hearts.", "https://quran.com/13/28"),
        QuranReflection("2:152", "Al-Baqarah 2:152", "Remember and be grateful", "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ", "Allah calls us to remember Him, be grateful, and not be ungrateful.", "https://quran.com/2/152"),
        QuranReflection("39:53", "Az-Zumar 39:53", "Do not despair", "قُلْ يَا عِبَادِيَ الَّذِينَ أَسْرَفُوا عَلَىٰ أَنفُسِهِمْ لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ ۚ إِنَّ اللَّهَ يَغْفِرُ الذُّنُوبَ جَمِيعًا ۚ إِنَّهُ هُوَ الْغَفُورُ الرَّحِيمُ", "Allah calls those who have wronged themselves not to despair of His mercy and reminds them of His forgiveness.", "https://quran.com/39/53"),
        QuranReflection("65:3", "At-Talaq 65:3", "Trust in Allah", "وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ ۚ وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ ۚ إِنَّ اللَّهَ بَالِغُ أَمْرِهِ ۚ قَدْ جَعَلَ اللَّهُ لِكُلِّ شَيْءٍ قَدْرًا", "Allah provides in unexpected ways and is sufficient for the one who places trust in Him.", "https://quran.com/65/3")
    )

    fun find(id: String): QuranReflection? = items.firstOrNull { it.id == id }
    fun daily(date: LocalDate): QuranReflection = items[Math.floorMod(date.toEpochDay(), items.size.toLong()).toInt()]
    fun search(query: String): List<QuranReflection> {
        val q = query.trim()
        if (q.isEmpty()) return items
        return items.filter { listOf(it.reference, it.title, it.arabic, it.meaning).any { text -> text.contains(q, ignoreCase = true) } }
    }
}
