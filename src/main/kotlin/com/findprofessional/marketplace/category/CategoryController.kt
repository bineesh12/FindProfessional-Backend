package com.findprofessional.marketplace.category

import com.findprofessional.marketplace.service.MarketplaceServiceResponse
import com.findprofessional.marketplace.service.ServiceCatalogService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService,
    private val serviceCatalog: ServiceCatalogService
) {
    @GetMapping
    fun listCategories(@AuthenticationPrincipal jwt: Jwt): List<CategoryResponse> =
        categoryService.listCategories(UUID.fromString(jwt.subject))

    @GetMapping("/{categoryId}/services")
    fun listServices(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable categoryId: UUID
    ): List<MarketplaceServiceResponse> =
        serviceCatalog.listByCategory(UUID.fromString(jwt.subject), categoryId)
}
