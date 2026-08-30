package com.findprofessional.marketplace.service

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/services")
class ServiceCatalogController(
    private val serviceCatalog: ServiceCatalogService
) {
    @GetMapping("/nearby")
    fun listNearby(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam(required = false) radiusKm: Double?
    ): List<NearbyServiceResponse> = serviceCatalog.listNearby(
        userId = UUID.fromString(jwt.subject),
        latitude = latitude,
        longitude = longitude,
        radiusKm = radiusKm
    )

    @GetMapping("/search")
    fun search(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam query: String
    ): List<MarketplaceServiceResponse> =
        serviceCatalog.search(UUID.fromString(jwt.subject), query)
}
