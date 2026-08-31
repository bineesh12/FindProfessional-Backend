package com.findprofessional.marketplace.professional

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface PortfolioProjectRepository : JpaRepository<PortfolioProject, UUID> {
    fun findAllByProfessionalUserIdOrderByDisplayOrderAscCreatedAtAsc(professionalUserId: UUID): List<PortfolioProject>
    fun findByIdAndProfessionalUserId(id: UUID, professionalUserId: UUID): Optional<PortfolioProject>
}

interface PortfolioImageRepository : JpaRepository<PortfolioImage, UUID> {
    fun findAllByProjectIdInOrderByDisplayOrderAscCreatedAtAsc(projectIds: Collection<UUID>): List<PortfolioImage>
    fun findByIdAndProjectProfessionalUserId(id: UUID, professionalUserId: UUID): Optional<PortfolioImage>
    fun countByProjectId(projectId: UUID): Long
}
