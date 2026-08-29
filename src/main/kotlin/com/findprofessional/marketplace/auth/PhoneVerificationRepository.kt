package com.findprofessional.marketplace.auth

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface PhoneVerificationRepository : JpaRepository<PhoneVerification, UUID> {
    fun findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(phoneNumber: String): Optional<PhoneVerification>
}
