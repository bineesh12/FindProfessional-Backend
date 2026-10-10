package com.findprofessional.marketplace.localization

object SupportedLocale {
    const val Default = "en"

    val all = setOf(
        "en", "bg", "hr", "cs", "da", "nl", "et", "fi", "fr", "de", "el", "hu", "ga",
        "it", "lv", "lt", "mt", "pl", "pt", "ro", "sk", "sl", "es", "sv", "ru", "hi"
    )

    fun normalize(languageTag: String?): String {
        val candidates = languageTag.orEmpty().split(',')
            .mapIndexedNotNull { index, value ->
                val parts = value.split(';').map(String::trim)
                val tag = parts.firstOrNull().orEmpty()
                if (tag.isBlank()) return@mapIndexedNotNull null
                val quality = parts.drop(1).firstOrNull { it.startsWith("q=", ignoreCase = true) }
                    ?.substringAfter('=')?.toDoubleOrNull() ?: 1.0
                Candidate(tag, quality, index)
            }
            .filter { it.quality > 0.0 }
            .sortedWith(compareByDescending<Candidate> { it.quality }.thenBy { it.index })
        return candidates.firstNotNullOfOrNull { candidate ->
            candidate.tag.substringBefore('-').substringBefore('_').lowercase().takeIf(all::contains)
        } ?: Default
    }

    private data class Candidate(val tag: String, val quality: Double, val index: Int)
}
