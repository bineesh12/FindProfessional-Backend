package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.professional.PortfolioStorageProperties
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.UUID
import kotlin.io.path.createTempDirectory

class LocalUserProfileImageStorageTest {
    @Test
    fun `save and delete remain inside the profile directory`() {
        val root = createTempDirectory("profile-image-storage-test")
        val storage = LocalUserProfileImageStorage(
            PortfolioStorageProperties(root.toString(), "/uploads")
        )

        val imageUrl = storage.save(UUID.randomUUID(), "png", byteArrayOf(1, 2, 3))
        val stored = root.resolve(imageUrl.removePrefix("/uploads/"))

        assertTrue(Files.exists(stored))
        storage.delete(imageUrl)
        assertFalse(Files.exists(stored))
    }
}
