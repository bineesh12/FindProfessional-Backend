package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.category.category
import com.findprofessional.marketplace.matching.ProfessionalOfferRepository
import com.findprofessional.marketplace.matching.ProfessionalOffer
import com.findprofessional.marketplace.matching.ProfessionalOfferAttachment
import com.findprofessional.marketplace.matching.ProfessionalOfferStatus
import com.findprofessional.marketplace.matching.ProfessionalOfferAttachmentRepository
import com.findprofessional.marketplace.matching.RequestOfferCount
import com.findprofessional.marketplace.matching.OpportunityNotificationMatcher
import com.findprofessional.marketplace.notification.CreateNotification
import com.findprofessional.marketplace.notification.MarketplaceNotificationType
import com.findprofessional.marketplace.notification.NotificationService
import com.findprofessional.marketplace.question.QuestionType
import com.findprofessional.marketplace.question.ServiceQuestion
import com.findprofessional.marketplace.professional.ProfessionalProfile
import com.findprofessional.marketplace.professional.ProfessionalProfileRepository
import com.findprofessional.marketplace.professional.PortfolioImageRepository
import com.findprofessional.marketplace.professional.PortfolioProjectRepository
import com.findprofessional.marketplace.professional.PortfolioStorageProperties
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.UUID
import java.util.Optional
import java.math.BigDecimal

class CustomerRequestQueryServiceTest {
    private val authorization = mock(CustomerAuthorizationService::class.java)
    private val requests = mock(CustomerRequestRepository::class.java)
    private val locations = mock(RequestLocationRepository::class.java)
    private val answers = mock(RequestAnswerRepository::class.java)
    private val offers = mock(ProfessionalOfferRepository::class.java)
    private val profiles = mock(ProfessionalProfileRepository::class.java)
    private val portfolioProjects = mock(PortfolioProjectRepository::class.java)
    private val portfolioImages = mock(PortfolioImageRepository::class.java)
    private val offerAttachments = mock(ProfessionalOfferAttachmentRepository::class.java)
    private val opportunityMatcher = mock(OpportunityNotificationMatcher::class.java)
    private val notifications = mock(NotificationService::class.java)
    private val service = CustomerRequestQueryService(
        authorization,
        requests,
        locations,
        answers,
        offers,
        profiles,
        portfolioProjects,
        portfolioImages,
        offerAttachments,
        PortfolioStorageProperties(),
        opportunityMatcher,
        notifications
    )

    @Test
    fun `list returns customer requests with location budget and submitted offer count`() {
        val fixture = fixture()
        val pageable = PageRequest.of(0, 20)
        val offerCount = mock(RequestOfferCount::class.java)
        `when`(
            requests.countByCustomerIdAndStatusIn(
                fixture.userId,
                setOf(CustomerRequestStatus.PUBLISHED, CustomerRequestStatus.HIRED)
            )
        )
            .thenReturn(1)
        `when`(requests.countByCustomerIdAndStatusIn(fixture.userId, setOf(CustomerRequestStatus.COMPLETED)))
            .thenReturn(0)
        `when`(
            requests.findAllByCustomerIdAndStatusInOrderByCreatedAtDesc(
                fixture.userId,
                setOf(
                    CustomerRequestStatus.PUBLISHED,
                    CustomerRequestStatus.HIRED,
                    CustomerRequestStatus.COMPLETED
                ),
                pageable
            )
        ).thenReturn(PageImpl(listOf(fixture.request), pageable, 1))
        `when`(locations.findAllByRequestIdIn(listOf(fixture.request.id))).thenReturn(listOf(fixture.location))
        `when`(answers.findAllBySessionIdIn(listOf(fixture.session.id))).thenReturn(listOf(fixture.budgetAnswer))
        `when`(
            offers.countByRequestIdsAndStatusIn(
                listOf(fixture.request.id),
                setOf(ProfessionalOfferStatus.SUBMITTED, ProfessionalOfferStatus.ACCEPTED)
            )
        )
            .thenReturn(listOf(offerCount))
        `when`(offerCount.requestId).thenReturn(fixture.request.id)
        `when`(offerCount.offerCount).thenReturn(3)

        val response = service.list(fixture.userId, CustomerRequestFilter.ALL, 0, 20)

        assertEquals(1, response.counts.all)
        assertEquals(1, response.counts.active)
        assertEquals(0, response.counts.completed)
        with(response.requests.single()) {
            assertEquals(fixture.request.id, id)
            assertEquals("Gothenburg", location?.municipality)
            assertEquals("15000", budget?.amount)
            assertEquals("SEK", budget?.currency)
            assertEquals(3, submittedOfferCount)
            assertFalse(canEdit)
            assertFalse(canDelete)
        }
        verify(authorization).requireCustomer(fixture.userId)
    }

    @Test
    fun `completed filter queries only completed requests`() {
        val userId = UUID.randomUUID()
        val pageable = PageRequest.of(0, 20)
        `when`(
            requests.countByCustomerIdAndStatusIn(
                userId,
                setOf(CustomerRequestStatus.PUBLISHED, CustomerRequestStatus.HIRED)
            )
        ).thenReturn(2)
        `when`(requests.countByCustomerIdAndStatusIn(userId, setOf(CustomerRequestStatus.COMPLETED))).thenReturn(0)
        `when`(
            requests.findAllByCustomerIdAndStatusInOrderByCreatedAtDesc(
                userId,
                setOf(CustomerRequestStatus.COMPLETED),
                pageable
            )
        ).thenReturn(PageImpl(emptyList(), pageable, 0))

        val response = service.list(userId, CustomerRequestFilter.COMPLETED, 0, 20)

        assertEquals(2, response.counts.all)
        assertEquals(0, response.totalCount)
        assertEquals(emptyList<CustomerRequestListItemResponse>(), response.requests)
        verifyNoInteractions(locations, answers, offers)
    }

    @Test
    fun `invalid page size is rejected`() {
        assertThrows(RequestException::class.java) {
            service.list(UUID.randomUUID(), CustomerRequestFilter.ALL, 0, 51)
        }
    }

    @Test
    fun `update changes owned request when no offers were submitted`() {
        val fixture = fixture()
        `when`(requests.findByIdAndCustomerId(fixture.request.id, fixture.userId))
            .thenReturn(Optional.of(fixture.request))
        `when`(
            offers.countByRequestIdAndStatusIn(
                fixture.request.id,
                setOf(ProfessionalOfferStatus.SUBMITTED, ProfessionalOfferStatus.ACCEPTED)
            )
        )
            .thenReturn(0)
        val professionalId = UUID.randomUUID()
        `when`(opportunityMatcher.matchingProfessionalIds(fixture.request)).thenReturn(listOf(professionalId))

        val response = service.update(
            fixture.userId,
            fixture.request.id,
            UpdateCustomerRequestRequest(
                title = "  Updated kitchen request  ",
                description = "  Replace the cabinets, flooring, and kitchen lighting.  "
            )
        )

        assertEquals("Updated kitchen request", response.title)
        assertEquals("Replace the cabinets, flooring, and kitchen lighting.", response.description)
        assertEquals(response.title, fixture.request.title)
        verify(authorization).requireCustomer(fixture.userId)
        verify(notifications).create(
            CreateNotification(
                userId = professionalId,
                type = MarketplaceNotificationType.REQUEST_UPDATED,
                title = "Opportunity updated",
                body = fixture.request.title,
                requestId = fixture.request.id
            )
        )
    }

    @Test
    fun `update rejects request after an offer was submitted`() {
        val fixture = fixture()
        `when`(requests.findByIdAndCustomerId(fixture.request.id, fixture.userId))
            .thenReturn(Optional.of(fixture.request))
        `when`(
            offers.countByRequestIdAndStatusIn(
                fixture.request.id,
                setOf(ProfessionalOfferStatus.SUBMITTED, ProfessionalOfferStatus.ACCEPTED)
            )
        )
            .thenReturn(1)

        val error = assertThrows(RequestException::class.java) {
            service.update(
                fixture.userId,
                fixture.request.id,
                UpdateCustomerRequestRequest(
                    "Updated kitchen request",
                    "Replace the cabinets, flooring, and kitchen lighting."
                )
            )
        }

        assertEquals("REQUEST_HAS_OFFERS", error.code)
    }

    @Test
    fun `offers returns only submitted offers for request owner with business name`() {
        val fixture = fixture()
        val professionalId = UUID.randomUUID()
        val offer = ProfessionalOffer(
            professionalUserId = professionalId,
            request = fixture.request,
            amount = BigDecimal("12500.00"),
            currency = "SEK",
            message = "Complete installation and cleanup.",
            status = ProfessionalOfferStatus.SUBMITTED
        )
        val profile = ProfessionalProfile(
            userId = professionalId,
            businessName = "Nordic Renovation",
            primaryService = fixture.request.service,
            serviceArea = "Gothenburg",
            experienceYears = 8,
            contactEmail = "pro@example.com",
            about = "Renovation specialists with local experience."
        )
        `when`(requests.findByIdAndCustomerId(fixture.request.id, fixture.userId))
            .thenReturn(Optional.of(fixture.request))
        `when`(
            offers.findAllByRequestIdAndStatusInOrderByUpdatedAtDesc(
                fixture.request.id,
                setOf(ProfessionalOfferStatus.SUBMITTED)
            )
        ).thenReturn(listOf(offer))
        `when`(profiles.findAllById(listOf(professionalId))).thenReturn(listOf(profile))
        `when`(portfolioProjects.findAllByProfessionalUserIdIn(listOf(professionalId))).thenReturn(emptyList())
        `when`(locations.findAllByRequestIdIn(listOf(fixture.request.id))).thenReturn(listOf(fixture.location))
        `when`(answers.findAllBySessionId(fixture.session.id)).thenReturn(listOf(fixture.budgetAnswer))

        val response = service.offers(fixture.userId, fixture.request.id, CustomerOfferSort.LATEST)

        assertEquals("Nordic Renovation", response.offers.single().professional.businessName)
        assertEquals("12500", response.offers.single().amount)
        assertEquals("SEK", response.offers.single().currency)
        assertEquals("Gothenburg", response.request.location?.municipality)
        assertEquals("15000", response.request.budget?.amount)
    }

    @Test
    fun `offers sort lowest price first`() {
        val fixture = fixture()
        val professionalId = UUID.randomUUID()
        val higher = submittedOffer(fixture.request, professionalId, "15000")
        val lower = submittedOffer(fixture.request, professionalId, "9500")
        val profile = professionalProfile(professionalId, fixture.request.service)
        stubOfferContext(fixture, professionalId, profile)
        `when`(
            offers.findAllByRequestIdAndStatusInOrderByUpdatedAtDesc(
                fixture.request.id,
                setOf(ProfessionalOfferStatus.SUBMITTED)
            )
        ).thenReturn(listOf(higher, lower))

        val response = service.offers(fixture.userId, fixture.request.id, CustomerOfferSort.PRICE_ASC)

        assertEquals(listOf("9500", "15000"), response.offers.map { it.amount })
    }

    @Test
    fun `offer details returns submitted offer attachments for request owner`() {
        val fixture = fixture()
        val professionalId = UUID.randomUUID()
        val offer = submittedOffer(fixture.request, professionalId, "12500")
        val profile = professionalProfile(professionalId, fixture.request.service)
        val attachment = ProfessionalOfferAttachment(
            offer = offer,
            storageKey = "offers/quote.pdf",
            originalFilename = "quote.pdf",
            contentType = "application/pdf",
            sizeBytes = 1200
        )
        stubOfferContext(fixture, professionalId, profile)
        `when`(
            offers.findByIdAndRequestIdAndStatusIn(
                offer.id,
                fixture.request.id,
                setOf(
                    ProfessionalOfferStatus.SUBMITTED,
                    ProfessionalOfferStatus.ACCEPTED,
                    ProfessionalOfferStatus.DECLINED
                )
            )
        ).thenReturn(Optional.of(offer))
        `when`(
            offers.countByRequestIdAndStatusIn(
                fixture.request.id,
                setOf(ProfessionalOfferStatus.SUBMITTED, ProfessionalOfferStatus.ACCEPTED)
            )
        )
            .thenReturn(1)
        `when`(offerAttachments.findAllByOfferIdOrderByCreatedAtAsc(offer.id)).thenReturn(listOf(attachment))

        val response = service.offerDetails(fixture.userId, fixture.request.id, offer.id)

        assertEquals("Nordic Renovation", response.offer.professional.businessName)
        assertEquals("quote.pdf", response.attachments.single().filename)
        assertEquals("/uploads/offers/quote.pdf", response.attachments.single().url)
    }

    private fun stubOfferContext(
        fixture: Fixture,
        professionalId: UUID,
        profile: ProfessionalProfile
    ) {
        `when`(requests.findByIdAndCustomerId(fixture.request.id, fixture.userId))
            .thenReturn(Optional.of(fixture.request))
        `when`(profiles.findAllById(listOf(professionalId))).thenReturn(listOf(profile))
        `when`(portfolioProjects.findAllByProfessionalUserIdIn(listOf(professionalId))).thenReturn(emptyList())
        `when`(locations.findAllByRequestIdIn(listOf(fixture.request.id))).thenReturn(listOf(fixture.location))
        `when`(answers.findAllBySessionId(fixture.session.id)).thenReturn(listOf(fixture.budgetAnswer))
    }

    private fun professionalProfile(professionalId: UUID, marketplaceService: MarketplaceService) =
        ProfessionalProfile(
            userId = professionalId,
            businessName = "Nordic Renovation",
            primaryService = marketplaceService,
            serviceArea = "Gothenburg",
            experienceYears = 8,
            contactEmail = "pro@example.com",
            about = "Renovation specialists with local experience."
        )

    private fun submittedOffer(request: CustomerRequest, professionalId: UUID, amount: String) =
        ProfessionalOffer(
            professionalUserId = professionalId,
            request = request,
            amount = BigDecimal(amount),
            currency = "SEK",
            message = "Complete installation and cleanup.",
            status = ProfessionalOfferStatus.SUBMITTED
        )

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
        val request = CustomerRequest(
            session = session,
            customerId = userId,
            category = category,
            service = marketplaceService,
            title = "Renovate the kitchen",
            description = "Complete kitchen renovation with new cabinets and flooring."
        )
        val budgetQuestion = ServiceQuestion(
            service = marketplaceService,
            key = "budget",
            prompt = "What approximate budget are you considering?",
            type = QuestionType.MONEY,
            displayOrder = 1
        )
        return Fixture(
            userId,
            session,
            request,
            RequestLocation(
                request = request,
                kind = RequestLocationKind.SERVICE,
                municipality = "Gothenburg",
                postalCode = "418 33",
                latitude = 57.7,
                longitude = 11.9
            ),
            RequestAnswer(session = session, question = budgetQuestion, value = "15000 SEK")
        )
    }

    private data class Fixture(
        val userId: UUID,
        val session: RequestSession,
        val request: CustomerRequest,
        val location: RequestLocation,
        val budgetAnswer: RequestAnswer
    )
}
