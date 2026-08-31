package com.findprofessional.marketplace.professional

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProfessionalServiceOfferingRepository : JpaRepository<ProfessionalServiceOffering, UUID> {
    fun findAllByProfessionalUserIdOrderByDisplayOrderAsc(professionalUserId: UUID): List<ProfessionalServiceOffering>
    fun deleteAllByProfessionalUserId(professionalUserId: UUID)
}
