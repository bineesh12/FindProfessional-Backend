package com.findprofessional.marketplace.professional

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class UploadStorageStartupValidator(
    private val properties: PortfolioStorageProperties
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val uploadDirectory = Path.of(properties.localDirectory).toAbsolutePath().normalize()
        val requiredMount = properties.persistentMountPath
            .takeIf(String::isNotBlank)
            ?.let { Path.of(it).toAbsolutePath().normalize() }

        if (properties.requirePersistent) {
            require(requiredMount != null) {
                "Persistent upload storage requires app.portfolio.storage.persistent-mount-path"
            }
            require(uploadDirectory.startsWith(requiredMount)) {
                "Upload directory must be inside the configured persistent mount"
            }
        }

        Files.createDirectories(uploadDirectory)
        val probe = Files.createTempFile(uploadDirectory, ".arbio-storage-probe-", ".tmp")
        Files.deleteIfExists(probe)
    }
}
