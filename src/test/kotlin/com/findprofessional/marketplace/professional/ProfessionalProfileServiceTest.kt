package com.findprofessional.marketplace.professional

import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.service.MarketplaceServiceRepository
import com.findprofessional.marketplace.request.RequestLocationService
import com.findprofessional.marketplace.request.PostcodeCoordinate
import com.findprofessional.marketplace.user.UserAccount
import com.findprofessional.marketplace.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class ProfessionalProfileServiceTest {
    private val profiles = mock(ProfessionalProfileRepository::class.java)
    private val services = mock(MarketplaceServiceRepository::class.java)
    private val offerings = mock(ProfessionalServiceOfferingRepository::class.java)
    private val portfolio = mock(ProfessionalPortfolioService::class.java)
    private val authorization = mock(ProfessionalAuthorizationService::class.java)
    private val locations = mock(RequestLocationService::class.java)
    private val service = ProfessionalProfileService(profiles, services, offerings, portfolio, authorization, locations)

    init {
        `when`(locations.resolvePostcode(anyString())).thenReturn(
            PostcodeCoordinate(
                countryCode = "se",
                postalCode = "418 33",
                latitude = 57.72,
                longitude = 11.95,
                source = "TEST"
            )
        )
    }

    @Test
    fun `setup returns service options and no profile for first use`() {
        val userId = UUID.randomUUID()
        val marketplaceService = marketplaceService()
        `when`(authorization.requireProfessional(userId)).thenReturn(
            UserAccount(
                id = userId,
                email = "pro@example.com",
                displayName = "Professional",
                roles = mutableSetOf(UserRole.PROFESSIONAL)
            )
        )
        `when`(profiles.findById(userId)).thenReturn(Optional.empty())
        `when`(services.findAllActiveForProfessionalSetup()).thenReturn(listOf(marketplaceService))
        `when`(portfolio.loadProjects(userId)).thenReturn(emptyList())

        val response = service.setup(userId)

        assertNull(response.profile)
        assertEquals("pro@example.com", response.suggestedContactEmail)
        assertEquals(marketplaceService.id, response.services.single().id)
        assertEquals("Home", response.services.single().categoryName)
        verify(authorization).requireProfessional(userId)
    }

    @Test
    fun `save creates normalized professional profile`() {
        val userId = UUID.randomUUID()
        val marketplaceService = marketplaceService()
        val request = profileRequest(marketplaceService.id)
        `when`(services.findAllById(listOf(marketplaceService.id))).thenReturn(listOf(marketplaceService))
        `when`(profiles.findById(userId)).thenReturn(Optional.empty())
        `when`(profiles.save(org.mockito.ArgumentMatchers.any(ProfessionalProfile::class.java)))
            .thenAnswer { it.arguments[0] as ProfessionalProfile }

        val response = service.save(userId, request)

        assertEquals("Nordic Roofs", response.businessName)
        assertEquals("hello@example.com", response.contactEmail)
        assertEquals("418 33", response.servicePostalCode)
        assertEquals(marketplaceService.id, response.primaryService.id)
        verify(authorization).requireProfessional(userId)
    }

    @Test
    fun `save updates existing profile owned by authenticated user`() {
        val userId = UUID.randomUUID()
        val oldService = marketplaceService("Old service")
        val newService = marketplaceService("Roofing")
        val existing = ProfessionalProfile(
            userId,
            "Old business",
            oldService,
            "Malmo",
            1,
            "old@example.com",
            "An existing professional profile description."
        )
        `when`(services.findAllById(listOf(newService.id))).thenReturn(listOf(newService))
        `when`(profiles.findById(userId)).thenReturn(Optional.of(existing))
        `when`(profiles.save(existing)).thenReturn(existing)

        val response = service.save(userId, profileRequest(newService.id))

        assertEquals("Nordic Roofs", response.businessName)
        assertEquals(newService.id, response.primaryService.id)
    }

    @Test
    fun `inactive service cannot be selected`() {
        val inactive = marketplaceService(active = false)
        `when`(services.findAllById(listOf(inactive.id))).thenReturn(listOf(inactive))

        val error = assertThrows(ProfessionalProfileException::class.java) {
            service.save(UUID.randomUUID(), profileRequest(inactive.id))
        }

        assertEquals("SERVICE_NOT_FOUND", error.code)
    }

    @Test
    fun `normalized profile values must satisfy minimum lengths`() {
        val serviceId = UUID.randomUUID()
        val invalid = profileRequest(serviceId).copy(about = "                    ")

        val error = assertThrows(ProfessionalProfileException::class.java) {
            service.save(UUID.randomUUID(), invalid)
        }

        assertEquals("VALIDATION_ERROR", error.code)
    }

    @Test
    fun `professional location postcode is required`() {
        val invalid = profileRequest(UUID.randomUUID()).copy(servicePostalCode = "   ")

        val error = assertThrows(ProfessionalProfileException::class.java) {
            service.save(UUID.randomUUID(), invalid)
        }

        assertEquals("VALIDATION_ERROR", error.code)
    }

    @Test
    fun `professional service area is required`() {
        val invalid = profileRequest(UUID.randomUUID()).copy(serviceArea = "   ")

        val error = assertThrows(ProfessionalProfileException::class.java) {
            service.save(UUID.randomUUID(), invalid)
        }

        assertEquals("VALIDATION_ERROR", error.code)
    }
}

private fun profileRequest(serviceId: UUID) = SaveProfessionalProfileRequest(
    businessName = " Nordic Roofs ",
    primaryServiceId = serviceId,
    serviceArea = " Gothenburg ",
    servicePostalCode = " 418 33 ",
    experienceYears = 8,
    contactEmail = " HELLO@EXAMPLE.COM ",
    about = " Experienced roofers serving homes across the region. "
)

private fun marketplaceService(
    name: String = "Roofing",
    active: Boolean = true
): MarketplaceService = MarketplaceService(
    category = ServiceCategory(code = "HOME", name = "Home", iconKey = "home", displayOrder = 1),
    code = name.uppercase().replace(' ', '_'),
    name = name,
    shortDescription = "Professional service",
    iconKey = "roofing",
    active = active
)
