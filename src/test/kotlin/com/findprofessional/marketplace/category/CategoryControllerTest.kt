package com.findprofessional.marketplace.category

import com.findprofessional.marketplace.service.ServiceCatalogService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

class CategoryControllerTest {
    @Test
    fun `list categories uses authenticated user subject`() {
        val userId = UUID.randomUUID()
        val categories = mock(CategoryService::class.java)
        val serviceCatalog = mock(ServiceCatalogService::class.java)
        val expected = listOf(category().toResponse())
        `when`(categories.listCategories(userId)).thenReturn(expected)
        val jwt = Jwt.withTokenValue("access-token")
            .header("alg", "HS256")
            .subject(userId.toString())
            .build()

        val response = CategoryController(categories, serviceCatalog).listCategories(jwt)

        assertEquals(expected, response)
    }
}
