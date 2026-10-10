package com.findprofessional.marketplace.matching

import com.findprofessional.marketplace.professional.ProfessionalAuthorizationService
import com.findprofessional.marketplace.professional.ProfessionalProfile
import com.findprofessional.marketplace.professional.ProfessionalProfileRepository
import com.findprofessional.marketplace.professional.ProfessionalServiceOfferingRepository
import com.findprofessional.marketplace.request.CustomerRequest
import com.findprofessional.marketplace.request.CustomerRequestRepository
import com.findprofessional.marketplace.request.CustomerRequestStatus
import com.findprofessional.marketplace.request.RequestLocation
import com.findprofessional.marketplace.request.RequestLocationKind
import com.findprofessional.marketplace.request.RequestLocationRepository
import com.findprofessional.marketplace.request.RequestAnswerRepository
import com.findprofessional.marketplace.user.UserAccountRepository
import com.findprofessional.marketplace.subscription.ProfessionalSubscriptionService
import com.findprofessional.marketplace.localization.CatalogLocalizationService
import com.findprofessional.marketplace.localization.RequestLocaleResolver
import org.springframework.http.HttpStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

@Service
class ProfessionalOpportunityService(
    private val authorization: ProfessionalAuthorizationService,
    private val offerings: ProfessionalServiceOfferingRepository,
    private val requests: CustomerRequestRepository,
    private val locations: RequestLocationRepository,
    private val profiles: ProfessionalProfileRepository,
    private val offers: ProfessionalOfferRepository,
    private val declines: ProfessionalOpportunityDeclineRepository,
    private val answers: RequestAnswerRepository,
    private val users: UserAccountRepository,
    private val subscriptions: ProfessionalSubscriptionService,
    private val localization: CatalogLocalizationService,
    private val localeResolver: RequestLocaleResolver
) {
    @Transactional(readOnly = true)
    fun getOpportunities(userId: UUID): ProfessionalOpportunitiesResponse {
        authorization.requireProfessional(userId)
        val serviceIds = offerings.findAllByProfessionalUserIdOrderByDisplayOrderAsc(userId)
            .map { it.service.id }
            .distinct()
        val subscription = subscriptions.getStatus(userId)
        if (serviceIds.isEmpty()) return ProfessionalOpportunitiesResponse(0, emptyList(), subscription)

        val candidates = requests.findProfessionalOpportunities(
            CustomerRequestStatus.PUBLISHED,
            serviceIds,
            userId,
            PageRequest.of(0, CandidateLimit)
        )
        val locationByRequestId = if (candidates.isEmpty()) {
            emptyMap()
        } else {
            locations.findAllByRequestIdIn(candidates.map { it.id })
                .groupBy { it.request.id }
                .mapValues { (_, requestLocations) -> requestLocations.preferredLocation() }
        }
        val profile = profiles.findById(userId).orElse(null)
        val eligibleCandidates = candidates.mapNotNull { request ->
            val location = locationByRequestId[request.id]
            val distance = profile.distanceTo(location)
            if (profile.hasConfiguredLocation() && location != null &&
                checkNotNull(distance) > profile.serviceRadiusKm
            ) {
                null
            } else {
                OpportunityCandidate(request, location, distance)
            }
        }
        val matches = eligibleCandidates.take(OpportunityLimit)
        val offerByRequestId = if (matches.isEmpty()) emptyMap() else {
            offers.findAllByProfessionalUserIdAndRequestIdIn(
                userId,
                matches.map { it.request.id }
            ).associateBy { it.request.id }
        }
        val locale = localeResolver.current()
        return ProfessionalOpportunitiesResponse(
            totalCount = eligibleCandidates.size.toLong(),
            opportunities = matches.map { candidate ->
                candidate.request.toResponse(
                    candidate.location?.toResponse(),
                    candidate.distanceKm,
                    offerByRequestId[candidate.request.id],
                    locale = locale
                )
            },
            subscription = subscription
        )
    }

    @Transactional(readOnly = true)
    fun getOffers(userId: UUID): ProfessionalOpportunitiesResponse {
        authorization.requireProfessional(userId)
        val professionalOffers = offers.findAllByProfessionalUserIdOrderByUpdatedAtDesc(userId)
        if (professionalOffers.isEmpty()) return ProfessionalOpportunitiesResponse(0, emptyList())
        val locationByRequestId = locations.findAllByRequestIdIn(professionalOffers.map { it.request.id })
            .groupBy { it.request.id }
            .mapValues { (_, requestLocations) -> requestLocations.preferredLocation() }
        val profile = profiles.findById(userId).orElse(null)
        val locale = localeResolver.current()
        return ProfessionalOpportunitiesResponse(
            totalCount = professionalOffers.size.toLong(),
            opportunities = professionalOffers.map { offer ->
                val location = locationByRequestId[offer.request.id]
                offer.request.toResponse(
                    location = location?.toResponse(),
                    distanceKm = profile.distanceTo(location),
                    offer = offer,
                    locale = locale
                )
            }
        )
    }

    @Transactional
    fun getOpportunity(userId: UUID, requestId: UUID): ProfessionalOpportunityResponse {
        authorization.requireProfessional(userId)
        val request = requests.findById(requestId).orElseThrow {
            ProfessionalOpportunityException("Opportunity was not found", "OPPORTUNITY_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        if (request.status != CustomerRequestStatus.PUBLISHED || request.customerId == userId ||
            !offerings.existsByProfessionalUserIdAndServiceId(userId, request.service.id) ||
            declines.existsByProfessionalUserIdAndRequestId(userId, requestId)
        ) {
            throw ProfessionalOpportunityException(
                "The professional is not eligible for this opportunity",
                "OPPORTUNITY_NOT_AVAILABLE",
                HttpStatus.FORBIDDEN
            )
        }
        val requestAnswers = answers.findAllBySessionId(request.session.id).sortedBy { it.question.displayOrder }
        val answerByKey = requestAnswers.associate { it.question.key to it.value }
        val storedLocation = locations.findAllByRequestIdIn(listOf(requestId)).preferredLocation()
        val location = storedLocation?.toResponse() ?: answerByKey.toOpportunityLocation()
        val profile = profiles.findById(userId).orElse(null)
        val distance = profile.distanceTo(storedLocation)
        if (profile.hasConfiguredLocation() && storedLocation != null && checkNotNull(distance) > profile.serviceRadiusKm) {
            throw ProfessionalOpportunityException(
                "The opportunity is outside the professional's working radius",
                "OPPORTUNITY_OUTSIDE_RADIUS",
                HttpStatus.FORBIDDEN
            )
        }
        val currentOffer = offers.findByProfessionalUserIdAndRequestId(userId, requestId).orElse(null)
        if (currentOffer == null) subscriptions.registerOpportunityView(userId, request)
        val customer = users.findById(request.customerId).orElseThrow {
            ProfessionalOpportunityException("Customer was not found", "CUSTOMER_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        val locale = localeResolver.current()
        val requirements = requestAnswers
            .filterNot { it.question.key in LocationQuestionKeys }
            .map { answer ->
                val translated = localization.question(answer.question, locale)
                OpportunityRequirementResponse(
                    key = answer.question.key,
                    label = translated.prompt,
                    value = translated.options.firstOrNull { it.value == answer.value }?.label ?: answer.value,
                    type = answer.question.type
                )
            }
        return request.toResponse(
            location = location,
            distanceKm = distance,
            offer = currentOffer,
            customer = OpportunityCustomerResponse(
                displayName = customer.displayName.trim().ifBlank { "Customer" },
                verified = customer.phoneVerified || customer.googleSubject != null
            ),
            requirements = requirements,
            locale = locale
        )
    }

    private fun List<RequestLocation>.preferredLocation(): RequestLocation? =
        firstOrNull { it.kind == RequestLocationKind.SERVICE } ?: firstOrNull {
            it.kind == RequestLocationKind.PROJECT
        } ?: firstOrNull { it.kind == RequestLocationKind.PICKUP } ?: firstOrNull()

    private fun CustomerRequest.toResponse(
        location: OpportunityLocationResponse?,
        distanceKm: Double?,
        offer: ProfessionalOffer?,
        customer: OpportunityCustomerResponse? = null,
        requirements: List<OpportunityRequirementResponse> = emptyList(),
        locale: String
    ): ProfessionalOpportunityResponse {
        val localizedService = localization.services(listOf(service), locale).single()
        return ProfessionalOpportunityResponse(
        id = id,
        title = title,
        description = description,
        serviceName = localizedService.name,
        categoryName = localizedService.categoryName,
        requestStatus = status,
        location = location,
        distanceKm = distanceKm?.let { round(it * 10.0) / 10.0 },
        publishedAt = createdAt,
        matchReasons = buildList {
            add(OpportunityMatchReason.SERVICE)
            if (distanceKm != null) add(OpportunityMatchReason.LOCATION)
        },
        offer = offer?.toResponse(),
        customer = customer,
        requirements = requirements
        )
    }

    private fun RequestLocation.toResponse() = OpportunityLocationResponse(municipality, postalCode)

    private fun Map<String, String>.toOpportunityLocation(): OpportunityLocationResponse? {
        val pair = LocationAnswerPairs.firstNotNullOfOrNull { (municipalityKey, postcodeKey) ->
            val municipality = this[municipalityKey]?.trim().takeUnless { it.isNullOrBlank() }
            val postcode = this[postcodeKey]?.trim().takeUnless { it.isNullOrBlank() }
            if (municipality != null) municipality to postcode.orEmpty() else null
        } ?: return null
        return OpportunityLocationResponse(pair.first, pair.second)
    }

    private fun ProfessionalProfile?.hasConfiguredLocation() = this?.latitude != null && this.longitude != null

    private fun ProfessionalProfile?.distanceTo(location: RequestLocation?): Double? {
        val startLatitude = this?.latitude ?: return null
        val startLongitude = this.longitude ?: return null
        location ?: return null
        val latitudeDelta = Math.toRadians(location.latitude - startLatitude)
        val longitudeDelta = Math.toRadians(location.longitude - startLongitude)
        val startRadians = Math.toRadians(startLatitude)
        val endRadians = Math.toRadians(location.latitude)
        val haversine = sin(latitudeDelta / 2).let { it * it } +
            cos(startRadians) * cos(endRadians) * sin(longitudeDelta / 2).let { it * it }
        return EarthRadiusKm * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
    }

    private data class OpportunityCandidate(
        val request: CustomerRequest,
        val location: RequestLocation?,
        val distanceKm: Double?
    )

    private companion object {
        const val OpportunityLimit = 20
        const val CandidateLimit = 500
        const val EarthRadiusKm = 6371.0
        val LocationQuestionKeys = setOf(
            "service_location",
            "service_postcode",
            "project_location",
            "project_postcode",
            "moving_from",
            "pickup_postcode",
            "moving_to",
            "destination_postcode"
        )
        val LocationAnswerPairs = listOf(
            "service_location" to "service_postcode",
            "project_location" to "project_postcode",
            "moving_from" to "pickup_postcode",
            "moving_to" to "destination_postcode"
        )
    }
}
