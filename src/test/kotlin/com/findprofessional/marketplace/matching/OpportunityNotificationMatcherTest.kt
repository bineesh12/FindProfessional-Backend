package com.findprofessional.marketplace.matching

import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.professional.ProfessionalProfile
import com.findprofessional.marketplace.professional.ProfessionalProfileRepository
import com.findprofessional.marketplace.professional.ProfessionalServiceOffering
import com.findprofessional.marketplace.professional.ProfessionalServiceOfferingRepository
import com.findprofessional.marketplace.request.CustomerRequest
import com.findprofessional.marketplace.request.RequestLocation
import com.findprofessional.marketplace.request.RequestLocationKind
import com.findprofessional.marketplace.request.RequestLocationRepository
import com.findprofessional.marketplace.request.RequestSession
import com.findprofessional.marketplace.request.RequestSessionStatus
import com.findprofessional.marketplace.service.MarketplaceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class OpportunityNotificationMatcherTest {
    private val offerings = mock(ProfessionalServiceOfferingRepository::class.java)
    private val profiles = mock(ProfessionalProfileRepository::class.java)
    private val locations = mock(RequestLocationRepository::class.java)
    private val matcher = OpportunityNotificationMatcher(offerings, profiles, locations)

    @Test
    fun `matching respects service and professional working radius`() {
        val category = ServiceCategory(code = "HOME", name = "Home", iconKey = "home", displayOrder = 1)
        val service = MarketplaceService(
            category = category,
            code = "ROOFING",
            name = "Roofing",
            shortDescription = "Roof work",
            iconKey = "roof"
        )
        val customerId = UUID.randomUUID()
        val request = CustomerRequest(
            session = RequestSession(
                customerId = customerId,
                category = category,
                service = service,
                status = RequestSessionStatus.CONFIRMED
            ),
            customerId = customerId,
            category = category,
            service = service,
            title = "Repair roof",
            description = "Repair leaking roof"
        )
        val nearbyId = UUID.randomUUID()
        val distantId = UUID.randomUUID()
        `when`(offerings.findAllByServiceIdOrderByDisplayOrderAsc(service.id)).thenReturn(
            listOf(
                ProfessionalServiceOffering(professionalUserId = nearbyId, service = service, primary = true, displayOrder = 0),
                ProfessionalServiceOffering(professionalUserId = distantId, service = service, primary = true, displayOrder = 0)
            )
        )
        `when`(profiles.findAllById(listOf(nearbyId, distantId))).thenReturn(
            listOf(
                profile(nearbyId, service, 59.33, 18.06, 25.0),
                profile(distantId, service, 57.70, 11.97, 25.0)
            )
        )
        `when`(locations.findAllByRequestIdIn(listOf(request.id))).thenReturn(
            listOf(
                RequestLocation(
                    request = request,
                    kind = RequestLocationKind.SERVICE,
                    municipality = "Stockholm",
                    postalCode = "111 20",
                    latitude = 59.33,
                    longitude = 18.07
                )
            )
        )

        assertEquals(listOf(nearbyId), matcher.matchingProfessionalIds(request))
    }

    @Test
    fun `matching excludes service offerings without a professional profile`() {
        val category = ServiceCategory(code = "HOME", name = "Home", iconKey = "home", displayOrder = 1)
        val service = MarketplaceService(
            category = category,
            code = "ROOFING",
            name = "Roofing",
            shortDescription = "Roof work",
            iconKey = "roof"
        )
        val customerId = UUID.randomUUID()
        val request = CustomerRequest(
            session = RequestSession(
                customerId = customerId,
                category = category,
                service = service,
                status = RequestSessionStatus.CONFIRMED
            ),
            customerId = customerId,
            category = category,
            service = service,
            title = "Repair roof",
            description = "Repair leaking roof"
        )
        val professionalId = UUID.randomUUID()
        `when`(offerings.findAllByServiceIdOrderByDisplayOrderAsc(service.id)).thenReturn(
            listOf(
                ProfessionalServiceOffering(
                    professionalUserId = professionalId,
                    service = service,
                    primary = true,
                    displayOrder = 0
                )
            )
        )
        `when`(profiles.findAllById(listOf(professionalId))).thenReturn(emptyList())
        `when`(locations.findAllByRequestIdIn(listOf(request.id))).thenReturn(emptyList())

        assertEquals(emptyList<UUID>(), matcher.matchingProfessionalIds(request))
    }

    private fun profile(
        id: UUID,
        service: MarketplaceService,
        latitude: Double,
        longitude: Double,
        radius: Double
    ) = ProfessionalProfile(
        userId = id,
        businessName = "Business",
        primaryService = service,
        serviceArea = "Sweden",
        experienceYears = 2,
        contactEmail = "pro@example.com",
        about = "Professional service",
        latitude = latitude,
        longitude = longitude,
        serviceRadiusKm = radius
    )
}
