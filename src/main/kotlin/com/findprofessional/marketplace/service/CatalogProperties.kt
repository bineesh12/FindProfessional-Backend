package com.findprofessional.marketplace.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.catalog")
data class CatalogProperties(
    val nearbyDefaultRadiusKm: Double = 50.0,
    val nearbyMinimumRadiusKm: Double = 1.0,
    val nearbyMaximumRadiusKm: Double = 100.0,
    val nearbyResultLimit: Int = 10
) {
    init {
        require(nearbyMinimumRadiusKm > 0.0) { "Nearby minimum radius must be positive" }
        require(nearbyDefaultRadiusKm in nearbyMinimumRadiusKm..nearbyMaximumRadiusKm) {
            "Nearby default radius must be within the configured range"
        }
        require(nearbyResultLimit > 0) { "Nearby result limit must be positive" }
    }
}
