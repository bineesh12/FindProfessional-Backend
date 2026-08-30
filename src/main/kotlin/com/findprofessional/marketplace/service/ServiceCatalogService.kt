package com.findprofessional.marketplace.service

import com.findprofessional.marketplace.auth.AuthException
import com.findprofessional.marketplace.category.ServiceCategoryRepository
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ServiceCatalogService(
    private val services: MarketplaceServiceRepository,
    private val categories: ServiceCategoryRepository,
    private val nearbyServices: NearbyServiceRepository,
    private val authorization: CustomerAuthorizationService,
    private val properties: CatalogProperties
) {
    @Transactional(readOnly = true)
    fun listNearby(
        userId: UUID,
        latitude: Double,
        longitude: Double,
        radiusKm: Double?
    ): List<NearbyServiceResponse> {
        authorization.requireCustomer(userId)
        val resolvedRadiusKm = radiusKm ?: properties.nearbyDefaultRadiusKm
        validateLocation(latitude, longitude, resolvedRadiusKm)
        return nearbyServices.findNearby(
            latitude,
            longitude,
            resolvedRadiusKm,
            properties.nearbyResultLimit
        )
            .map { match ->
                NearbyServiceResponse(
                    service = match.service,
                    distanceKm = kotlin.math.round(match.distanceKm * 10.0) / 10.0
                )
            }
    }

    @Transactional(readOnly = true)
    fun listByCategory(userId: UUID, categoryId: UUID): List<MarketplaceServiceResponse> {
        authorization.requireCustomer(userId)
        val category = categories.findById(categoryId).orElseThrow {
            AuthException("Service category was not found", "CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        if (!category.active) {
            throw AuthException("Service category was not found", "CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        return services.findAllByCategoryIdAndActiveTrueOrderByNameAsc(categoryId)
            .map(MarketplaceService::toResponse)
    }

    @Transactional(readOnly = true)
    fun search(userId: UUID, query: String): List<MarketplaceServiceResponse> {
        authorization.requireCustomer(userId)
        val normalized = query.trim()
        if (normalized.length !in SearchQueryMinLength..SearchQueryMaxLength) {
            throw AuthException(
                "Search query must contain between $SearchQueryMinLength and $SearchQueryMaxLength characters",
                "INVALID_SEARCH_QUERY",
                HttpStatus.BAD_REQUEST
            )
        }
        return services.search("%${normalized.lowercase()}%").map(MarketplaceService::toResponse)
    }

    private fun validateLocation(latitude: Double, longitude: Double, radiusKm: Double) {
        if (!latitude.isFinite() || latitude !in -90.0..90.0 ||
            !longitude.isFinite() || longitude !in -180.0..180.0 ||
            !radiusKm.isFinite() ||
            radiusKm !in properties.nearbyMinimumRadiusKm..properties.nearbyMaximumRadiusKm
        ) {
            throw AuthException(
                "Location coordinates or radius are invalid",
                "INVALID_LOCATION",
                HttpStatus.BAD_REQUEST
            )
        }
    }

    private companion object {
        const val SearchQueryMinLength = 2
        const val SearchQueryMaxLength = 120
    }
}
