package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.matching.ProfessionalOffer
import com.findprofessional.marketplace.matching.ProfessionalOfferRepository
import com.findprofessional.marketplace.matching.ProfessionalOfferStatus
import com.findprofessional.marketplace.notification.NotificationService
import com.findprofessional.marketplace.notification.CreateNotification
import com.findprofessional.marketplace.notification.MarketplaceNotificationType
import com.findprofessional.marketplace.professional.ProfessionalAuthorizationService
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class RequestCompletionServiceTest {
    private val professionalAuthorization = mock(ProfessionalAuthorizationService::class.java)
    private val customerAuthorization = mock(CustomerAuthorizationService::class.java)
    private val requests = mock(CustomerRequestRepository::class.java)
    private val offers = mock(ProfessionalOfferRepository::class.java)
    private val notifications = mock(NotificationService::class.java)
    private val now = Instant.parse("2026-10-07T16:00:00Z")
    private val service = RequestCompletionService(
        professionalAuthorization,
        customerAuthorization,
        requests,
        offers,
        notifications,
        Clock.fixed(now, ZoneOffset.UTC)
    )

    @Test
    fun `hired professional marks work finished`() {
        val fixture = fixture(CustomerRequestStatus.HIRED)
        `when`(requests.findByIdForUpdate(fixture.request.id)).thenReturn(Optional.of(fixture.request))
        `when`(offers.findByProfessionalUserIdAndRequestId(fixture.professionalId, fixture.request.id))
            .thenReturn(Optional.of(fixture.offer))

        val response = service.markWorkFinished(fixture.professionalId, fixture.request.id)

        assertEquals(CustomerRequestStatus.WORK_FINISHED, response.status)
        assertEquals(now, fixture.request.workFinishedAt)
        assertNotNull(response.workFinishedAt)
        verify(professionalAuthorization).requireProfessional(fixture.professionalId)
        verify(notifications).create(anyNotification())
    }

    @Test
    fun `customer completes work after professional confirmation`() {
        val fixture = fixture(CustomerRequestStatus.WORK_FINISHED)
        `when`(requests.findByIdAndCustomerIdForUpdate(fixture.request.id, fixture.customerId))
            .thenReturn(Optional.of(fixture.request))
        `when`(
            offers.findAllByRequestIdAndStatusOrderByUpdatedAtDesc(
                fixture.request.id,
                ProfessionalOfferStatus.ACCEPTED
            )
        ).thenReturn(listOf(fixture.offer))

        val response = service.complete(fixture.customerId, fixture.request.id)

        assertEquals(CustomerRequestStatus.COMPLETED, response.status)
        assertEquals(now, fixture.request.completedAt)
        verify(customerAuthorization).requireCustomer(fixture.customerId)
        verify(notifications).create(anyNotification())
    }

    @Test
    fun `non hired professional cannot mark work finished`() {
        val fixture = fixture(CustomerRequestStatus.HIRED)
        val otherProfessional = UUID.randomUUID()
        `when`(requests.findByIdForUpdate(fixture.request.id)).thenReturn(Optional.of(fixture.request))
        `when`(offers.findByProfessionalUserIdAndRequestId(otherProfessional, fixture.request.id))
            .thenReturn(Optional.empty())

        val error = assertThrows(RequestException::class.java) {
            service.markWorkFinished(otherProfessional, fixture.request.id)
        }

        assertEquals("ACCEPTED_OFFER_NOT_FOUND", error.code)
        verifyNoInteractions(notifications)
    }

    @Test
    fun `customer cannot complete work before professional finishes`() {
        val fixture = fixture(CustomerRequestStatus.HIRED)
        `when`(requests.findByIdAndCustomerIdForUpdate(fixture.request.id, fixture.customerId))
            .thenReturn(Optional.of(fixture.request))
        `when`(
            offers.findAllByRequestIdAndStatusOrderByUpdatedAtDesc(
                fixture.request.id,
                ProfessionalOfferStatus.ACCEPTED
            )
        ).thenReturn(listOf(fixture.offer))

        val error = assertThrows(RequestException::class.java) {
            service.complete(fixture.customerId, fixture.request.id)
        }

        assertEquals("WORK_NOT_FINISHED", error.code)
        verifyNoInteractions(notifications)
    }

    private fun fixture(status: CustomerRequestStatus): Fixture {
        val customerId = UUID.randomUUID()
        val professionalId = UUID.randomUUID()
        val request = CustomerRequest(
            session = mock(RequestSession::class.java),
            customerId = customerId,
            category = mock(ServiceCategory::class.java),
            service = mock(MarketplaceService::class.java),
            status = status,
            title = "Roof repair",
            description = "Repair and seal the leaking roof."
        )
        val offer = ProfessionalOffer(
            professionalUserId = professionalId,
            request = request,
            amount = BigDecimal("12000"),
            currency = "SEK",
            status = ProfessionalOfferStatus.ACCEPTED
        )
        return Fixture(customerId, professionalId, request, offer)
    }

    private data class Fixture(
        val customerId: UUID,
        val professionalId: UUID,
        val request: CustomerRequest,
        val offer: ProfessionalOffer
    )

    private fun anyNotification(): CreateNotification {
        ArgumentMatchers.any<CreateNotification>()
        return CreateNotification(
            userId = UUID.randomUUID(),
            type = MarketplaceNotificationType.REQUEST_COMPLETED,
            title = "ignored",
            body = "ignored"
        )
    }
}
