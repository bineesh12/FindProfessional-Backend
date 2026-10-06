package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.matching.ProfessionalOffer
import com.findprofessional.marketplace.matching.ProfessionalOfferRepository
import com.findprofessional.marketplace.matching.ProfessionalOfferStatus
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import com.findprofessional.marketplace.notification.CreateNotification
import com.findprofessional.marketplace.notification.MarketplaceNotificationType
import com.findprofessional.marketplace.notification.NotificationService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class CustomerOfferDecisionService(
    private val authorization: CustomerAuthorizationService,
    private val requests: CustomerRequestRepository,
    private val offers: ProfessionalOfferRepository,
    private val notifications: NotificationService
) {
    @Transactional
    fun accept(userId: UUID, requestId: UUID, offerId: UUID): CustomerOfferDecisionResponse {
        authorization.requireCustomer(userId)
        val request = requireOwnedRequestForUpdate(userId, requestId)

        if (request.status == CustomerRequestStatus.HIRED) {
            val acceptedOffer = offers.findByIdAndRequestIdAndStatusIn(
                offerId,
                requestId,
                setOf(ProfessionalOfferStatus.ACCEPTED)
            )
            if (acceptedOffer.isPresent) return acceptedOffer.get().toDecision(request)
            throw conflict("Another offer has already been accepted", "REQUEST_ALREADY_HIRED")
        }
        if (request.status != CustomerRequestStatus.PUBLISHED) {
            throw conflict("This request cannot accept an offer", "REQUEST_NOT_OPEN")
        }
        val offer = requireOffer(requestId, offerId, setOf(ProfessionalOfferStatus.SUBMITTED))

        val now = Instant.now()
        offers.findAllByRequestIdAndStatusOrderByUpdatedAtDesc(requestId, ProfessionalOfferStatus.SUBMITTED)
            .forEach { submittedOffer ->
                submittedOffer.status = if (submittedOffer.id == offer.id) {
                    ProfessionalOfferStatus.ACCEPTED
                } else {
                    ProfessionalOfferStatus.DECLINED
                }
                submittedOffer.updatedAt = now
                notifications.create(
                    CreateNotification(
                        userId = submittedOffer.professionalUserId,
                        type = if (submittedOffer.id == offer.id) {
                            MarketplaceNotificationType.OFFER_ACCEPTED
                        } else {
                            MarketplaceNotificationType.OFFER_DECLINED
                        },
                        title = if (submittedOffer.id == offer.id) "Offer accepted" else "Request awarded",
                        body = if (submittedOffer.id == offer.id) {
                            "Your offer for ${request.title} was accepted"
                        } else {
                            "The customer selected another offer for ${request.title}"
                        },
                        requestId = request.id,
                        offerId = submittedOffer.id
                    )
                )
            }
        request.status = CustomerRequestStatus.HIRED
        request.updatedAt = now
        return offer.toDecision(request)
    }

    @Transactional
    fun decline(userId: UUID, requestId: UUID, offerId: UUID): CustomerOfferDecisionResponse {
        authorization.requireCustomer(userId)
        val request = requireOwnedRequestForUpdate(userId, requestId)
        if (request.status != CustomerRequestStatus.PUBLISHED) {
            throw conflict("This request cannot decline an offer", "REQUEST_NOT_OPEN")
        }
        val offer = requireOffer(requestId, offerId, setOf(ProfessionalOfferStatus.SUBMITTED))
        offer.status = ProfessionalOfferStatus.DECLINED
        offer.updatedAt = Instant.now()
        notifications.create(
            CreateNotification(
                userId = offer.professionalUserId,
                type = MarketplaceNotificationType.OFFER_DECLINED,
                title = "Offer declined",
                body = "Your offer for ${request.title} was declined",
                requestId = request.id,
                offerId = offer.id
            )
        )
        return offer.toDecision(request)
    }

    private fun requireOwnedRequestForUpdate(userId: UUID, requestId: UUID) =
        requests.findByIdAndCustomerIdForUpdate(requestId, userId).orElseThrow {
            RequestException("Request was not found", "REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND)
        }

    private fun requireOffer(
        requestId: UUID,
        offerId: UUID,
        statuses: Set<ProfessionalOfferStatus>
    ): ProfessionalOffer = offers.findByIdAndRequestIdAndStatusIn(offerId, requestId, statuses).orElseThrow {
        RequestException("Offer was not found", "OFFER_NOT_FOUND", HttpStatus.NOT_FOUND)
    }

    private fun ProfessionalOffer.toDecision(request: CustomerRequest) = CustomerOfferDecisionResponse(
        requestId = request.id,
        offerId = id,
        status = status,
        requestStatus = request.status
    )

    private fun conflict(message: String, code: String) =
        RequestException(message, code, HttpStatus.CONFLICT)
}
