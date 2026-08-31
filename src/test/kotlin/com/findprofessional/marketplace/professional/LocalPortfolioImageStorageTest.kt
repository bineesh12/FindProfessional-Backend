package com.findprofessional.marketplace.professional

import java.nio.file.Files
import java.util.UUID
import kotlin.io.path.createTempDirectory
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalPortfolioImageStorageTest {
    @Test
    fun `save and delete use configured writable directory`() {
        val root = createTempDirectory("portfolio-storage-test")
        val storage = LocalPortfolioImageStorage(PortfolioStorageProperties(root.toString()))
        val bytes = byteArrayOf(1, 2, 3)

        try {
            val stored = storage.save(UUID.randomUUID(), UUID.randomUUID(), "jpg", bytes)
            val image = root.resolve(stored.storageKey)

            assertTrue(Files.exists(image))
            assertArrayEquals(bytes, Files.readAllBytes(image))

            storage.delete(stored.storageKey)
            assertFalse(Files.exists(image))
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
