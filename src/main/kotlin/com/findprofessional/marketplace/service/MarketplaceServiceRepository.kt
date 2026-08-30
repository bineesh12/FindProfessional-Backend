package com.findprofessional.marketplace.service

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MarketplaceServiceRepository : JpaRepository<MarketplaceService, UUID> {
    fun findAllByCategoryIdAndActiveTrueOrderByNameAsc(categoryId: UUID): List<MarketplaceService>

    @Query(
        """
        SELECT service FROM MarketplaceService service
        WHERE service.active = true
          AND (
            LOWER(service.name) LIKE :query
            OR LOWER(service.shortDescription) LIKE :query
            OR LOWER(service.searchKeywords) LIKE :query
            OR LOWER(service.category.name) LIKE :query
          )
        ORDER BY service.name ASC
        """
    )
    fun search(@Param("query") query: String): List<MarketplaceService>
}
