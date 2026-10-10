package com.findprofessional.marketplace.review

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class SaveProfessionalReviewRequest(
    @field:Min(1)
    @field:Max(5)
    val rating: Int,

    @field:Size(max = 1000)
    val comment: String? = null
)
data class ReviewCustomerResponse(
    val displayName: String,
    val profileImageUrl: String?
)

data class ProfessionalReviewResponse(
    val id: UUID,
    val requestId: UUID,
    val offerId: UUID,
    val professionalId: UUID,
    val customer: ReviewCustomerResponse,
    val serviceName: String,
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CustomerReviewStateResponse(val review: ProfessionalReviewResponse?)

data class RatingBreakdownResponse(val rating: Int, val count: Long)

data class ProfessionalRatingSummaryResponse(
    val averageRating: Double?,
    val reviewCount: Long,
    val breakdown: List<RatingBreakdownResponse>
)

data class ProfessionalReviewsResponse(
    val summary: ProfessionalRatingSummaryResponse,
    val reviews: List<ProfessionalReviewResponse>,
    val page: Int,
    val size: Int,
    val totalCount: Long,
    val totalPages: Int
)
