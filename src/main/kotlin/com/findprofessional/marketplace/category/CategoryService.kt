package com.findprofessional.marketplace.category

import com.findprofessional.marketplace.user.CustomerAuthorizationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CategoryService(
    private val categories: ServiceCategoryRepository,
    private val authorization: CustomerAuthorizationService
) {
    @Transactional(readOnly = true)
    fun listCategories(userId: UUID): List<CategoryResponse> {
        authorization.requireCustomer(userId)
        return categories.findAllByActiveTrueOrderByDisplayOrderAsc().map(ServiceCategory::toResponse)
    }
}
