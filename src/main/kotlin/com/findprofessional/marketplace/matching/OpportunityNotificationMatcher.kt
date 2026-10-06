package com.findprofessional.marketplace.matching

import com.findprofessional.marketplace.professional.ProfessionalProfile
import com.findprofessional.marketplace.professional.ProfessionalProfileRepository
import com.findprofessional.marketplace.professional.ProfessionalServiceOfferingRepository
import com.findprofessional.marketplace.request.CustomerRequest
import com.findprofessional.marketplace.request.RequestLocation
import com.findprofessional.marketplace.request.RequestLocationKind
import com.findprofessional.marketplace.request.RequestLocationRepository
import org.springframework.stereotype.Service
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Service
class OpportunityNotificationMatcher(
    private val offerings: ProfessionalServiceOfferingRepository,
    private val profiles: ProfessionalProfileRepository,
    private val locations: RequestLocationRepository
) {
    fun matchingProfessionalIds(request: CustomerRequest): List<java.util.UUID> {
        val professionalIds = offerings.findAllByServiceIdOrderByDisplayOrderAsc(request.service.id)
            .map { it.professionalUserId }
            .distinct()
            .filterNot { it == request.customerId }
        if (professionalIds.isEmpty()) return emptyList()
        val profileById = profiles.findAllById(professionalIds).associateBy(ProfessionalProfile::userId)
        val location = locations.findAllByRequestIdIn(listOf(request.id)).preferredLocation()
        return professionalIds.filter { professionalId ->
            val profile = profileById[professionalId] ?: return@filter false
            val distance = profile.distanceTo(location)
            distance == null || distance <= profile.serviceRadiusKm
        }
    }

    private fun List<RequestLocation>.preferredLocation(): RequestLocation? =
        firstOrNull { it.kind == RequestLocationKind.SERVICE }
            ?: firstOrNull { it.kind == RequestLocationKind.PROJECT }
            ?: firstOrNull { it.kind == RequestLocationKind.PICKUP }
            ?: firstOrNull()

    private fun ProfessionalProfile?.distanceTo(location: RequestLocation?): Double? {
        val startLatitude = this?.latitude ?: return null
        val startLongitude = longitude ?: return null
        location ?: return null
        val latitudeDelta = Math.toRadians(location.latitude - startLatitude)
        val longitudeDelta = Math.toRadians(location.longitude - startLongitude)
        val startRadians = Math.toRadians(startLatitude)
        val endRadians = Math.toRadians(location.latitude)
        val haversine = sin(latitudeDelta / 2).let { it * it } +
            cos(startRadians) * cos(endRadians) * sin(longitudeDelta / 2).let { it * it }
        return EarthRadiusKm * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
    }

    private companion object {
        const val EarthRadiusKm = 6371.0
    }
}
