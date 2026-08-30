package com.findprofessional.marketplace.category

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ServiceCategoryRepository : JpaRepository<ServiceCategory, UUID> {
    fun findAllByActiveTrueOrderByDisplayOrderAsc(): List<ServiceCategory>
}
