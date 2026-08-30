package com.findprofessional.marketplace.category

import com.findprofessional.marketplace.user.CustomerAuthorizationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class CategoryServiceTest {
    private val categories = mock(ServiceCategoryRepository::class.java)
    private val authorization = mock(CustomerAuthorizationService::class.java)
    private val service = CategoryService(categories, authorization)

    @Test
    fun `categories are returned in repository display order`() {
        val userId = UUID.randomUUID()
        val home = category("HOME", "Home", "home", 1)
        val vehicle = category("VEHICLE", "Vehicle", "directions_car", 2)
        `when`(categories.findAllByActiveTrueOrderByDisplayOrderAsc())
            .thenReturn(listOf(home, vehicle))

        val response = service.listCategories(userId)

        verify(authorization).requireCustomer(userId)
        assertEquals(listOf("HOME", "VEHICLE"), response.map(CategoryResponse::code))
        assertEquals("directions_car", response.last().iconKey)
    }
}

internal fun category(
    code: String = "HOME",
    name: String = "Home",
    iconKey: String = "home",
    displayOrder: Int = 1,
    active: Boolean = true
) = ServiceCategory(
    id = UUID.randomUUID(),
    code = code,
    name = name,
    iconKey = iconKey,
    displayOrder = displayOrder,
    active = active
)
