package com.findprofessional.marketplace.matching

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import com.findprofessional.marketplace.question.QuestionType

enum class OpportunityMatchReason { SERVICE, LOCATION }

data class ProfessionalOfferResponse(
    val id: UUID,
    val amount: BigDecimal?,
    val currency: String,
    val message: String?,
    val estimatedDays: Int?,
    val availableStartDate: LocalDate?,
    val scopeIncluded: String?,
    val scopeExcluded: String?,
    val status: ProfessionalOfferStatus,
    val attachments: List<ProfessionalOfferAttachmentResponse> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)

data class SaveProfessionalOfferRequest(
    val amount: BigDecimal? = null,
    val currency: String = "SEK",
    val message: String? = null,
    val estimatedDays: Int? = null,
    val availableStartDate: LocalDate? = null,
    val scopeIncluded: String? = null,
    val scopeExcluded: String? = null
)

data class ProfessionalOfferAttachmentResponse(
    val id: UUID,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val url: String
)

data class OpportunityLocationResponse(
    val municipality: String,
    val postalCode: String
)

data class OpportunityCustomerResponse(
    val displayName: String,
    val verified: Boolean
)

data class OpportunityRequirementResponse(
    val key: String,
    val label: String,
    val value: String,
    val type: QuestionType
)

data class ProfessionalOpportunityResponse(
    val id: UUID,
    val title: String,
    val description: String,
    val serviceName: String,
    val categoryName: String,
    val location: OpportunityLocationResponse?,
    val distanceKm: Double?,
    val publishedAt: Instant,
    val matchReasons: List<OpportunityMatchReason>,
    val offer: ProfessionalOfferResponse?,
    val customer: OpportunityCustomerResponse? = null,
    val requirements: List<OpportunityRequirementResponse> = emptyList()
)

data class ProfessionalOpportunitiesResponse(
    val totalCount: Long,
    val opportunities: List<ProfessionalOpportunityResponse>
)
