package com.findprofessional.marketplace.professional

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProfessionalVerificationRepository : JpaRepository<ProfessionalVerification, UUID>
