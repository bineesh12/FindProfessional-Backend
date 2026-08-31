package com.findprofessional.marketplace.professional

import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID

data class StoredPortfolioImage(val storageKey: String)

interface PortfolioImageStorage {
    fun save(professionalUserId: UUID, projectId: UUID, extension: String, bytes: ByteArray): StoredPortfolioImage
    fun delete(storageKey: String)
}

@Component
class LocalPortfolioImageStorage(
    properties: PortfolioStorageProperties
) : PortfolioImageStorage {
    private val root = Path.of(properties.localDirectory).toAbsolutePath().normalize()

    override fun save(
        professionalUserId: UUID,
        projectId: UUID,
        extension: String,
        bytes: ByteArray
    ): StoredPortfolioImage {
        val key = "portfolio/$professionalUserId/$projectId/${UUID.randomUUID()}.$extension"
        val target = root.resolve(key).normalize()
        require(target.startsWith(root)) { "Invalid portfolio image storage key" }
        Files.createDirectories(target.parent)
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW)
        return StoredPortfolioImage(key)
    }

    override fun delete(storageKey: String) {
        val target = root.resolve(storageKey).normalize()
        if (target.startsWith(root)) Files.deleteIfExists(target)
    }
}
