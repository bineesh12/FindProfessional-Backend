package com.findprofessional.marketplace.request

data class ResolvedPostcode(
    val latitude: Double,
    val longitude: Double,
    val source: String
)

fun interface PostcodeGeocodingClient {
    fun resolve(countryCode: String, postalCode: String): ResolvedPostcode?
}
