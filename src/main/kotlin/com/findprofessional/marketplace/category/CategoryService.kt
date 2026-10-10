package com.findprofessional.marketplace.category

import com.findprofessional.marketplace.user.CustomerAuthorizationService
import com.findprofessional.marketplace.localization.CatalogLocalizationService
import com.findprofessional.marketplace.localization.RequestLocaleResolver
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CategoryService(
    private val categories: ServiceCategoryRepository,
    private val authorization: CustomerAuthorizationService,
    private val localization: CatalogLocalizationService,
    private val locale: RequestLocaleResolver
) {
    @Transactional(readOnly = true)
    fun listCategories(userId: UUID): List<CategoryResponse> {
        authorization.requireCustomer(userId)
        return localization.categories(categories.findAllByActiveTrueOrderByDisplayOrderAsc(), locale.current())
    }
}
