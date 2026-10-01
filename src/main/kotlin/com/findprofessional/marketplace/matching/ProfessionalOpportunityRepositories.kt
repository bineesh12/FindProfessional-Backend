package com.findprofessional.marketplace.matching

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface ProfessionalOpportunityDeclineRepository : JpaRepository<ProfessionalOpportunityDecline, UUID> {
    fun existsByProfessionalUserIdAndRequestId(professionalUserId: UUID, requestId: UUID): Boolean
}

interface ProfessionalOfferRepository : JpaRepository<ProfessionalOffer, UUID> {
    fun findAllByProfessionalUserIdOrderByUpdatedAtDesc(professionalUserId: UUID): List<ProfessionalOffer>
    fun findAllByRequestIdAndStatusOrderByUpdatedAtDesc(
        requestId: UUID,
        status: ProfessionalOfferStatus
    ): List<ProfessionalOffer>
    fun findByIdAndRequestIdAndStatus(
        id: UUID,
        requestId: UUID,
        status: ProfessionalOfferStatus
    ): Optional<ProfessionalOffer>
    fun countByRequestIdAndStatus(requestId: UUID, status: ProfessionalOfferStatus): Long
    fun findByProfessionalUserIdAndRequestId(professionalUserId: UUID, requestId: UUID): Optional<ProfessionalOffer>
    fun findAllByProfessionalUserIdAndRequestIdIn(
        professionalUserId: UUID,
        requestIds: Collection<UUID>
    ): List<ProfessionalOffer>

    @Query(
        """
        SELECT offer.request.id AS requestId, COUNT(offer.id) AS offerCount
        FROM ProfessionalOffer offer
        WHERE offer.request.id IN :requestIds AND offer.status = :status
        GROUP BY offer.request.id
        """
    )
    fun countByRequestIdsAndStatus(
        @Param("requestIds") requestIds: Collection<UUID>,
        @Param("status") status: ProfessionalOfferStatus
    ): List<RequestOfferCount>
}

interface RequestOfferCount {
    val requestId: UUID
    val offerCount: Long
}

interface ProfessionalOfferAttachmentRepository : JpaRepository<ProfessionalOfferAttachment, UUID> {
    fun findAllByOfferIdOrderByCreatedAtAsc(offerId: UUID): List<ProfessionalOfferAttachment>
    fun countByOfferId(offerId: UUID): Long
    fun findByIdAndOfferProfessionalUserId(id: UUID, professionalUserId: UUID): Optional<ProfessionalOfferAttachment>
}
