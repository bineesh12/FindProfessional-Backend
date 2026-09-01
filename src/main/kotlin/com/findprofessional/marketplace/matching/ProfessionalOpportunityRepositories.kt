package com.findprofessional.marketplace.matching

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface ProfessionalOpportunityDeclineRepository : JpaRepository<ProfessionalOpportunityDecline, UUID> {
    fun existsByProfessionalUserIdAndRequestId(professionalUserId: UUID, requestId: UUID): Boolean
}

interface ProfessionalOfferRepository : JpaRepository<ProfessionalOffer, UUID> {
    fun findByProfessionalUserIdAndRequestId(professionalUserId: UUID, requestId: UUID): Optional<ProfessionalOffer>
    fun findAllByProfessionalUserIdAndRequestIdIn(
        professionalUserId: UUID,
        requestIds: Collection<UUID>
    ): List<ProfessionalOffer>
}

interface ProfessionalOfferAttachmentRepository : JpaRepository<ProfessionalOfferAttachment, UUID> {
    fun findAllByOfferIdOrderByCreatedAtAsc(offerId: UUID): List<ProfessionalOfferAttachment>
    fun countByOfferId(offerId: UUID): Long
    fun findByIdAndOfferProfessionalUserId(id: UUID, professionalUserId: UUID): Optional<ProfessionalOfferAttachment>
}
