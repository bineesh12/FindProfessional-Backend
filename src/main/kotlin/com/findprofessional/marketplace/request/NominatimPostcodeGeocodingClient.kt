package com.findprofessional.marketplace.request

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class NominatimPostcodeGeocodingClient(
    builder: RestClient.Builder,
    private val properties: PostcodeResolverProperties
) : PostcodeGeocodingClient {
    private val client = builder.baseUrl(properties.baseUrl).build()
    private val rateLimitMonitor = Any()
    private var nextRequestAtMillis = 0L

    override fun resolve(municipality: String, postalCode: String): ResolvedPostcode? =
        synchronized(rateLimitMonitor) {
            search(municipality, postalCode)
        }

    private fun search(municipality: String, postalCode: String): ResolvedPostcode? {
        val waitMillis = nextRequestAtMillis - System.currentTimeMillis()
        if (waitMillis > 0) Thread.sleep(waitMillis)
        try {
            return client.get()
                .uri { uri ->
                    uri.path("/search")
                        .queryParam("q", "$postalCode $municipality")
                        .queryParam("format", "jsonv2")
                        .queryParam("addressdetails", 1)
                        .queryParam("limit", 1)
                        .build()
                }
                .header("User-Agent", properties.userAgent)
                .retrieve()
                .body(Array<NominatimResult>::class.java)
                ?.firstOrNull()
                ?.toResolvedPostcode(postalCode)
        } finally {
            nextRequestAtMillis = System.currentTimeMillis() + properties.minimumRequestIntervalMillis
        }
    }

    private fun NominatimResult.toResolvedPostcode(expectedPostalCode: String): ResolvedPostcode? {
        val resolvedLatitude = latitude.toDoubleOrNull() ?: return null
        val resolvedLongitude = longitude.toDoubleOrNull() ?: return null
        if (resolvedLatitude !in -90.0..90.0 || resolvedLongitude !in -180.0..180.0) return null
        val returnedPostalCode = address?.postalCode ?: return null
        if (returnedPostalCode.postcodeKey() != expectedPostalCode.postcodeKey()) return null
        val resolvedCountryCode = address?.countryCode?.trim()?.lowercase()
            ?.takeIf { it.length == 2 }
            ?: return null
        return ResolvedPostcode(resolvedCountryCode, resolvedLatitude, resolvedLongitude, "NOMINATIM")
    }

    private data class NominatimResult(
        @JsonProperty("lat") val latitude: String,
        @JsonProperty("lon") val longitude: String,
        val address: NominatimAddress? = null
    )

    private data class NominatimAddress(
        @JsonProperty("country_code") val countryCode: String? = null,
        @JsonProperty("postcode") val postalCode: String? = null
    )

    private fun String.postcodeKey() = filter(Char::isLetterOrDigit).uppercase()
}
