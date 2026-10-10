package com.findprofessional.marketplace.service

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID
import java.util.Optional

interface MarketplaceServiceRepository : JpaRepository<MarketplaceService, UUID> {
    fun findAllByCategoryIdAndActiveTrueOrderByNameAsc(categoryId: UUID): List<MarketplaceService>
    fun findByCodeAndActiveTrue(code: String): Optional<MarketplaceService>

    @Query(
        """
        SELECT service FROM MarketplaceService service
        WHERE service.active = true
        ORDER BY service.category.displayOrder ASC, service.name ASC
        """
    )
    fun findAllActiveForProfessionalSetup(): List<MarketplaceService>

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

    @Query(
        """
        SELECT DISTINCT service FROM MarketplaceService service
        LEFT JOIN MarketplaceServiceTranslation translation
          ON translation.entityId = service.id AND translation.locale = :locale
        LEFT JOIN ServiceCategoryTranslation categoryTranslation
          ON categoryTranslation.entityId = service.category.id AND categoryTranslation.locale = :locale
        WHERE service.active = true
          AND (
            LOWER(COALESCE(translation.name, service.name)) LIKE :query
            OR LOWER(COALESCE(translation.shortDescription, service.shortDescription)) LIKE :query
            OR LOWER(COALESCE(translation.searchKeywords, service.searchKeywords)) LIKE :query
            OR LOWER(COALESCE(categoryTranslation.name, service.category.name)) LIKE :query
          )
        ORDER BY service.name ASC
        """
    )
    fun searchLocalized(@Param("query") query: String, @Param("locale") locale: String): List<MarketplaceService>
}

interface ServiceAliasRepository : JpaRepository<ServiceAlias, UUID> {
    fun findAllByServiceIdInAndActiveTrue(serviceIds: Collection<UUID>): List<ServiceAlias>
}
