package com.findprofessional.marketplace.subscription

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

enum class ProfessionalPlan { FREE, PRO }

data class ProfessionalSubscriptionStatusResponse(
    val enabled: Boolean,
    val plan: ProfessionalPlan,
    val source: SubscriptionSource?,
    val expiresAt: Instant?,
    val freeOpportunityLimit: Int,
    val opportunitiesViewed: Long,
    val opportunitiesRemaining: Long?,
    val periodEndsAt: Instant,
    val canViewNewOpportunity: Boolean
)

data class VerifyGooglePlaySubscriptionRequest(
    @field:NotBlank
    val purchaseToken: String
)

data class VerifyAppStoreSubscriptionRequest(
    @field:NotBlank
    @field:Size(max = 1_000_000)
    val receiptData: String
)

data class GrantPromotionalSubscriptionRequest(
    @field:NotNull
    val professionalUserId: UUID,

    @field:Future
    val expiresAt: Instant,

    @field:Size(max = 500)
    val note: String? = null
)
