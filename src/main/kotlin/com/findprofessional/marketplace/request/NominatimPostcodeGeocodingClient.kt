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

    override fun resolve(countryCode: String, postalCode: String): ResolvedPostcode? =
        synchronized(rateLimitMonitor) {
            val waitMillis = nextRequestAtMillis - System.currentTimeMillis()
            if (waitMillis > 0) Thread.sleep(waitMillis)
            try {
                client.get()
                    .uri { uri ->
                        uri.path("/search")
                            .queryParam("postalcode", postalCode)
                            .queryParam("countrycodes", countryCode)
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", 1)
                            .build()
                    }
                    .header("User-Agent", properties.userAgent)
                    .retrieve()
                    .body(Array<NominatimResult>::class.java)
                    ?.firstOrNull()
                    ?.toResolvedPostcode()
            } finally {
                nextRequestAtMillis = System.currentTimeMillis() + properties.minimumRequestIntervalMillis
            }
        }

    private fun NominatimResult.toResolvedPostcode(): ResolvedPostcode? {
        val resolvedLatitude = latitude.toDoubleOrNull() ?: return null
        val resolvedLongitude = longitude.toDoubleOrNull() ?: return null
        if (resolvedLatitude !in -90.0..90.0 || resolvedLongitude !in -180.0..180.0) return null
        return ResolvedPostcode(resolvedLatitude, resolvedLongitude, "NOMINATIM")
    }

    private data class NominatimResult(
        @JsonProperty("lat") val latitude: String,
        @JsonProperty("lon") val longitude: String
    )
}
