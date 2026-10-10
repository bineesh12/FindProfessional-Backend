package com.findprofessional.marketplace.professional

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import java.nio.file.Files
import kotlin.io.path.createTempDirectory

class UploadStorageStartupValidatorTest {
    @Test
    fun `creates and verifies writable upload directory`() {
        val root = createTempDirectory("upload-validator")
        val uploads = root.resolve("nested/uploads")

        UploadStorageStartupValidator(
            PortfolioStorageProperties(localDirectory = uploads.toString())
        ).run(DefaultApplicationArguments())

        assertTrue(Files.isDirectory(uploads))
        assertFalse(Files.list(uploads).use { it.findAny().isPresent })
    }

    @Test
    fun `persistent mode rejects directory outside configured mount`() {
        val root = createTempDirectory("upload-validator")
        val mount = root.resolve("volume")
        val uploads = root.resolve("ephemeral/uploads")

        assertThrows(IllegalArgumentException::class.java) {
            UploadStorageStartupValidator(
                PortfolioStorageProperties(
                    localDirectory = uploads.toString(),
                    requirePersistent = true,
                    persistentMountPath = mount.toString()
                )
            ).run(DefaultApplicationArguments())
        }
    }

    @Test
    fun `persistent mode accepts directory inside configured mount`() {
        val root = createTempDirectory("upload-validator")
        val mount = root.resolve("volume")
        val uploads = mount.resolve("uploads")

        UploadStorageStartupValidator(
            PortfolioStorageProperties(
                localDirectory = uploads.toString(),
                requirePersistent = true,
                persistentMountPath = mount.toString()
            )
        ).run(DefaultApplicationArguments())

        assertTrue(Files.isDirectory(uploads))
    }
}
