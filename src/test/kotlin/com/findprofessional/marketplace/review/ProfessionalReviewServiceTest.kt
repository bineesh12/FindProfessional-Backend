package com.findprofessional.marketplace.review

import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.matching.ProfessionalOffer
import com.findprofessional.marketplace.matching.ProfessionalOfferRepository
import com.findprofessional.marketplace.matching.ProfessionalOfferStatus
import com.findprofessional.marketplace.notification.NotificationService
import com.findprofessional.marketplace.notification.CreateNotification
import com.findprofessional.marketplace.notification.MarketplaceNotificationType
import com.findprofessional.marketplace.professional.ProfessionalAuthorizationService
import com.findprofessional.marketplace.request.CustomerRequest
import com.findprofessional.marketplace.request.CustomerRequestRepository
import com.findprofessional.marketplace.request.CustomerRequestStatus
import com.findprofessional.marketplace.request.RequestException
import com.findprofessional.marketplace.request.RequestSession
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import com.findprofessional.marketplace.user.UserAccount
import com.findprofessional.marketplace.user.UserAccountRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class ProfessionalReviewServiceTest {
    private val customerAuthorization = mock(CustomerAuthorizationService::class.java)
    private val professionalAuthorization = mock(ProfessionalAuthorizationService::class.java)
    private val requests = mock(CustomerRequestRepository::class.java)
    private val offers = mock(ProfessionalOfferRepository::class.java)
    private val reviews = mock(ProfessionalReviewRepository::class.java)
    private val users = mock(UserAccountRepository::class.java)
    private val notifications = mock(NotificationService::class.java)
    private val now = Instant.parse("2026-10-07T18:00:00Z")
    private val service = ProfessionalReviewService(
        customerAuthorization,
        professionalAuthorization,
        requests,
        offers,
        reviews,
        users,
        notifications,
        Clock.fixed(now, ZoneOffset.UTC)
    )

    @Test
    fun `customer reviews completed work for accepted professional`() {
        val fixture = fixture(CustomerRequestStatus.COMPLETED)
        `when`(requests.findByIdAndCustomerIdForUpdate(fixture.request.id, fixture.customer.id))
            .thenReturn(Optional.of(fixture.request))
        `when`(reviews.existsByRequestId(fixture.request.id)).thenReturn(false)
        `when`(offers.findAllByRequestIdAndStatusOrderByUpdatedAtDesc(
            fixture.request.id,
            ProfessionalOfferStatus.ACCEPTED
        )).thenReturn(listOf(fixture.offer))
        `when`(users.findById(fixture.customer.id)).thenReturn(Optional.of(fixture.customer))
        `when`(reviews.save(any(ProfessionalReview::class.java))).thenAnswer { it.getArgument(0) }

        val result = service.create(
            fixture.customer.id,
            fixture.request.id,
            SaveProfessionalReviewRequest(5, "  Excellent work.  ")
        )

        assertEquals(5, result.rating)
        assertEquals("Excellent work.", result.comment)
        assertEquals(fixture.professionalId, result.professionalId)
        assertEquals(now, result.createdAt)
        verify(notifications).create(anyNotification())
    }

    @Test
    fun `customer cannot review work before completion`() {
        val fixture = fixture(CustomerRequestStatus.HIRED)
        `when`(requests.findByIdAndCustomerIdForUpdate(fixture.request.id, fixture.customer.id))
            .thenReturn(Optional.of(fixture.request))

        val error = assertThrows(RequestException::class.java) {
            service.create(fixture.customer.id, fixture.request.id, SaveProfessionalReviewRequest(5))
        }

        assertEquals("REQUEST_NOT_COMPLETED", error.code)
        verifyNoInteractions(offers, notifications)
    }

    @Test
    fun `completed request accepts only one review`() {
        val fixture = fixture(CustomerRequestStatus.COMPLETED)
        `when`(requests.findByIdAndCustomerIdForUpdate(fixture.request.id, fixture.customer.id))
            .thenReturn(Optional.of(fixture.request))
        `when`(reviews.existsByRequestId(fixture.request.id)).thenReturn(true)

        val error = assertThrows(RequestException::class.java) {
            service.create(fixture.customer.id, fixture.request.id, SaveProfessionalReviewRequest(4))
        }

        assertEquals("REVIEW_ALREADY_EXISTS", error.code)
        verifyNoInteractions(offers, notifications)
    }

    private fun fixture(status: CustomerRequestStatus): Fixture {
        val customer = UserAccount(id = UUID.randomUUID(), displayName = "Anna Customer")
        val professionalId = UUID.randomUUID()
        val marketplaceService = mock(MarketplaceService::class.java)
        `when`(marketplaceService.name).thenReturn("Roofing")
        val request = CustomerRequest(
            session = mock(RequestSession::class.java),
            customerId = customer.id,
            category = mock(ServiceCategory::class.java),
            service = marketplaceService,
            status = status,
            title = "Roof repair",
            description = "Repair the leaking roof."
        )
        val offer = ProfessionalOffer(
            professionalUserId = professionalId,
            request = request,
            currency = "SEK",
            status = ProfessionalOfferStatus.ACCEPTED
        )
        return Fixture(customer, professionalId, request, offer)
    }

    private data class Fixture(
        val customer: UserAccount,
        val professionalId: UUID,
        val request: CustomerRequest,
        val offer: ProfessionalOffer
    )

    private fun anyNotification(): CreateNotification {
        any<CreateNotification>()
        return CreateNotification(
            userId = UUID.randomUUID(),
            type = MarketplaceNotificationType.REVIEW_RECEIVED,
            title = "ignored",
            body = "ignored"
        )
    }
}
