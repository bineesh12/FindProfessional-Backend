package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.matching.ProfessionalOffer
import com.findprofessional.marketplace.matching.ProfessionalOfferRepository
import com.findprofessional.marketplace.matching.ProfessionalOfferStatus
import com.findprofessional.marketplace.notification.CreateNotification
import com.findprofessional.marketplace.notification.MarketplaceNotificationType
import com.findprofessional.marketplace.notification.NotificationService
import com.findprofessional.marketplace.professional.ProfessionalAuthorizationService
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class RequestCompletionService(
    private val professionalAuthorization: ProfessionalAuthorizationService,
    private val customerAuthorization: CustomerAuthorizationService,
    private val requests: CustomerRequestRepository,
    private val offers: ProfessionalOfferRepository,
    private val notifications: NotificationService,
    private val clock: Clock = Clock.systemUTC()
) {
    @Transactional
    fun markWorkFinished(professionalUserId: UUID, requestId: UUID): RequestCompletionResponse {
        professionalAuthorization.requireProfessional(professionalUserId)
        val request = requests.findByIdForUpdate(requestId).orElseThrow(::requestNotFound)
        val acceptedOffer = requireAcceptedOffer(requestId, professionalUserId)
        if (request.status == CustomerRequestStatus.WORK_FINISHED) return request.toCompletionResponse()
        if (request.status != CustomerRequestStatus.HIRED) {
            throw conflict("Only hired work can be marked finished", "REQUEST_NOT_IN_PROGRESS")
        }

        val now = Instant.now(clock)
        request.status = CustomerRequestStatus.WORK_FINISHED
        request.workFinishedAt = now
        request.updatedAt = now
        notifications.create(
            CreateNotification(
                userId = request.customerId,
                type = MarketplaceNotificationType.WORK_FINISHED,
                title = "Work finished",
                body = "${request.title} is ready for your confirmation",
                requestId = request.id,
                offerId = acceptedOffer.id
            )
        )
        return request.toCompletionResponse()
    }

    @Transactional
    fun complete(customerUserId: UUID, requestId: UUID): RequestCompletionResponse {
        customerAuthorization.requireCustomer(customerUserId)
        val request = requests.findByIdAndCustomerIdForUpdate(requestId, customerUserId)
            .orElseThrow(::requestNotFound)
        val acceptedOffer = acceptedOffer(requestId)
        if (request.status == CustomerRequestStatus.COMPLETED) return request.toCompletionResponse()
        if (request.status != CustomerRequestStatus.WORK_FINISHED) {
            throw conflict("The professional has not marked this work finished", "WORK_NOT_FINISHED")
        }

        val now = Instant.now(clock)
        request.status = CustomerRequestStatus.COMPLETED
        request.completedAt = now
        request.updatedAt = now
        notifications.create(
            CreateNotification(
                userId = acceptedOffer.professionalUserId,
                type = MarketplaceNotificationType.REQUEST_COMPLETED,
                title = "Request completed",
                body = "The customer confirmed completion of ${request.title}",
                requestId = request.id,
                offerId = acceptedOffer.id
            )
        )
        return request.toCompletionResponse()
    }

    private fun requireAcceptedOffer(requestId: UUID, professionalUserId: UUID): ProfessionalOffer {
        val offer = offers.findByProfessionalUserIdAndRequestId(professionalUserId, requestId).orElseThrow {
            RequestException("Accepted offer was not found", "ACCEPTED_OFFER_NOT_FOUND", HttpStatus.FORBIDDEN)
        }
        if (offer.status != ProfessionalOfferStatus.ACCEPTED) {
            throw RequestException(
                "Only the hired professional can finish this work",
                "NOT_HIRED_PROFESSIONAL",
                HttpStatus.FORBIDDEN
            )
        }
        return offer
    }

    private fun acceptedOffer(requestId: UUID): ProfessionalOffer =
        offers.findAllByRequestIdAndStatusOrderByUpdatedAtDesc(requestId, ProfessionalOfferStatus.ACCEPTED)
            .singleOrNull()
            ?: throw conflict("Accepted offer was not found", "ACCEPTED_OFFER_NOT_FOUND")

    private fun CustomerRequest.toCompletionResponse() = RequestCompletionResponse(
        requestId = id,
        status = status,
        workFinishedAt = workFinishedAt,
        completedAt = completedAt
    )

    private fun requestNotFound() =
        RequestException("Request was not found", "REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND)

    private fun conflict(message: String, code: String) =
        RequestException(message, code, HttpStatus.CONFLICT)
}
