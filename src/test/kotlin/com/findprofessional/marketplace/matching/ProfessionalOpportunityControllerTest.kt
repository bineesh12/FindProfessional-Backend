package com.findprofessional.marketplace.matching

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class ProfessionalOpportunityControllerTest {
    private val service = mock(ProfessionalOpportunityService::class.java)
    private val actions = mock(ProfessionalOpportunityActionService::class.java)
    private val attachments = mock(ProfessionalOfferAttachmentService::class.java)
    private val controller = ProfessionalOpportunityController(service, actions, attachments)

    @Test
    fun `feed uses authenticated professional subject`() {
        val userId = UUID.randomUUID()
        val expected = ProfessionalOpportunitiesResponse(0, emptyList())
        `when`(service.getOpportunities(userId)).thenReturn(expected)

        assertEquals(expected, controller.getOpportunities(opportunityJwt(userId)))
    }

    @Test
    fun `offers feed uses authenticated professional subject`() {
        val userId = UUID.randomUUID()
        val expected = ProfessionalOpportunitiesResponse(0, emptyList())
        `when`(service.getOffers(userId)).thenReturn(expected)

        assertEquals(expected, controller.getOffers(opportunityJwt(userId)))
        verify(service).getOffers(userId)
    }

    @Test
    fun `decline uses authenticated professional subject`() {
        val userId = UUID.randomUUID()
        val requestId = UUID.randomUUID()

        controller.decline(opportunityJwt(userId), requestId)

        verify(actions).decline(userId, requestId)
    }

    @Test
    fun `submit offer forwards authenticated professional and request`() {
        val userId = UUID.randomUUID()
        val requestId = UUID.randomUUID()
        val body = SaveProfessionalOfferRequest(
            amount = BigDecimal("1200.00"),
            currency = "SEK",
            message = "Complete service offer.",
            estimatedDays = 3
        )
        val expected = ProfessionalOfferResponse(
            id = UUID.randomUUID(),
            amount = body.amount,
            currency = body.currency,
            message = body.message,
            estimatedDays = body.estimatedDays,
            availableStartDate = null,
            scopeIncluded = null,
            scopeExcluded = null,
            status = ProfessionalOfferStatus.SUBMITTED,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        `when`(actions.submitOffer(userId, requestId, body)).thenReturn(expected)

        assertEquals(expected, controller.submitOffer(opportunityJwt(userId), requestId, body))
    }

    @Test
    fun `detail uses authenticated professional subject`() {
        val userId = UUID.randomUUID()
        val requestId = UUID.randomUUID()
        val expected = ProfessionalOpportunityResponse(
            id = requestId,
            title = "Roof repair",
            description = "Repair a leaking roof.",
            serviceName = "Roofing",
            categoryName = "Home",
            location = null,
            distanceKm = null,
            publishedAt = Instant.now(),
            matchReasons = listOf(OpportunityMatchReason.SERVICE),
            offer = null,
            customer = OpportunityCustomerResponse("Erik A.", true),
            requirements = emptyList()
        )
        `when`(service.getOpportunity(userId, requestId)).thenReturn(expected)

        assertEquals(expected, controller.getOpportunity(opportunityJwt(userId), requestId))
    }
}

private fun opportunityJwt(userId: UUID) = Jwt.withTokenValue("access-token")
    .header("alg", "HS256")
    .subject(userId.toString())
    .build()
