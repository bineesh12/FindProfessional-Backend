package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.matching.ProfessionalOffer
import com.findprofessional.marketplace.matching.ProfessionalOfferRepository
import com.findprofessional.marketplace.matching.ProfessionalOfferStatus
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import com.findprofessional.marketplace.notification.NotificationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class CustomerOfferDecisionServiceTest {
    private val authorization = mock(CustomerAuthorizationService::class.java)
    private val requests = mock(CustomerRequestRepository::class.java)
    private val offers = mock(ProfessionalOfferRepository::class.java)
    private val notifications = mock(NotificationService::class.java)
    private val service = CustomerOfferDecisionService(authorization, requests, offers, notifications)

    @Test
    fun `accept marks selected offer accepted and remaining submitted offers declined`() {
        val fixture = fixture()
        val selected = offer(fixture.request)
        val competing = offer(fixture.request)
        `when`(requests.findByIdAndCustomerIdForUpdate(fixture.request.id, fixture.userId))
            .thenReturn(Optional.of(fixture.request))
        `when`(
            offers.findByIdAndRequestIdAndStatusIn(
                selected.id,
                fixture.request.id,
                setOf(ProfessionalOfferStatus.SUBMITTED)
            )
        ).thenReturn(Optional.of(selected))
        `when`(
            offers.findAllByRequestIdAndStatusOrderByUpdatedAtDesc(
                fixture.request.id,
                ProfessionalOfferStatus.SUBMITTED
            )
        ).thenReturn(listOf(selected, competing))

        val response = service.accept(fixture.userId, fixture.request.id, selected.id)

        assertEquals(ProfessionalOfferStatus.ACCEPTED, selected.status)
        assertEquals(ProfessionalOfferStatus.DECLINED, competing.status)
        assertEquals(CustomerRequestStatus.HIRED, fixture.request.status)
        assertEquals(ProfessionalOfferStatus.ACCEPTED, response.status)
        verify(authorization).requireCustomer(fixture.userId)
    }

    @Test
    fun `decline changes only the selected submitted offer`() {
        val fixture = fixture()
        val selected = offer(fixture.request)
        `when`(requests.findByIdAndCustomerIdForUpdate(fixture.request.id, fixture.userId))
            .thenReturn(Optional.of(fixture.request))
        `when`(
            offers.findByIdAndRequestIdAndStatusIn(
                selected.id,
                fixture.request.id,
                setOf(ProfessionalOfferStatus.SUBMITTED)
            )
        ).thenReturn(Optional.of(selected))

        val response = service.decline(fixture.userId, fixture.request.id, selected.id)

        assertEquals(ProfessionalOfferStatus.DECLINED, selected.status)
        assertEquals(CustomerRequestStatus.PUBLISHED, fixture.request.status)
        assertEquals(ProfessionalOfferStatus.DECLINED, response.status)
    }

    @Test
    fun `accept rejects a different offer after request was hired`() {
        val fixture = fixture(status = CustomerRequestStatus.HIRED)
        val selected = offer(fixture.request)
        `when`(requests.findByIdAndCustomerIdForUpdate(fixture.request.id, fixture.userId))
            .thenReturn(Optional.of(fixture.request))
        val error = assertThrows(RequestException::class.java) {
            service.accept(fixture.userId, fixture.request.id, selected.id)
        }

        assertEquals("REQUEST_ALREADY_HIRED", error.code)
    }

    private fun fixture(status: CustomerRequestStatus = CustomerRequestStatus.PUBLISHED): Fixture {
        val userId = UUID.randomUUID()
        val category = mock(ServiceCategory::class.java)
        val marketplaceService = mock(MarketplaceService::class.java)
        val session = mock(RequestSession::class.java)
        return Fixture(
            userId,
            CustomerRequest(
                session = session,
                customerId = userId,
                category = category,
                service = marketplaceService,
                status = status,
                title = "Kitchen renovation",
                description = "Replace kitchen cabinets and flooring."
            )
        )
    }

    private fun offer(request: CustomerRequest) = ProfessionalOffer(
        professionalUserId = UUID.randomUUID(),
        request = request,
        amount = BigDecimal("12500"),
        currency = "SEK",
        status = ProfessionalOfferStatus.SUBMITTED
    )

    private data class Fixture(val userId: UUID, val request: CustomerRequest)
}
