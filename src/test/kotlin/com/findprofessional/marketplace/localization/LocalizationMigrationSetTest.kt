package com.findprofessional.marketplace.localization

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

class LocalizationMigrationSetTest {
    @Test
    fun `every non-English locale has a catalog migration and notification bundle`() {
        val resources = Path.of("src/main/resources")
        Locales.forEachIndexed { index, locale ->
            val migration = resources.resolve(
                "db/migration/V${43 + index}__add_${locale}_catalog_translations.sql"
            )
            val messages = resources.resolve("messages_${locale}.properties")
            assertTrue(migration.exists(), "Missing catalog migration for $locale")
            assertTrue(messages.exists(), "Missing notification bundle for $locale")
            val sql = migration.readText()
            assertTrue(sql.contains("service_category_translations"))
            assertTrue(sql.contains("marketplace_service_translations"))
            assertTrue(sql.contains("service_question_translations"))
            assertTrue(sql.contains("question_option_translations"))
        }
    }

    private companion object {
        val Locales = listOf(
            "bg", "hr", "cs", "da", "nl", "et", "fi", "fr", "de", "el", "hu", "ga", "it",
            "lv", "lt", "mt", "pl", "pt", "ro", "sk", "sl", "es", "sv", "ru", "hi"
        )
    }
}
