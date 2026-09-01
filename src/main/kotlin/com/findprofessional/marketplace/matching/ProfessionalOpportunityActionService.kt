package com.findprofessional.marketplace.matching

import com.findprofessional.marketplace.professional.ProfessionalAuthorizationService
import com.findprofessional.marketplace.professional.ProfessionalServiceOfferingRepository
import com.findprofessional.marketplace.professional.PortfolioStorageProperties
import com.findprofessional.marketplace.request.CustomerRequest
import com.findprofessional.marketplace.request.CustomerRequestRepository
import com.findprofessional.marketplace.request.CustomerRequestStatus
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.util.UUID

@Service
class ProfessionalOpportunityActionService(
    private val authorization: ProfessionalAuthorizationService,
    private val offerings: ProfessionalServiceOfferingRepository,
    private val requests: CustomerRequestRepository,
    private val declines: ProfessionalOpportunityDeclineRepository,
    private val offers: ProfessionalOfferRepository,
    private val attachments: ProfessionalOfferAttachmentRepository,
    private val attachmentStorage: ProfessionalOfferAttachmentStorage,
    private val storageProperties: PortfolioStorageProperties
) {
    @Transactional
    fun decline(userId: UUID, requestId: UUID) {
        val request = requireEligibleRequest(userId, requestId)
        val existingOffer = offers.findByProfessionalUserIdAndRequestId(userId, requestId).orElse(null)
        if (existingOffer?.status == ProfessionalOfferStatus.SUBMITTED) {
            throw ProfessionalOpportunityException(
                "An opportunity with a submitted offer cannot be declined",
                "OFFER_ALREADY_SUBMITTED",
                HttpStatus.CONFLICT
            )
        }
        if (existingOffer?.status == ProfessionalOfferStatus.DRAFT) {
            attachments.findAllByOfferIdOrderByCreatedAtAsc(existingOffer.id).forEach {
                attachmentStorage.delete(it.storageKey)
            }
            offers.delete(existingOffer)
        }
        if (!declines.existsByProfessionalUserIdAndRequestId(userId, requestId)) {
            declines.save(ProfessionalOpportunityDecline(professionalUserId = userId, request = request))
        }
    }

    @Transactional
    fun submitOffer(
        userId: UUID,
        requestId: UUID,
        body: SaveProfessionalOfferRequest
    ): ProfessionalOfferResponse = saveOffer(userId, requestId, body, ProfessionalOfferStatus.SUBMITTED)

    @Transactional
    fun saveDraft(
        userId: UUID,
        requestId: UUID,
        body: SaveProfessionalOfferRequest
    ): ProfessionalOfferResponse = saveOffer(userId, requestId, body, ProfessionalOfferStatus.DRAFT)

    private fun saveOffer(
        userId: UUID,
        requestId: UUID,
        body: SaveProfessionalOfferRequest,
        status: ProfessionalOfferStatus
    ): ProfessionalOfferResponse {
        val request = requireEligibleRequest(userId, requestId)
        val normalized = body.normalized()
        validateOffer(normalized, status)
        if (declines.existsByProfessionalUserIdAndRequestId(userId, requestId)) {
            throw ProfessionalOpportunityException(
                "A declined opportunity cannot receive an offer",
                "OPPORTUNITY_DECLINED",
                HttpStatus.CONFLICT
            )
        }
        val offer = offers.findByProfessionalUserIdAndRequestId(userId, requestId).orElseGet {
            ProfessionalOffer(
                professionalUserId = userId,
                request = request,
                currency = normalized.currency
            )
        }
        if (status == ProfessionalOfferStatus.DRAFT && offer.status == ProfessionalOfferStatus.SUBMITTED) {
            throw ProfessionalOpportunityException(
                "A submitted offer cannot be changed back to draft",
                "OFFER_ALREADY_SUBMITTED",
                HttpStatus.CONFLICT
            )
        }
        offer.amount = normalized.amount?.setScale(2, RoundingMode.HALF_UP)
        offer.currency = normalized.currency
        offer.message = normalized.message
        offer.estimatedDays = normalized.estimatedDays
        offer.availableStartDate = normalized.availableStartDate
        offer.scopeIncluded = normalized.scopeIncluded
        offer.scopeExcluded = normalized.scopeExcluded
        offer.status = status
        val saved = offers.save(offer)
        return saved.toResponse(attachments.findAllByOfferIdOrderByCreatedAtAsc(saved.id), storageProperties.publicPath)
    }

    @Transactional(readOnly = true)
    fun getOffer(userId: UUID, requestId: UUID): ProfessionalOfferResponse {
        authorization.requireProfessional(userId)
        return offers.findByProfessionalUserIdAndRequestId(userId, requestId)
            .orElseThrow {
                ProfessionalOpportunityException("Offer was not found", "OFFER_NOT_FOUND", HttpStatus.NOT_FOUND)
            }
            .let { offer ->
                offer.toResponse(attachments.findAllByOfferIdOrderByCreatedAtAsc(offer.id), storageProperties.publicPath)
            }
    }

    private fun requireEligibleRequest(userId: UUID, requestId: UUID): CustomerRequest {
        authorization.requireProfessional(userId)
        val request = requests.findById(requestId).orElseThrow {
            ProfessionalOpportunityException(
                "Opportunity was not found",
                "OPPORTUNITY_NOT_FOUND",
                HttpStatus.NOT_FOUND
            )
        }
        if (request.status != CustomerRequestStatus.PUBLISHED || request.customerId == userId ||
            !offerings.existsByProfessionalUserIdAndServiceId(userId, request.service.id)
        ) {
            throw ProfessionalOpportunityException(
                "The professional is not eligible for this opportunity",
                "OPPORTUNITY_NOT_AVAILABLE",
                HttpStatus.FORBIDDEN
            )
        }
        return request
    }

    private fun validateOffer(body: SaveProfessionalOfferRequest, status: ProfessionalOfferStatus) {
        val invalidOptionalValues = body.amount?.let { it.signum() <= 0 || it.precision() > 12 || it.scale() > 2 } == true ||
            body.currency.length != 3 || body.message?.length?.let { it > 1000 } == true ||
            body.estimatedDays?.let { it !in 1..3650 } == true ||
            body.scopeIncluded?.length?.let { it > 2000 } == true ||
            body.scopeExcluded?.length?.let { it > 2000 } == true
        val missingSubmissionValues = status == ProfessionalOfferStatus.SUBMITTED && (
            body.amount == null || body.message?.length !in 10..1000 || body.estimatedDays == null ||
                body.availableStartDate == null || body.scopeIncluded?.length !in 10..2000
            )
        if (invalidOptionalValues || missingSubmissionValues) {
            throw ProfessionalOpportunityException(
                "Offer details are invalid",
                "VALIDATION_ERROR",
                HttpStatus.BAD_REQUEST
            )
        }
    }

    private fun SaveProfessionalOfferRequest.normalized() = copy(
        currency = currency.trim().uppercase(),
        message = message?.trim()?.takeIf(String::isNotEmpty),
        scopeIncluded = scopeIncluded?.trim()?.takeIf(String::isNotEmpty),
        scopeExcluded = scopeExcluded?.trim()?.takeIf(String::isNotEmpty)
    )
}

internal fun ProfessionalOffer.toResponse(
    offerAttachments: List<ProfessionalOfferAttachment> = emptyList(),
    publicPath: String = "/uploads"
) = ProfessionalOfferResponse(
    id = id,
    amount = amount,
    currency = currency,
    message = message,
    estimatedDays = estimatedDays,
    availableStartDate = availableStartDate,
    scopeIncluded = scopeIncluded,
    scopeExcluded = scopeExcluded,
    status = status,
    attachments = offerAttachments.map {
        ProfessionalOfferAttachmentResponse(
            id = it.id,
            filename = it.originalFilename,
            contentType = it.contentType,
            sizeBytes = it.sizeBytes,
            url = "${publicPath.trimEnd('/')}/${it.storageKey}"
        )
    },
    createdAt = createdAt,
    updatedAt = updatedAt
)
