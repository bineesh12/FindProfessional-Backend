package com.findprofessional.marketplace.matching

import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.professional.ProfessionalAuthorizationService
import com.findprofessional.marketplace.professional.ProfessionalServiceOfferingRepository
import com.findprofessional.marketplace.professional.PortfolioStorageProperties
import com.findprofessional.marketplace.request.CustomerRequest
import com.findprofessional.marketplace.request.CustomerRequestRepository
import com.findprofessional.marketplace.request.RequestSession
import com.findprofessional.marketplace.request.RequestSessionStatus
import com.findprofessional.marketplace.service.MarketplaceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class ProfessionalOpportunityActionServiceTest {
    private val authorization = mock(ProfessionalAuthorizationService::class.java)
    private val offerings = mock(ProfessionalServiceOfferingRepository::class.java)
    private val requests = mock(CustomerRequestRepository::class.java)
    private val declines = mock(ProfessionalOpportunityDeclineRepository::class.java)
    private val offers = mock(ProfessionalOfferRepository::class.java)
    private val attachments = mock(ProfessionalOfferAttachmentRepository::class.java)
    private val attachmentStorage = mock(ProfessionalOfferAttachmentStorage::class.java)
    private val service = ProfessionalOpportunityActionService(
        authorization,
        offerings,
        requests,
        declines,
        offers,
        attachments,
        attachmentStorage,
        PortfolioStorageProperties()
    )

    @Test
    fun `decline persists an eligible opportunity`() {
        val professionalId = UUID.randomUUID()
        val request = actionCustomerRequest(actionMarketplaceService())
        stubEligible(professionalId, request)
        `when`(offers.findByProfessionalUserIdAndRequestId(professionalId, request.id)).thenReturn(Optional.empty())
        `when`(declines.existsByProfessionalUserIdAndRequestId(professionalId, request.id)).thenReturn(false)

        service.decline(professionalId, request.id)

        val captor = ArgumentCaptor.forClass(ProfessionalOpportunityDecline::class.java)
        verify(declines).save(captor.capture())
        assertEquals(professionalId, captor.value.professionalUserId)
        assertEquals(request.id, captor.value.request.id)
    }

    @Test
    fun `opportunity with an offer cannot be declined`() {
        val professionalId = UUID.randomUUID()
        val request = actionCustomerRequest(actionMarketplaceService())
        val offer = offer(professionalId, request, ProfessionalOfferStatus.SUBMITTED)
        stubEligible(professionalId, request)
        `when`(offers.findByProfessionalUserIdAndRequestId(professionalId, request.id))
            .thenReturn(Optional.of(offer))

        val error = assertThrows(ProfessionalOpportunityException::class.java) {
            service.decline(professionalId, request.id)
        }

        assertEquals("OFFER_ALREADY_SUBMITTED", error.code)
    }

    @Test
    fun `declining an opportunity discards its draft offer`() {
        val professionalId = UUID.randomUUID()
        val request = actionCustomerRequest(actionMarketplaceService())
        val draft = offer(professionalId, request, ProfessionalOfferStatus.DRAFT)
        stubEligible(professionalId, request)
        `when`(offers.findByProfessionalUserIdAndRequestId(professionalId, request.id))
            .thenReturn(Optional.of(draft))
        `when`(attachments.findAllByOfferIdOrderByCreatedAtAsc(draft.id)).thenReturn(emptyList())
        `when`(declines.existsByProfessionalUserIdAndRequestId(professionalId, request.id)).thenReturn(false)

        service.decline(professionalId, request.id)

        verify(offers).delete(draft)
        verify(declines).save(any(ProfessionalOpportunityDecline::class.java))
    }

    @Test
    fun `submit offer normalizes and persists valid details`() {
        val professionalId = UUID.randomUUID()
        val request = actionCustomerRequest(actionMarketplaceService())
        stubEligible(professionalId, request)
        `when`(declines.existsByProfessionalUserIdAndRequestId(professionalId, request.id)).thenReturn(false)
        `when`(offers.findByProfessionalUserIdAndRequestId(professionalId, request.id)).thenReturn(Optional.empty())
        `when`(offers.save(any(ProfessionalOffer::class.java))).thenAnswer { it.arguments[0] }

        val response = service.submitOffer(
            professionalId,
            request.id,
            SaveProfessionalOfferRequest(
                amount = BigDecimal("12500"),
                currency = " sek ",
                message = "  Includes labor and materials.  ",
                estimatedDays = 5,
                availableStartDate = java.time.LocalDate.now().plusDays(1),
                scopeIncluded = "Labor and roofing materials"
            )
        )

        assertEquals(BigDecimal("12500.00"), response.amount)
        assertEquals("SEK", response.currency)
        assertEquals("Includes labor and materials.", response.message)
        assertEquals(5, response.estimatedDays)
        assertEquals(ProfessionalOfferStatus.SUBMITTED, response.status)
    }

    @Test
    fun `draft accepts partial offer details`() {
        val professionalId = UUID.randomUUID()
        val request = actionCustomerRequest(actionMarketplaceService())
        stubEligible(professionalId, request)
        `when`(declines.existsByProfessionalUserIdAndRequestId(professionalId, request.id)).thenReturn(false)
        `when`(offers.findByProfessionalUserIdAndRequestId(professionalId, request.id)).thenReturn(Optional.empty())
        `when`(offers.save(any(ProfessionalOffer::class.java))).thenAnswer { it.arguments[0] }

        val response = service.saveDraft(
            professionalId,
            request.id,
            SaveProfessionalOfferRequest(scopeIncluded = "  Inspect the existing roof  ")
        )

        assertNull(response.amount)
        assertEquals("Inspect the existing roof", response.scopeIncluded)
        assertEquals(ProfessionalOfferStatus.DRAFT, response.status)
    }

    @Test
    fun `submit rejects an incomplete draft`() {
        val professionalId = UUID.randomUUID()
        val request = actionCustomerRequest(actionMarketplaceService())
        stubEligible(professionalId, request)

        val error = assertThrows(ProfessionalOpportunityException::class.java) {
            service.submitOffer(
                professionalId,
                request.id,
                SaveProfessionalOfferRequest(amount = BigDecimal("1200"), currency = "SEK")
            )
        }

        assertEquals("VALIDATION_ERROR", error.code)
    }

    private fun stubEligible(professionalId: UUID, request: com.findprofessional.marketplace.request.CustomerRequest) {
        `when`(requests.findById(request.id)).thenReturn(Optional.of(request))
        `when`(offerings.existsByProfessionalUserIdAndServiceId(professionalId, request.service.id)).thenReturn(true)
    }

    private fun offer(
        professionalId: UUID,
        request: com.findprofessional.marketplace.request.CustomerRequest,
        status: ProfessionalOfferStatus = ProfessionalOfferStatus.DRAFT
    ) =
        ProfessionalOffer(
            professionalUserId = professionalId,
            request = request,
            amount = BigDecimal("1000.00"),
            currency = "SEK",
            message = "A complete offer.",
            estimatedDays = 2,
            status = status
        )
}

private fun actionMarketplaceService(): MarketplaceService {
    val category = ServiceCategory(code = "HOME", name = "Home", iconKey = "home", displayOrder = 1)
    return MarketplaceService(
        category = category,
        code = "ROOFING",
        name = "Roofing",
        shortDescription = "Roof installation and repair",
        iconKey = "roofing"
    )
}

private fun actionCustomerRequest(service: MarketplaceService): CustomerRequest {
    val customerId = UUID.randomUUID()
    return CustomerRequest(
        session = RequestSession(
            customerId = customerId,
            category = service.category,
            service = service,
            status = RequestSessionStatus.CONFIRMED
        ),
        customerId = customerId,
        category = service.category,
        service = service,
        title = "Replace a tiled roof",
        description = "Replace the existing tiled roof on a detached home."
    )
}
