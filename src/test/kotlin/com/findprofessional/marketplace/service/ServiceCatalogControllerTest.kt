package com.findprofessional.marketplace.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

class ServiceCatalogControllerTest {
    @Test
    fun `search uses authenticated user subject`() {
        val userId = UUID.randomUUID()
        val catalog = mock(ServiceCatalogService::class.java)
        val expected = listOf(marketplaceService().toResponse())
        `when`(catalog.search(userId, "roof")).thenReturn(expected)
        val jwt = Jwt.withTokenValue("access-token")
            .header("alg", "HS256")
            .subject(userId.toString())
            .build()

        val response = ServiceCatalogController(catalog).search(jwt, "roof")

        assertEquals(expected, response)
    }

    @Test
    fun `nearby uses authenticated user and supplied coordinates`() {
        val userId = UUID.randomUUID()
        val catalog = mock(ServiceCatalogService::class.java)
        val expected = listOf(NearbyServiceResponse(marketplaceService().toResponse(), 3.2))
        `when`(catalog.listNearby(userId, 59.3293, 18.0686, 25.0)).thenReturn(expected)
        val jwt = Jwt.withTokenValue("access-token")
            .header("alg", "HS256")
            .subject(userId.toString())
            .build()

        val response = ServiceCatalogController(catalog)
            .listNearby(jwt, 59.3293, 18.0686, 25.0)

        assertEquals(expected, response)
    }
}
