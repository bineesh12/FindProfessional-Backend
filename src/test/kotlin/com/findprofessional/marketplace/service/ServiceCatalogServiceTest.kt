package com.findprofessional.marketplace.service

import com.findprofessional.marketplace.auth.AuthException
import com.findprofessional.marketplace.category.ServiceCategoryRepository
import com.findprofessional.marketplace.category.category
import com.findprofessional.marketplace.user.CustomerAuthorizationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class ServiceCatalogServiceTest {
    private val services = mock(MarketplaceServiceRepository::class.java)
    private val categories = mock(ServiceCategoryRepository::class.java)
    private val nearbyServices = mock(NearbyServiceRepository::class.java)
    private val authorization = mock(CustomerAuthorizationService::class.java)
    private val catalog = ServiceCatalogService(services, categories, nearbyServices, authorization)

    @Test
    fun `popular services expose backend content and category`() {
        val userId = UUID.randomUUID()
        val service = marketplaceService()
        `when`(services.findAllByPopularTrueAndActiveTrueOrderByPopularityRankAsc())
            .thenReturn(listOf(service))

        val response = catalog.listPopular(userId)

        verify(authorization).requireCustomer(userId)
        assertEquals("Home renovation", response.single().name)
        assertEquals("Home", response.single().categoryName)
    }

    @Test
    fun `nearby services are ranked by repository and distance is rounded`() {
        val userId = UUID.randomUUID()
        val match = NearbyServiceMatch(marketplaceService().toResponse(), 2.345)
        `when`(nearbyServices.findNearby(59.3293, 18.0686, 50.0, 10))
            .thenReturn(listOf(match))

        val response = catalog.listNearby(userId, 59.3293, 18.0686, 50.0)

        verify(authorization).requireCustomer(userId)
        assertEquals(2.3, response.single().distanceKm)
        assertEquals("Home renovation", response.single().service.name)
    }

    @Test
    fun `invalid nearby coordinates are rejected before repository access`() {
        val error = assertThrows(AuthException::class.java) {
            catalog.listNearby(UUID.randomUUID(), 91.0, 18.0686, 50.0)
        }

        assertEquals("INVALID_LOCATION", error.code)
        verifyNoInteractions(nearbyServices)
    }

    @Test
    fun `search trims and normalizes query`() {
        val userId = UUID.randomUUID()
        `when`(services.search("%roof repair%")).thenReturn(listOf(marketplaceService()))

        val response = catalog.search(userId, "  Roof Repair  ")

        verify(services).search("%roof repair%")
        assertEquals(1, response.size)
    }

    @Test
    fun `short search query is rejected before repository access`() {
        val error = assertThrows(AuthException::class.java) {
            catalog.search(UUID.randomUUID(), "a")
        }

        assertEquals("INVALID_SEARCH_QUERY", error.code)
        verifyNoInteractions(services)
    }

    @Test
    fun `inactive category is not exposed`() {
        val userId = UUID.randomUUID()
        val category = category(active = false)
        `when`(categories.findById(category.id)).thenReturn(Optional.of(category))

        val error = assertThrows(AuthException::class.java) {
            catalog.listByCategory(userId, category.id)
        }

        assertEquals("CATEGORY_NOT_FOUND", error.code)
    }
}

internal fun marketplaceService() = MarketplaceService(
    id = UUID.randomUUID(),
    category = category(),
    code = "HOME_RENOVATION",
    name = "Home renovation",
    shortDescription = "Top rated professionals",
    iconKey = "construction",
    imageUrl = "https://example.com/home.jpg",
    searchKeywords = "renovate roof",
    popular = true,
    popularityRank = 1
)
