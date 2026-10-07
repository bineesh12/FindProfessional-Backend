package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.professional.PortfolioStorageProperties
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID

interface UserProfileImageStorage {
    fun save(userId: UUID, extension: String, bytes: ByteArray): String
    fun delete(imageUrl: String)
}

@Component
class LocalUserProfileImageStorage(
    private val properties: PortfolioStorageProperties
) : UserProfileImageStorage {
    private val root = Path.of(properties.localDirectory).toAbsolutePath().normalize()

    override fun save(userId: UUID, extension: String, bytes: ByteArray): String {
        val key = "profiles/$userId/${UUID.randomUUID()}.$extension"
        val target = root.resolve(key).normalize()
        require(target.startsWith(root)) { "Invalid profile image storage key" }
        Files.createDirectories(target.parent)
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW)
        return "${properties.publicPath.trimEnd('/')}/$key"
    }

    override fun delete(imageUrl: String) {
        val key = imageUrl.removePrefix(properties.publicPath.trimEnd('/')).removePrefix("/")
        val target = root.resolve(key).normalize()
        if (target.startsWith(root.resolve("profiles"))) Files.deleteIfExists(target)
    }
}
