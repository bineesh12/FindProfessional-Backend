package com.findprofessional.marketplace.request

import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
import org.slf4j.LoggerFactory

@Service
class RequestLocationService(
    private val postcodeCoordinates: PostcodeCoordinateRepository,
    private val requestLocations: RequestLocationRepository,
    private val geocodingClient: PostcodeGeocodingClient,
    private val properties: PostcodeResolverProperties
) {
    fun saveResolvedLocations(request: CustomerRequest, answers: Map<String, String>) {
        locationDefinitions.mapNotNull { definition ->
            val municipality = answers[definition.municipalityKey]?.trim() ?: return@mapNotNull null
            val postalCode = answers[definition.postcodeKey]?.trim()
                ?: throw RequestException("A postcode is required for the request location", "POSTCODE_REQUIRED")
            val coordinates = resolve(postalCode)
            RequestLocation(
                request = request,
                kind = definition.kind,
                municipality = municipality,
                postalCode = postalCode,
                latitude = coordinates.latitude,
                longitude = coordinates.longitude
            )
        }.takeIf { it.isNotEmpty() }?.let(requestLocations::saveAll)
    }

    private fun resolve(postalCode: String): PostcodeCoordinate {
        val countryCode = properties.countryCode.lowercase()
        return postcodeCoordinates.findByCountryCodeAndPostalCode(countryCode, postalCode).orElseGet {
            val resolved = try {
                geocodingClient.resolve(countryCode, postalCode)
            } catch (exception: Exception) {
                logger.warn("Postcode resolver failed for country={}", countryCode, exception)
                throw RequestException(
                    "Location lookup is temporarily unavailable. Please try again.",
                    "LOCATION_RESOLVER_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE
                )
            } ?: throw RequestException(
                "The postcode could not be located. Check it and try again.",
                "POSTCODE_NOT_FOUND"
            )
            postcodeCoordinates.save(
                PostcodeCoordinate(
                    countryCode = countryCode,
                    postalCode = postalCode,
                    latitude = resolved.latitude,
                    longitude = resolved.longitude,
                    source = resolved.source
                )
            )
        }
    }

    private data class LocationDefinition(
        val kind: RequestLocationKind,
        val municipalityKey: String,
        val postcodeKey: String
    )

    private companion object {
        val logger = LoggerFactory.getLogger(RequestLocationService::class.java)
        val locationDefinitions = listOf(
            LocationDefinition(RequestLocationKind.SERVICE, "service_location", "service_postcode"),
            LocationDefinition(RequestLocationKind.PROJECT, "project_location", "project_postcode"),
            LocationDefinition(RequestLocationKind.PICKUP, "moving_from", "pickup_postcode"),
            LocationDefinition(RequestLocationKind.DESTINATION, "moving_to", "destination_postcode")
        )
    }
}
