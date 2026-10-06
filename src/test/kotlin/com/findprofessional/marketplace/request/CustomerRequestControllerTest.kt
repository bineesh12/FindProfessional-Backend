package com.findprofessional.marketplace.request

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.UUID

class CustomerRequestControllerTest {
    @Test
    fun `list uses authenticated customer subject`() {
        val service = mock(CustomerRequestQueryService::class.java)
        val controller = controller(service)
        val userId = UUID.randomUUID()
        val jwt = Jwt("token", Instant.now(), Instant.now().plusSeconds(60), mapOf("alg" to "none"), mapOf("sub" to userId.toString()))
        val expected = CustomerRequestListResponse(
            CustomerRequestCountsResponse(0, 0, 0),
            emptyList(),
            0,
            20,
            0,
            0
        )
        `when`(service.list(userId, CustomerRequestFilter.ACTIVE, 0, 20)).thenReturn(expected)

        val response = controller.list(jwt, CustomerRequestFilter.ACTIVE, 0, 20)

        assertSame(expected, response)
        verify(service).list(userId, CustomerRequestFilter.ACTIVE, 0, 20)
    }

    @Test
    fun `offers uses authenticated customer subject and request id`() {
        val service = mock(CustomerRequestQueryService::class.java)
        val controller = controller(service)
        val userId = UUID.randomUUID()
        val requestId = UUID.randomUUID()
        val jwt = Jwt("token", Instant.now(), Instant.now().plusSeconds(60), mapOf("alg" to "none"), mapOf("sub" to userId.toString()))
        val expected = CustomerRequestOffersResponse(
            request = CustomerOfferRequestResponse(
                requestId,
                "Kitchen renovation",
                "Renovate the kitchen",
                "Home renovation",
                null,
                null,
                0
            ),
            offers = emptyList()
        )
        `when`(service.offers(userId, requestId, CustomerOfferSort.PRICE_ASC)).thenReturn(expected)

        val response = controller.offers(jwt, requestId, CustomerOfferSort.PRICE_ASC)

        assertSame(expected, response)
        verify(service).offers(userId, requestId, CustomerOfferSort.PRICE_ASC)
    }

    @Test
    fun `offer details uses authenticated customer subject and both ids`() {
        val service = mock(CustomerRequestQueryService::class.java)
        val controller = controller(service)
        val userId = UUID.randomUUID()
        val requestId = UUID.randomUUID()
        val offerId = UUID.randomUUID()
        val jwt = Jwt("token", Instant.now(), Instant.now().plusSeconds(60), mapOf("alg" to "none"), mapOf("sub" to userId.toString()))
        val request = CustomerOfferRequestResponse(
            requestId,
            "Kitchen renovation",
            "Renovate the kitchen",
            "Home renovation",
            null,
            null,
            1
        )
        val professional = CustomerOfferProfessionalResponse(
            UUID.randomUUID(),
            "Nordic Renovation",
            "Gothenburg",
            8,
            "Local renovation specialists.",
            null
        )
        val offer = CustomerOfferListItemResponse(
            offerId,
            professional,
            "12500",
            "SEK",
            null,
            5,
            null,
            null,
            null,
            com.findprofessional.marketplace.matching.ProfessionalOfferStatus.SUBMITTED,
            Instant.now()
        )
        val expected = CustomerOfferDetailsResponse(request, offer, emptyList())
        `when`(service.offerDetails(userId, requestId, offerId)).thenReturn(expected)

        val response = controller.offerDetails(jwt, requestId, offerId)

        assertSame(expected, response)
        verify(service).offerDetails(userId, requestId, offerId)
    }

    @Test
    fun `update uses authenticated customer subject and request id`() {
        val service = mock(CustomerRequestQueryService::class.java)
        val controller = controller(service)
        val userId = UUID.randomUUID()
        val requestId = UUID.randomUUID()
        val jwt = Jwt("token", Instant.now(), Instant.now().plusSeconds(60), mapOf("alg" to "none"), mapOf("sub" to userId.toString()))
        val input = UpdateCustomerRequestRequest(
            "Updated kitchen request",
            "Replace the cabinets, flooring, and kitchen lighting."
        )
        val expected = CustomerRequestUpdateResponse(
            requestId,
            input.title,
            input.description,
            Instant.now()
        )
        `when`(service.update(userId, requestId, input)).thenReturn(expected)

        val response = controller.update(jwt, requestId, input)

        assertSame(expected, response)
        verify(service).update(userId, requestId, input)
    }

    @Test
    fun `accept offer uses authenticated customer subject and both ids`() {
        val service = mock(CustomerRequestQueryService::class.java)
        val decisions = mock(CustomerOfferDecisionService::class.java)
        val controller = controller(service, decisions)
        val userId = UUID.randomUUID()
        val requestId = UUID.randomUUID()
        val offerId = UUID.randomUUID()
        val jwt = Jwt("token", Instant.now(), Instant.now().plusSeconds(60), mapOf("alg" to "none"), mapOf("sub" to userId.toString()))
        val expected = CustomerOfferDecisionResponse(
            requestId,
            offerId,
            com.findprofessional.marketplace.matching.ProfessionalOfferStatus.ACCEPTED,
            CustomerRequestStatus.HIRED
        )
        `when`(decisions.accept(userId, requestId, offerId)).thenReturn(expected)

        val response = controller.acceptOffer(jwt, requestId, offerId)

        assertSame(expected, response)
        verify(decisions).accept(userId, requestId, offerId)
    }

    @Test
    fun `delete uses authenticated customer subject and request id`() {
        val service = mock(CustomerRequestQueryService::class.java)
        val cancellations = mock(CustomerRequestCancellationService::class.java)
        val controller = controller(service, cancellations = cancellations)
        val userId = UUID.randomUUID()
        val requestId = UUID.randomUUID()
        val jwt = Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            mapOf("alg" to "none"),
            mapOf("sub" to userId.toString())
        )

        controller.delete(jwt, requestId)

        verify(cancellations).cancel(userId, requestId)
    }

    private fun controller(
        service: CustomerRequestQueryService,
        decisions: CustomerOfferDecisionService = mock(CustomerOfferDecisionService::class.java),
        cancellations: CustomerRequestCancellationService = mock(CustomerRequestCancellationService::class.java)
    ) = CustomerRequestController(service, decisions, cancellations)
}
