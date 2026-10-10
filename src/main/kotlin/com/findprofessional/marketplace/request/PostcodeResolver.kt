package com.findprofessional.marketplace.request

data class ResolvedPostcode(
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val source: String
)

fun interface PostcodeGeocodingClient {
    fun resolve(municipality: String, postalCode: String): ResolvedPostcode?
}
