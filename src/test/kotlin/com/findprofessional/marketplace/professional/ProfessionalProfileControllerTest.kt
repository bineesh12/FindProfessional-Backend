package com.findprofessional.marketplace.professional

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.mock.web.MockMultipartFile
import java.util.UUID

class ProfessionalProfileControllerTest {
    private val service = mock(ProfessionalProfileService::class.java)
    private val portfolioService = mock(ProfessionalPortfolioService::class.java)
    private val controller = ProfessionalProfileController(service, portfolioService)

    @Test
    fun `setup uses authenticated user subject`() {
        val userId = UUID.randomUUID()
        val expected = ProfessionalProfileSetupResponse(null, "pro@example.com", emptyList(), emptyList())
        `when`(service.setup(userId)).thenReturn(expected)

        assertEquals(expected, controller.setup(jwt(userId)))
    }

    @Test
    fun `save uses authenticated user subject`() {
        val userId = UUID.randomUUID()
        val serviceId = UUID.randomUUID()
        val request = controllerProfileRequest(serviceId)
        val expected = ProfessionalProfileResponse(
            businessName = "Nordic Roofs",
            primaryService = ProfessionalServiceOptionResponse(serviceId, "Roofing", "Home"),
            offeredServices = listOf(ProfessionalServiceOptionResponse(serviceId, "Roofing", "Home")),
            serviceArea = "Gothenburg",
            experienceYears = 8,
            contactEmail = "hello@example.com",
            about = "Experienced roofers serving homes across the region."
        )
        `when`(service.save(userId, request)).thenReturn(expected)

        assertEquals(expected, controller.save(jwt(userId), request))
    }

    @Test
    fun `portfolio creation uses authenticated user subject`() {
        val userId = UUID.randomUUID()
        val serviceId = UUID.randomUUID()
        val request = SavePortfolioProjectRequest(serviceId, "Garden update", "New lawn and stone path.")
        val expected = PortfolioProjectResponse(
            id = UUID.randomUUID(),
            title = request.title,
            description = request.description,
            service = ProfessionalServiceOptionResponse(serviceId, "Garden maintenance", "Garden"),
            images = emptyList()
        )
        `when`(portfolioService.create(userId, request)).thenReturn(expected)

        assertEquals(expected, controller.createPortfolioProject(jwt(userId), request))
    }

    @Test
    fun `portfolio image upload uses authenticated user and owned project`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val file = MockMultipartFile(
            "file",
            "garden.jpg",
            "image/jpeg",
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        )
        val expected = PortfolioImageResponse(UUID.randomUUID(), "/uploads/portfolio/image.jpg", "image/jpeg", 0)
        `when`(portfolioService.uploadImage(userId, projectId, file)).thenReturn(expected)

        assertEquals(expected, controller.uploadPortfolioImage(jwt(userId), projectId, file))
    }
}

private fun jwt(userId: UUID) = Jwt.withTokenValue("access-token")
    .header("alg", "HS256")
    .subject(userId.toString())
    .build()

private fun controllerProfileRequest(serviceId: UUID) = SaveProfessionalProfileRequest(
    businessName = "Nordic Roofs",
    primaryServiceId = serviceId,
    serviceArea = "Gothenburg",
    servicePostalCode = "418 33",
    experienceYears = 8,
    contactEmail = "hello@example.com",
    about = "Experienced roofers serving homes across the region."
)
