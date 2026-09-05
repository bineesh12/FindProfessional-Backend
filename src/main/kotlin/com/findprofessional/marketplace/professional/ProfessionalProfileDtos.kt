package com.findprofessional.marketplace.professional

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class SaveProfessionalProfileRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 120)
    val businessName: String,

    val primaryServiceId: UUID,

    @field:Size(min = 1, max = 10)
    val serviceIds: List<UUID> = listOf(primaryServiceId),

    @field:NotBlank
    @field:Size(min = 2, max = 120)
    val serviceArea: String,

    @field:NotBlank
    @field:Size(min = 2, max = 20)
    val servicePostalCode: String,

    @field:Min(1)
    @field:Max(500)
    val serviceRadiusKm: Int = 50,

    @field:Min(0)
    @field:Max(80)
    val experienceYears: Int,

    @field:NotBlank
    @field:Email
    @field:Size(max = 254)
    val contactEmail: String,

    @field:NotBlank
    @field:Size(min = 20, max = 500)
    val about: String
)

data class ProfessionalServiceOptionResponse(
    val id: UUID,
    val name: String,
    val categoryName: String
)

data class ProfessionalProfileResponse(
    val businessName: String,
    val primaryService: ProfessionalServiceOptionResponse,
    val offeredServices: List<ProfessionalServiceOptionResponse>,
    val serviceArea: String,
    val servicePostalCode: String? = null,
    val serviceRadiusKm: Double = 50.0,
    val experienceYears: Int,
    val contactEmail: String,
    val about: String
)

data class SavePortfolioProjectRequest(
    val serviceId: UUID,

    @field:NotBlank
    @field:Size(min = 2, max = 120)
    val title: String,

    @field:NotBlank
    @field:Size(min = 10, max = 500)
    val description: String
)

data class ReorderPortfolioProjectsRequest(
    @field:Size(max = 12)
    val projectIds: List<UUID>
)

data class PortfolioImageResponse(
    val id: UUID,
    val url: String,
    val contentType: String,
    val displayOrder: Int
)

data class PortfolioProjectResponse(
    val id: UUID,
    val title: String,
    val description: String,
    val service: ProfessionalServiceOptionResponse,
    val images: List<PortfolioImageResponse>
)

data class ProfessionalProfileSetupResponse(
    val profile: ProfessionalProfileResponse?,
    val suggestedContactEmail: String?,
    val services: List<ProfessionalServiceOptionResponse>,
    val portfolio: List<PortfolioProjectResponse>
)
