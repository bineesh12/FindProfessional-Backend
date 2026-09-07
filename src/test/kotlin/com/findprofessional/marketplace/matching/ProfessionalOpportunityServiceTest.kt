package com.findprofessional.marketplace.matching

import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.professional.ProfessionalAuthorizationService
import com.findprofessional.marketplace.professional.ProfessionalServiceOffering
import com.findprofessional.marketplace.professional.ProfessionalServiceOfferingRepository
import com.findprofessional.marketplace.professional.ProfessionalProfileRepository
import com.findprofessional.marketplace.professional.ProfessionalProfile
import com.findprofessional.marketplace.question.QuestionType
import com.findprofessional.marketplace.question.ServiceQuestion
import com.findprofessional.marketplace.request.RequestAnswer
import com.findprofessional.marketplace.request.CustomerRequest
import com.findprofessional.marketplace.request.CustomerRequestRepository
import com.findprofessional.marketplace.request.CustomerRequestStatus
import com.findprofessional.marketplace.request.RequestLocation
import com.findprofessional.marketplace.request.RequestLocationKind
import com.findprofessional.marketplace.request.RequestLocationRepository
import com.findprofessional.marketplace.request.RequestSession
import com.findprofessional.marketplace.request.RequestSessionStatus
import com.findprofessional.marketplace.request.RequestAnswerRepository
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.user.UserAccountRepository
import com.findprofessional.marketplace.user.UserAccount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import java.util.UUID
import java.util.Optional

class ProfessionalOpportunityServiceTest {
    private val authorization = mock(ProfessionalAuthorizationService::class.java)
    private val offerings = mock(ProfessionalServiceOfferingRepository::class.java)
    private val requests = mock(CustomerRequestRepository::class.java)
    private val locations = mock(RequestLocationRepository::class.java)
    private val profiles = mock(ProfessionalProfileRepository::class.java)
    private val offers = mock(ProfessionalOfferRepository::class.java)
    private val declines = mock(ProfessionalOpportunityDeclineRepository::class.java)
    private val answers = mock(RequestAnswerRepository::class.java)
    private val users = mock(UserAccountRepository::class.java)
    private val service = ProfessionalOpportunityService(
        authorization,
        offerings,
        requests,
        locations,
        profiles,
        offers,
        declines,
        answers,
        users
    )

    @Test
    fun `returns published requests matching offered services with preferred location`() {
        val professionalId = UUID.randomUUID()
        val marketplaceService = opportunityService()
        val request = customerRequest(marketplaceService)
        val location = RequestLocation(
            request = request,
            kind = RequestLocationKind.SERVICE,
            municipality = "Gothenburg",
            postalCode = "418 33",
            latitude = 57.7,
            longitude = 11.9
        )
        `when`(offerings.findAllByProfessionalUserIdOrderByDisplayOrderAsc(professionalId)).thenReturn(
            listOf(ProfessionalServiceOffering(
                professionalUserId = professionalId,
                service = marketplaceService,
                primary = true,
                displayOrder = 0
            ))
        )
        `when`(
            requests.findProfessionalOpportunities(
                CustomerRequestStatus.PUBLISHED,
                listOf(marketplaceService.id),
                professionalId,
                PageRequest.of(0, 500)
            )
        ).thenReturn(listOf(request))
        `when`(
            requests.countProfessionalOpportunities(
                CustomerRequestStatus.PUBLISHED,
                listOf(marketplaceService.id),
                professionalId
            )
        ).thenReturn(1)
        `when`(locations.findAllByRequestIdIn(listOf(request.id))).thenReturn(listOf(location))
        `when`(profiles.findById(professionalId)).thenReturn(Optional.empty())
        `when`(offers.findAllByProfessionalUserIdAndRequestIdIn(professionalId, listOf(request.id)))
            .thenReturn(emptyList())

        val response = service.getOpportunities(professionalId)

        assertEquals(1, response.totalCount)
        assertEquals(request.id, response.opportunities.single().id)
        assertEquals("Gothenburg", response.opportunities.single().location?.municipality)
        assertEquals(listOf(OpportunityMatchReason.SERVICE), response.opportunities.single().matchReasons)
        verify(authorization).requireProfessional(professionalId)
    }

    @Test
    fun `professional without offered services receives an empty feed`() {
        val professionalId = UUID.randomUUID()
        `when`(offerings.findAllByProfessionalUserIdOrderByDisplayOrderAsc(professionalId)).thenReturn(emptyList())

        val response = service.getOpportunities(professionalId)

        assertEquals(0, response.totalCount)
        assertTrue(response.opportunities.isEmpty())
        verifyNoInteractions(requests, locations)
    }

    @Test
    fun `returns all offers owned by professional with request location`() {
        val professionalId = UUID.randomUUID()
        val marketplaceService = opportunityService()
        val request = customerRequest(marketplaceService)
        val offer = ProfessionalOffer(
            professionalUserId = professionalId,
            request = request,
            amount = java.math.BigDecimal("12500.00"),
            currency = "EUR",
            status = ProfessionalOfferStatus.SUBMITTED
        )
        val location = RequestLocation(
            request = request,
            kind = RequestLocationKind.SERVICE,
            municipality = "Gothenburg",
            postalCode = "418 33",
            latitude = 57.7,
            longitude = 11.9
        )
        `when`(offers.findAllByProfessionalUserIdOrderByUpdatedAtDesc(professionalId)).thenReturn(listOf(offer))
        `when`(locations.findAllByRequestIdIn(listOf(request.id))).thenReturn(listOf(location))
        `when`(profiles.findById(professionalId)).thenReturn(
            Optional.of(
                ProfessionalProfile(
                    userId = professionalId,
                    businessName = "Nordic Roofs",
                    primaryService = marketplaceService,
                    serviceArea = "Gothenburg",
                    experienceYears = 8,
                    contactEmail = "pro@example.com",
                    about = "Experienced roofers.",
                    servicePostalCode = "418 33",
                    latitude = 57.7,
                    longitude = 11.9,
                    serviceRadiusKm = 50.0
                )
            )
        )

        val response = service.getOffers(professionalId)

        assertEquals(1, response.totalCount)
        assertEquals(request.id, response.opportunities.single().id)
        assertEquals("Gothenburg", response.opportunities.single().location?.municipality)
        assertEquals(0.0, response.opportunities.single().distanceKm)
        assertEquals(
            listOf(OpportunityMatchReason.SERVICE, OpportunityMatchReason.LOCATION),
            response.opportunities.single().matchReasons
        )
        assertEquals("EUR", response.opportunities.single().offer?.currency)
        verify(authorization).requireProfessional(professionalId)
    }

    @Test
    fun `filters requests outside professional radius and explains nearby match`() {
        val professionalId = UUID.randomUUID()
        val marketplaceService = opportunityService()
        val nearby = customerRequest(marketplaceService)
        val distant = customerRequest(marketplaceService)
        `when`(offerings.findAllByProfessionalUserIdOrderByDisplayOrderAsc(professionalId)).thenReturn(
            listOf(ProfessionalServiceOffering(professionalUserId = professionalId, service = marketplaceService, primary = true, displayOrder = 0))
        )
        `when`(
            requests.findProfessionalOpportunities(
                CustomerRequestStatus.PUBLISHED,
                listOf(marketplaceService.id),
                professionalId,
                PageRequest.of(0, 500)
            )
        ).thenReturn(listOf(nearby, distant))
        `when`(locations.findAllByRequestIdIn(listOf(nearby.id, distant.id))).thenReturn(
            listOf(
                RequestLocation(request = nearby, kind = RequestLocationKind.SERVICE, municipality = "Gothenburg", postalCode = "418 33", latitude = 57.70, longitude = 11.90),
                RequestLocation(request = distant, kind = RequestLocationKind.SERVICE, municipality = "Stockholm", postalCode = "111 20", latitude = 59.33, longitude = 18.07)
            )
        )
        `when`(profiles.findById(professionalId)).thenReturn(
            Optional.of(
                ProfessionalProfile(
                    userId = professionalId,
                    businessName = "Nordic Roofs",
                    primaryService = marketplaceService,
                    serviceArea = "Gothenburg",
                    experienceYears = 8,
                    contactEmail = "pro@example.com",
                    about = "Experienced roofers.",
                    servicePostalCode = "418 33",
                    latitude = 57.70,
                    longitude = 11.90,
                    serviceRadiusKm = 50.0
                )
            )
        )
        `when`(offers.findAllByProfessionalUserIdAndRequestIdIn(professionalId, listOf(nearby.id)))
            .thenReturn(emptyList())

        val response = service.getOpportunities(professionalId)

        assertEquals(1, response.totalCount)
        assertEquals(nearby.id, response.opportunities.single().id)
        assertEquals(listOf(OpportunityMatchReason.SERVICE, OpportunityMatchReason.LOCATION), response.opportunities.single().matchReasons)
        assertEquals(0.0, response.opportunities.single().distanceKm)
    }

    @Test
    fun `detail returns real customer and structured requirements`() {
        val professionalId = UUID.randomUUID()
        val marketplaceService = opportunityService()
        val request = customerRequest(marketplaceService)
        val question = ServiceQuestion(
            service = marketplaceService,
            key = "property_size",
            prompt = "How large is the property?",
            type = QuestionType.TEXT,
            displayOrder = 1
        )
        `when`(offerings.existsByProfessionalUserIdAndServiceId(professionalId, marketplaceService.id)).thenReturn(true)
        `when`(requests.findById(request.id)).thenReturn(Optional.of(request))
        `when`(declines.existsByProfessionalUserIdAndRequestId(professionalId, request.id)).thenReturn(false)
        `when`(locations.findAllByRequestIdIn(listOf(request.id))).thenReturn(emptyList())
        `when`(profiles.findById(professionalId)).thenReturn(Optional.empty())
        `when`(users.findById(request.customerId)).thenReturn(
            Optional.of(UserAccount(id = request.customerId, displayName = "Erik Andersson", phoneVerified = true))
        )
        `when`(answers.findAllBySessionId(request.session.id)).thenReturn(
            listOf(RequestAnswer(session = request.session, question = question, value = "180 sqm"))
        )
        `when`(offers.findByProfessionalUserIdAndRequestId(professionalId, request.id)).thenReturn(Optional.empty())

        val response = service.getOpportunity(professionalId, request.id)

        assertEquals("Erik Andersson", response.customer?.displayName)
        assertEquals(true, response.customer?.verified)
        assertEquals("How large is the property?", response.requirements.single().label)
        assertEquals("180 sqm", response.requirements.single().value)
    }
}

private fun opportunityService(): MarketplaceService {
    val category = ServiceCategory(code = "HOME", name = "Home", iconKey = "home", displayOrder = 1)
    return MarketplaceService(
        category = category,
        code = "ROOFING",
        name = "Roofing",
        shortDescription = "Roof installation and repair",
        iconKey = "roofing"
    )
}

private fun customerRequest(service: MarketplaceService): CustomerRequest {
    val customerId = UUID.randomUUID()
    val session = RequestSession(
        customerId = customerId,
        category = service.category,
        service = service,
        status = RequestSessionStatus.CONFIRMED
    )
    return CustomerRequest(
        session = session,
        customerId = customerId,
        category = service.category,
        service = service,
        title = "Replace a tiled roof",
        description = "Replace the existing tiled roof on a detached home."
    )
}
