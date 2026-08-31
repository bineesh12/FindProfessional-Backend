package com.findprofessional.marketplace.professional

import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.service.MarketplaceServiceRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProfessionalProfileService(
    private val profiles: ProfessionalProfileRepository,
    private val services: MarketplaceServiceRepository,
    private val offerings: ProfessionalServiceOfferingRepository,
    private val portfolio: ProfessionalPortfolioService,
    private val authorization: ProfessionalAuthorizationService
) {
    @Transactional(readOnly = true)
    fun setup(userId: UUID): ProfessionalProfileSetupResponse {
        val user = authorization.requireProfessional(userId)
        return ProfessionalProfileSetupResponse(
            profile = profiles.findById(userId).map { profile ->
                val selected = offerings.findAllByProfessionalUserIdOrderByDisplayOrderAsc(userId)
                    .map(ProfessionalServiceOffering::service)
                    .ifEmpty { listOf(profile.primaryService) }
                profile.toResponse(selected)
            }.orElse(null),
            suggestedContactEmail = user.email,
            services = services.findAllActiveForProfessionalSetup().map(MarketplaceService::toOptionResponse),
            portfolio = portfolio.loadProjects(userId)
        )
    }

    @Transactional
    fun save(userId: UUID, request: SaveProfessionalProfileRequest): ProfessionalProfileResponse {
        authorization.requireProfessional(userId)
        val businessName = request.businessName.trim()
        val serviceArea = request.serviceArea.trim()
        val contactEmail = request.contactEmail.trim().lowercase()
        val about = request.about.trim()
        validateNormalized(businessName, serviceArea, request.experienceYears, contactEmail, about)
        val requestedServiceIds = request.serviceIds.distinct()
        if (requestedServiceIds.isEmpty() || requestedServiceIds.size > 10 || request.primaryServiceId !in requestedServiceIds) {
            throw ProfessionalProfileException(
                "Primary service must be included in the offered services",
                "VALIDATION_ERROR",
                HttpStatus.BAD_REQUEST
            )
        }
        val selectedServices = services.findAllById(requestedServiceIds)
            .filter(MarketplaceService::active)
            .associateBy(MarketplaceService::id)
        if (selectedServices.size != requestedServiceIds.size) {
            throw ProfessionalProfileException(
                "One or more services were not found",
                "SERVICE_NOT_FOUND",
                HttpStatus.NOT_FOUND
            )
        }
        val primaryService = checkNotNull(selectedServices[request.primaryServiceId])
        val profile = profiles.findById(userId).orElseGet {
            ProfessionalProfile(
                userId = userId,
                businessName = businessName,
                primaryService = primaryService,
                serviceArea = serviceArea,
                experienceYears = request.experienceYears,
                contactEmail = contactEmail,
                about = about
            )
        }
        profile.businessName = businessName
        profile.primaryService = primaryService
        profile.serviceArea = serviceArea
        profile.experienceYears = request.experienceYears
        profile.contactEmail = contactEmail
        profile.about = about
        val saved = profiles.save(profile)
        offerings.deleteAllByProfessionalUserId(userId)
        offerings.flush()
        val orderedIds = listOf(request.primaryServiceId) + requestedServiceIds.filterNot { it == request.primaryServiceId }
        offerings.saveAll(
            orderedIds.mapIndexed { index, serviceId ->
                ProfessionalServiceOffering(
                    professionalUserId = userId,
                    service = checkNotNull(selectedServices[serviceId]),
                    primary = serviceId == request.primaryServiceId,
                    displayOrder = index
                )
            }
        )
        return saved.toResponse(orderedIds.map { checkNotNull(selectedServices[it]) })
    }

    private fun validateNormalized(
        businessName: String,
        serviceArea: String,
        experienceYears: Int,
        contactEmail: String,
        about: String
    ) {
        if (businessName.length !in 2..120 ||
            serviceArea.length !in 2..120 ||
            experienceYears !in 0..80 ||
            contactEmail.length !in 3..254 || !EmailPattern.matches(contactEmail) ||
            about.length !in 20..500
        ) {
            throw ProfessionalProfileException(
                "Professional profile details are invalid",
                "VALIDATION_ERROR",
                HttpStatus.BAD_REQUEST
            )
        }
    }

    private companion object {
        val EmailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}

private fun ProfessionalProfile.toResponse(
    offeredServices: List<MarketplaceService> = listOf(primaryService)
) = ProfessionalProfileResponse(
    businessName = businessName,
    primaryService = primaryService.toOptionResponse(),
    offeredServices = offeredServices.map(MarketplaceService::toOptionResponse),
    serviceArea = serviceArea,
    experienceYears = experienceYears,
    contactEmail = contactEmail,
    about = about
)

private fun MarketplaceService.toOptionResponse() = ProfessionalServiceOptionResponse(
    id = id,
    name = name,
    categoryName = category.name
)
