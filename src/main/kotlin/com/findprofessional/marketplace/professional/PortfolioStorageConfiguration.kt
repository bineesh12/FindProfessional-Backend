package com.findprofessional.marketplace.professional

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Path

@Configuration
class PortfolioStorageConfiguration(
    private val properties: PortfolioStorageProperties
) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val publicPath = properties.publicPath.trimEnd('/')
        val directoryUri = Path.of(properties.localDirectory).resolve("portfolio")
            .toAbsolutePath().normalize().toUri().toString()
        val location = if (directoryUri.endsWith('/')) directoryUri else "$directoryUri/"
        registry.addResourceHandler("$publicPath/portfolio/**").addResourceLocations(location)
    }
}
