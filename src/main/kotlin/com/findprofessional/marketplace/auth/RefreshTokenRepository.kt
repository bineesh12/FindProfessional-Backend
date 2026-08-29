package com.findprofessional.marketplace.auth

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.util.Optional
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByTokenHash(tokenHash: String): Optional<RefreshToken>
    fun findAllByFamilyIdAndRevokedAtIsNull(familyId: UUID): List<RefreshToken>
}
