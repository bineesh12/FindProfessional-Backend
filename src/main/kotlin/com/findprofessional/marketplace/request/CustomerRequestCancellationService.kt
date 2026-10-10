package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.matching.ProfessionalOfferRepository
import com.findprofessional.marketplace.matching.ProfessionalOfferStatus
import com.findprofessional.marketplace.matching.OpportunityNotificationMatcher
import com.findprofessional.marketplace.notification.CreateNotification
import com.findprofessional.marketplace.notification.MarketplaceNotificationType
import com.findprofessional.marketplace.notification.NotificationService
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class CustomerRequestCancellationService(
    private val authorization: CustomerAuthorizationService,
    private val requests: CustomerRequestRepository,
    private val offers: ProfessionalOfferRepository,
    private val opportunityMatcher: OpportunityNotificationMatcher,
    private val notifications: NotificationService
) {
    @Transactional
    fun cancel(userId: UUID, requestId: UUID) {
        authorization.requireCustomer(userId)
        val request = requests.findByIdAndCustomerIdForUpdate(requestId, userId).orElseThrow {
            RequestException("Request was not found", "REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        if (request.status != CustomerRequestStatus.PUBLISHED) {
            throw RequestException(
                "Only active requests can be deleted",
                "REQUEST_NOT_DELETABLE",
                HttpStatus.CONFLICT
            )
        }
        if (offers.countByRequestIdAndStatusIn(requestId, BlockingOfferStatuses) > 0) {
            throw RequestException(
                "Requests with submitted offers cannot be deleted",
                "REQUEST_HAS_OFFERS",
                HttpStatus.CONFLICT
            )
        }
        request.status = CustomerRequestStatus.CANCELLED
        request.updatedAt = Instant.now()
        opportunityMatcher.matchingProfessionalIds(request).forEach { professionalId ->
            notifications.create(
                CreateNotification(
                    userId = professionalId,
                    type = MarketplaceNotificationType.REQUEST_CANCELLED,
                    titleKey = "notification.request.cancelled.title",
                    bodyKey = "notification.request.cancelled.body.simple",
                    bodyArguments = listOf(request.title),
                    requestId = request.id
                )
            )
        }
    }

    private companion object {
        val BlockingOfferStatuses = setOf(
            ProfessionalOfferStatus.SUBMITTED,
            ProfessionalOfferStatus.ACCEPTED
        )
    }
}
