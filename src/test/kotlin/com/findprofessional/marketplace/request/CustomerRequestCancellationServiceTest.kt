package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.category.category
import com.findprofessional.marketplace.matching.ProfessionalOfferRepository
import com.findprofessional.marketplace.matching.ProfessionalOfferStatus
import com.findprofessional.marketplace.matching.OpportunityNotificationMatcher
import com.findprofessional.marketplace.notification.CreateNotification
import com.findprofessional.marketplace.notification.MarketplaceNotificationType
import com.findprofessional.marketplace.notification.NotificationService
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class CustomerRequestCancellationServiceTest {
    private val authorization = mock(CustomerAuthorizationService::class.java)
    private val requests = mock(CustomerRequestRepository::class.java)
    private val offers = mock(ProfessionalOfferRepository::class.java)
    private val opportunityMatcher = mock(OpportunityNotificationMatcher::class.java)
    private val notifications = mock(NotificationService::class.java)
    private val service = CustomerRequestCancellationService(
        authorization,
        requests,
        offers,
        opportunityMatcher,
        notifications
    )

    @Test
    fun `cancel marks owned published request as cancelled when it has no offers`() {
        val fixture = fixture()
        `when`(requests.findByIdAndCustomerIdForUpdate(fixture.request.id, fixture.userId))
            .thenReturn(Optional.of(fixture.request))
        `when`(
            offers.countByRequestIdAndStatusIn(
                fixture.request.id,
                setOf(ProfessionalOfferStatus.SUBMITTED, ProfessionalOfferStatus.ACCEPTED)
            )
        ).thenReturn(0)
        val professionalId = UUID.randomUUID()
        `when`(opportunityMatcher.matchingProfessionalIds(fixture.request)).thenReturn(listOf(professionalId))

        service.cancel(fixture.userId, fixture.request.id)

        assertEquals(CustomerRequestStatus.CANCELLED, fixture.request.status)
        verify(authorization).requireCustomer(fixture.userId)
        verify(notifications).create(
            CreateNotification(
                userId = professionalId,
                type = MarketplaceNotificationType.REQUEST_CANCELLED,
                titleKey = "notification.request.cancelled.title",
                bodyKey = "notification.request.cancelled.body.simple",
                bodyArguments = listOf(fixture.request.title),
                requestId = fixture.request.id
            )
        )
    }

    @Test
    fun `cancel rejects request with a submitted offer`() {
        val fixture = fixture()
        `when`(requests.findByIdAndCustomerIdForUpdate(fixture.request.id, fixture.userId))
            .thenReturn(Optional.of(fixture.request))
        `when`(
            offers.countByRequestIdAndStatusIn(
                fixture.request.id,
                setOf(ProfessionalOfferStatus.SUBMITTED, ProfessionalOfferStatus.ACCEPTED)
            )
        ).thenReturn(1)

        val error = assertThrows(RequestException::class.java) {
            service.cancel(fixture.userId, fixture.request.id)
        }

        assertEquals("REQUEST_HAS_OFFERS", error.code)
        assertEquals(CustomerRequestStatus.PUBLISHED, fixture.request.status)
    }

    private fun fixture(): Fixture {
        val userId = UUID.randomUUID()
        val category = category()
        val marketplaceService = MarketplaceService(
            category = category,
            code = "HOME_RENOVATION",
            name = "Home renovation",
            shortDescription = "Renovate a home",
            iconKey = "construction"
        )
        val session = RequestSession(
            customerId = userId,
            category = category,
            service = marketplaceService,
            status = RequestSessionStatus.CONFIRMED
        )
        return Fixture(
            userId,
            CustomerRequest(
                session = session,
                customerId = userId,
                category = category,
                service = marketplaceService,
                title = "Renovate my home",
                description = "Renovate the kitchen and replace the flooring."
            )
        )
    }

    private data class Fixture(val userId: UUID, val request: CustomerRequest)
}
