package com.findprofessional.marketplace.question

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ServiceQuestionRepository : JpaRepository<ServiceQuestion, UUID> {
    fun findAllByServiceIdAndActiveTrueOrderByDisplayOrderAsc(serviceId: UUID): List<ServiceQuestion>
}
