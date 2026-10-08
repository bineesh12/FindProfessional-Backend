package com.findprofessional.marketplace.review

import com.findprofessional.marketplace.matching.ProfessionalOffer
import com.findprofessional.marketplace.matching.ProfessionalOfferRepository
import com.findprofessional.marketplace.matching.ProfessionalOfferStatus
import com.findprofessional.marketplace.notification.CreateNotification
import com.findprofessional.marketplace.notification.MarketplaceNotificationType
import com.findprofessional.marketplace.notification.NotificationService
import com.findprofessional.marketplace.professional.ProfessionalAuthorizationService
import com.findprofessional.marketplace.request.CustomerRequest
import com.findprofessional.marketplace.request.CustomerRequestRepository
import com.findprofessional.marketplace.request.CustomerRequestStatus
import com.findprofessional.marketplace.request.RequestException
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import com.findprofessional.marketplace.user.UserAccount
import com.findprofessional.marketplace.user.UserAccountRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class ProfessionalReviewService(
    private val customerAuthorization: CustomerAuthorizationService,
    private val professionalAuthorization: ProfessionalAuthorizationService,
    private val requests: CustomerRequestRepository,
    private val offers: ProfessionalOfferRepository,
    private val reviews: ProfessionalReviewRepository,
    private val users: UserAccountRepository,
    private val notifications: NotificationService,
    private val clock: Clock = Clock.systemUTC()
) {
    @Transactional(readOnly = true)
    fun customerReview(customerUserId: UUID, requestId: UUID): CustomerReviewStateResponse {
        customerAuthorization.requireCustomer(customerUserId)
        requireOwnedRequest(requestId, customerUserId)
        val review = reviews.findByRequestId(requestId).orElse(null)
        return CustomerReviewStateResponse(review?.toResponse(customer(review.customerUserId)))
    }

    @Transactional
    fun create(
        customerUserId: UUID,
        requestId: UUID,
        input: SaveProfessionalReviewRequest
    ): ProfessionalReviewResponse {
        customerAuthorization.requireCustomer(customerUserId)
        val request = requireOwnedCompletedRequestForUpdate(requestId, customerUserId)
        if (reviews.existsByRequestId(requestId)) {
            throw conflict("This completed request already has a review", "REVIEW_ALREADY_EXISTS")
        }
        val offer = acceptedOffer(requestId)
        val now = Instant.now(clock)
        val review = reviews.save(
            ProfessionalReview(
                request = request,
                offer = offer,
                customerUserId = customerUserId,
                professionalUserId = offer.professionalUserId,
                rating = input.rating,
                comment = input.normalizedComment(),
                createdAt = now,
                updatedAt = now
            )
        )
        notifications.create(
            CreateNotification(
                userId = offer.professionalUserId,
                type = MarketplaceNotificationType.REVIEW_RECEIVED,
                title = "New customer review",
                body = "You received a ${input.rating}-star review for ${request.title}",
                requestId = request.id,
                offerId = offer.id
            )
        )
        return review.toResponse(customer(customerUserId))
    }

    @Transactional
    fun update(
        customerUserId: UUID,
        requestId: UUID,
        input: SaveProfessionalReviewRequest
    ): ProfessionalReviewResponse {
        customerAuthorization.requireCustomer(customerUserId)
        requireOwnedCompletedRequestForUpdate(requestId, customerUserId)
        val review = reviews.findByRequestIdAndCustomerUserId(requestId, customerUserId).orElseThrow {
            RequestException("Review was not found", "REVIEW_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        review.rating = input.rating
        review.comment = input.normalizedComment()
        review.updatedAt = Instant.now(clock)
        return review.toResponse(customer(customerUserId))
    }

    @Transactional(readOnly = true)
    fun professionalReviews(
        professionalUserId: UUID,
        page: Int,
        size: Int
    ): ProfessionalReviewsResponse {
        professionalAuthorization.requireProfessional(professionalUserId)
        if (page < 0 || size !in 1..50) {
            throw RequestException("Invalid review page", "INVALID_PAGE")
        }
        val result = reviews.findAllByProfessionalUserIdOrderByCreatedAtDescIdDesc(
            professionalUserId,
            PageRequest.of(page, size)
        )
        val customerIds = result.content.map(ProfessionalReview::customerUserId).toSet()
        val customers = users.findAllById(customerIds).associateBy(UserAccount::id)
        val counts = reviews.ratingBreakdown(professionalUserId).associate { it.rating to it.reviewCount }
        return ProfessionalReviewsResponse(
            summary = ProfessionalRatingSummaryResponse(
                averageRating = reviews.averageRating(professionalUserId),
                reviewCount = reviews.countByProfessionalUserId(professionalUserId),
                breakdown = (5 downTo 1).map { RatingBreakdownResponse(it, counts[it] ?: 0) }
            ),
            reviews = result.content.map { review ->
                review.toResponse(customers[review.customerUserId] ?: customer(review.customerUserId))
            },
            page = result.number,
            size = result.size,
            totalCount = result.totalElements,
            totalPages = result.totalPages
        )
    }

    private fun requireOwnedRequest(requestId: UUID, customerUserId: UUID): CustomerRequest =
        requests.findByIdAndCustomerId(requestId, customerUserId).orElseThrow {
            RequestException("Request was not found", "REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND)
        }

    private fun requireOwnedCompletedRequestForUpdate(requestId: UUID, customerUserId: UUID): CustomerRequest {
        val request = requests.findByIdAndCustomerIdForUpdate(requestId, customerUserId).orElseThrow {
            RequestException("Request was not found", "REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        if (request.status != CustomerRequestStatus.COMPLETED) {
            throw conflict("Only completed work can be reviewed", "REQUEST_NOT_COMPLETED")
        }
        return request
    }

    private fun acceptedOffer(requestId: UUID): ProfessionalOffer =
        offers.findAllByRequestIdAndStatusOrderByUpdatedAtDesc(requestId, ProfessionalOfferStatus.ACCEPTED)
            .singleOrNull()
            ?: throw conflict("Accepted offer was not found", "ACCEPTED_OFFER_NOT_FOUND")

    private fun customer(id: UUID): UserAccount {
        return users.findById(id).orElseThrow {
            RequestException("Customer was not found", "CUSTOMER_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
    }

    private fun ProfessionalReview.toResponse(customer: UserAccount) = ProfessionalReviewResponse(
        id = id,
        requestId = request.id,
        offerId = offer.id,
        professionalId = professionalUserId,
        customer = ReviewCustomerResponse(customer.displayName, customer.profileImageUrl),
        serviceName = request.service.name,
        rating = rating,
        comment = comment,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun SaveProfessionalReviewRequest.normalizedComment() = comment?.trim()?.ifBlank { null }

    private fun conflict(message: String, code: String) =
        RequestException(message, code, HttpStatus.CONFLICT)
}
