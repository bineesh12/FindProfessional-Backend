package com.findprofessional.marketplace.professional

import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.service.MarketplaceService
import com.findprofessional.marketplace.service.MarketplaceServiceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockMultipartFile
import java.util.Optional
import java.util.UUID

class ProfessionalPortfolioServiceTest {
    private val projects = mock(PortfolioProjectRepository::class.java)
    private val images = mock(PortfolioImageRepository::class.java)
    private val services = mock(MarketplaceServiceRepository::class.java)
    private val offerings = mock(ProfessionalServiceOfferingRepository::class.java)
    private val profiles = mock(ProfessionalProfileRepository::class.java)
    private val authorization = mock(ProfessionalAuthorizationService::class.java)
    private val storage = mock(PortfolioImageStorage::class.java)
    private val service = ProfessionalPortfolioService(
        projects,
        images,
        services,
        offerings,
        profiles,
        authorization,
        storage,
        PortfolioStorageProperties(publicPath = "/uploads")
    )

    @Test
    fun `create normalizes and associates project with authenticated professional`() {
        val userId = UUID.randomUUID()
        val marketplaceService = portfolioMarketplaceService()
        `when`(profiles.existsById(userId)).thenReturn(true)
        `when`(projects.findAllByProfessionalUserIdOrderByDisplayOrderAscCreatedAtAsc(userId)).thenReturn(emptyList())
        `when`(offerings.findAllByProfessionalUserIdOrderByDisplayOrderAsc(userId)).thenReturn(
            listOf(
                ProfessionalServiceOffering(
                    professionalUserId = userId,
                    service = marketplaceService,
                    primary = true,
                    displayOrder = 0
                )
            )
        )
        `when`(services.findById(marketplaceService.id)).thenReturn(Optional.of(marketplaceService))
        `when`(projects.save(org.mockito.ArgumentMatchers.any(PortfolioProject::class.java)))
            .thenAnswer { it.arguments[0] as PortfolioProject }

        val response = service.create(
            userId,
            SavePortfolioProjectRequest(marketplaceService.id, " Garden update ", " New lawn and stone path. ")
        )

        assertEquals("Garden update", response.title)
        assertEquals(userId, org.mockito.ArgumentCaptor.forClass(PortfolioProject::class.java).run {
            verify(projects).save(capture())
            value.professionalUserId
        })
        verify(authorization).requireProfessional(userId)
    }

    @Test
    fun `update cannot access another professional project`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        `when`(projects.findByIdAndProfessionalUserId(projectId, userId)).thenReturn(Optional.empty())

        val error = assertThrows(ProfessionalProfileException::class.java) {
            service.update(
                userId,
                projectId,
                SavePortfolioProjectRequest(UUID.randomUUID(), "Project", "A valid description")
            )
        }

        assertEquals("PORTFOLIO_NOT_FOUND", error.code)
    }

    @Test
    fun `upload validates image signature before writing storage`() {
        val userId = UUID.randomUUID()
        val project = portfolioProject(userId)
        `when`(projects.findByIdAndProfessionalUserId(project.id, userId)).thenReturn(Optional.of(project))
        `when`(images.countByProjectId(project.id)).thenReturn(0)
        val fakeImage = MockMultipartFile("file", "photo.jpg", "image/jpeg", "not-an-image".encodeToByteArray())

        val error = assertThrows(ProfessionalProfileException::class.java) {
            service.uploadImage(userId, project.id, fakeImage)
        }

        assertEquals("VALIDATION_ERROR", error.code)
        verifyNoInteractions(storage)
    }

    @Test
    fun `upload stores valid image metadata for owned project`() {
        val userId = UUID.randomUUID()
        val project = portfolioProject(userId)
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)
        val file = MockMultipartFile("file", "roof.jpg", "image/jpeg", bytes)
        `when`(projects.findByIdAndProfessionalUserId(project.id, userId)).thenReturn(Optional.of(project))
        `when`(images.countByProjectId(project.id)).thenReturn(0)
        `when`(storage.save(userId, project.id, "jpg", bytes)).thenReturn(StoredPortfolioImage("portfolio/photo.jpg"))
        `when`(images.save(org.mockito.ArgumentMatchers.any(PortfolioImage::class.java)))
            .thenAnswer { it.arguments[0] as PortfolioImage }

        val response = service.uploadImage(userId, project.id, file)

        assertEquals("/uploads/portfolio/photo.jpg", response.url)
        assertEquals("image/jpeg", response.contentType)
    }
}

private fun portfolioMarketplaceService() = MarketplaceService(
    category = ServiceCategory(code = "GARDEN", name = "Garden", iconKey = "garden", displayOrder = 1),
    code = "GARDEN_MAINTENANCE",
    name = "Garden maintenance",
    shortDescription = "Garden services",
    iconKey = "garden"
)

private fun portfolioProject(userId: UUID) = PortfolioProject(
    professionalUserId = userId,
    service = portfolioMarketplaceService(),
    title = "Garden update",
    description = "New lawn and stone path.",
    displayOrder = 0
)
