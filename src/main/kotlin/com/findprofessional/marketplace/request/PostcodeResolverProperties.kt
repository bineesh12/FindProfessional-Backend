package com.findprofessional.marketplace.request

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.location.postcode-resolver")
data class PostcodeResolverProperties(
    val baseUrl: String = "https://nominatim.openstreetmap.org",
    val userAgent: String = "FindProfessional-Marketplace/0.1",
    val minimumRequestIntervalMillis: Long = 1_000
)
