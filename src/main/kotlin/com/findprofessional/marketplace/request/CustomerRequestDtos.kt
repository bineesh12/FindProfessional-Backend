package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.matching.ProfessionalOfferStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class CustomerRequestFilter { ALL, ACTIVE, COMPLETED }

enum class CustomerOfferSort { LATEST, PRICE_ASC, START_DATE_ASC }

data class CustomerRequestCountsResponse(
    val all: Long,
    val active: Long,
    val completed: Long
)

data class CustomerRequestLocationResponse(
    val municipality: String,
    val postalCode: String
)

data class CustomerRequestBudgetResponse(
    val amount: String,
    val currency: String?
)

data class CustomerRequestListItemResponse(
    val id: UUID,
    val title: String,
    val description: String,
    val categoryName: String,
    val serviceName: String,
    val serviceIconKey: String,
    val status: CustomerRequestStatus,
    val location: CustomerRequestLocationResponse?,
    val budget: CustomerRequestBudgetResponse?,
    val submittedOfferCount: Long,
    val canEdit: Boolean,
    val canDelete: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CustomerRequestListResponse(
    val counts: CustomerRequestCountsResponse,
    val requests: List<CustomerRequestListItemResponse>,
    val page: Int,
    val size: Int,
    val totalCount: Long,
    val totalPages: Int
)

data class UpdateCustomerRequestRequest(
    val title: String,
    val description: String
)

data class CustomerRequestUpdateResponse(
    val id: UUID,
    val title: String,
    val description: String,
    val updatedAt: Instant
)

data class CustomerOfferListItemResponse(
    val id: UUID,
    val professional: CustomerOfferProfessionalResponse,
    val amount: String,
    val currency: String,
    val message: String?,
    val estimatedDays: Int?,
    val availableStartDate: LocalDate?,
    val scopeIncluded: String?,
    val scopeExcluded: String?,
    val status: ProfessionalOfferStatus,
    val submittedAt: Instant
)

data class CustomerOfferProfessionalResponse(
    val id: UUID,
    val businessName: String,
    val serviceArea: String,
    val experienceYears: Int,
    val about: String,
    val portfolioImageUrl: String?
)

data class CustomerOfferRequestResponse(
    val id: UUID,
    val title: String,
    val description: String,
    val serviceName: String,
    val location: CustomerRequestLocationResponse?,
    val budget: CustomerRequestBudgetResponse?,
    val submittedOfferCount: Long
)

data class CustomerOfferAttachmentResponse(
    val id: UUID,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val url: String
)

data class CustomerRequestOffersResponse(
    val request: CustomerOfferRequestResponse,
    val offers: List<CustomerOfferListItemResponse>
)

data class CustomerOfferDetailsResponse(
    val request: CustomerOfferRequestResponse,
    val offer: CustomerOfferListItemResponse,
    val attachments: List<CustomerOfferAttachmentResponse>
)

data class CustomerOfferDecisionResponse(
    val requestId: UUID,
    val offerId: UUID,
    val status: ProfessionalOfferStatus,
    val requestStatus: CustomerRequestStatus
)
