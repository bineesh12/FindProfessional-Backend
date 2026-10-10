package com.findprofessional.marketplace.localization

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class SupportedLocaleTest {
    @Test
    fun `normalizes region tags to supported base language`() {
        assertEquals("sv", SupportedLocale.normalize("sv-SE"))
        assertEquals("pt", SupportedLocale.normalize("pt-BR, en;q=0.8"))
    }

    @Test
    fun `falls back to English for missing and unsupported languages`() {
        assertEquals("en", SupportedLocale.normalize(null))
        assertEquals("en", SupportedLocale.normalize("ja-JP"))
    }

    @Test
    fun `uses the first supported language from an accept language list`() {
        assertEquals("de", SupportedLocale.normalize("ja;q=1, de-DE;q=0.8, en;q=0.5"))
        assertEquals("sv", SupportedLocale.normalize("en;q=0.4, sv-SE;q=0.9"))
    }
}
