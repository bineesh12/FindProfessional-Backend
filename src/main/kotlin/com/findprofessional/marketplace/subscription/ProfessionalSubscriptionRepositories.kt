package com.findprofessional.marketplace.subscription

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.Optional
import java.util.UUID

interface ProfessionalSubscriptionEntitlementRepository :
    JpaRepository<ProfessionalSubscriptionEntitlement, UUID> {
    fun findFirstByProfessionalUserIdAndRevokedAtIsNullAndStartsAtLessThanEqualAndExpiresAtAfterOrderByExpiresAtDesc(
        professionalUserId: UUID,
        startsAt: Instant,
        expiresAt: Instant
    ): Optional<ProfessionalSubscriptionEntitlement>

    fun findBySourceAndExternalReference(
        source: SubscriptionSource,
        externalReference: String
    ): Optional<ProfessionalSubscriptionEntitlement>
}

interface ProfessionalOpportunityViewRepository : JpaRepository<ProfessionalOpportunityView, UUID> {
    fun existsByProfessionalUserIdAndRequestId(professionalUserId: UUID, requestId: UUID): Boolean

    fun countByProfessionalUserIdAndViewedAtGreaterThanEqualAndViewedAtLessThan(
        professionalUserId: UUID,
        periodStart: Instant,
        periodEnd: Instant
    ): Long
}
