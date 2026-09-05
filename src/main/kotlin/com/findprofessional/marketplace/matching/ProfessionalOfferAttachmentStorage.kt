package com.findprofessional.marketplace.matching

import com.findprofessional.marketplace.professional.PortfolioStorageProperties
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID

data class StoredOfferAttachment(val storageKey: String)

interface ProfessionalOfferAttachmentStorage {
    fun save(professionalUserId: UUID, offerId: UUID, extension: String, bytes: ByteArray): StoredOfferAttachment
    fun delete(storageKey: String)
}

@Component
class LocalProfessionalOfferAttachmentStorage(
    properties: PortfolioStorageProperties
) : ProfessionalOfferAttachmentStorage {
    private val root = Path.of(properties.localDirectory).toAbsolutePath().normalize()

    override fun save(
        professionalUserId: UUID,
        offerId: UUID,
        extension: String,
        bytes: ByteArray
    ): StoredOfferAttachment {
        val key = "offers/$professionalUserId/$offerId/${UUID.randomUUID()}.$extension"
        val target = root.resolve(key).normalize()
        require(target.startsWith(root)) { "Invalid offer attachment storage key" }
        Files.createDirectories(target.parent)
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW)
        return StoredOfferAttachment(key)
    }

    override fun delete(storageKey: String) {
        val target = root.resolve(storageKey).normalize()
        if (target.startsWith(root)) Files.deleteIfExists(target)
    }
}
