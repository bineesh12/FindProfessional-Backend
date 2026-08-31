package com.findprofessional.marketplace.service

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

data class NearbyServiceMatch(
    val service: MarketplaceServiceResponse,
    val distanceKm: Double
)

@Repository
class NearbyServiceRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {
    fun findNearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        limit: Int
    ): List<NearbyServiceMatch> = jdbc.query(
        NearbyServicesSql,
        mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "radiusKm" to radiusKm,
            "limit" to limit
        )
    ) { result, _ ->
        NearbyServiceMatch(
            service = MarketplaceServiceResponse(
                id = result.getObject("service_id", UUID::class.java),
                code = result.getString("service_code"),
                name = result.getString("service_name"),
                shortDescription = result.getString("short_description"),
                iconKey = result.getString("icon_key"),
                imageUrl = result.getString("image_url"),
                categoryId = result.getObject("category_id", UUID::class.java),
                categoryName = result.getString("category_name")
            ),
            distanceKm = result.getDouble("distance_km")
        )
    }

    private companion object {
        // Standard JPA has no spherical-distance operator. Keep the PostgreSQL
        // calculation isolated here so callers receive a typed repository result.
        val NearbyServicesSql =
            """
            WITH candidates AS (
                SELECT
                    service.id AS service_id,
                    service.code AS service_code,
                    service.name AS service_name,
                    service.short_description,
                    service.icon_key,
                    service.image_url,
                    category.id AS category_id,
                    category.name AS category_name,
                    area.service_radius_km,
                    area.local_popularity_score,
                    6371.0 * 2.0 * ASIN(LEAST(1.0, SQRT(
                        POWER(SIN(RADIANS(area.latitude - :latitude) / 2.0), 2) +
                        COS(RADIANS(:latitude)) * COS(RADIANS(area.latitude)) *
                        POWER(SIN(RADIANS(area.longitude - :longitude) / 2.0), 2)
                    ))) AS distance_km
                FROM service_availability_areas area
                JOIN marketplace_services service ON service.id = area.service_id
                JOIN service_categories category ON category.id = service.category_id
                WHERE area.active = TRUE
                  AND service.active = TRUE
                  AND category.active = TRUE
            ), ranked AS (
                SELECT *, ROW_NUMBER() OVER (
                    PARTITION BY service_id
                    ORDER BY local_popularity_score DESC, distance_km ASC
                ) AS service_row
                FROM candidates
                WHERE distance_km <= LEAST(service_radius_km, :radiusKm)
            )
            SELECT *
            FROM ranked
            WHERE service_row = 1
            ORDER BY local_popularity_score DESC, distance_km ASC, service_name ASC
            LIMIT :limit
            """.trimIndent()
    }
}
