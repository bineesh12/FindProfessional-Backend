package com.findprofessional.marketplace.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserAccountRepository : JpaRepository<UserAccount, UUID> {
    fun existsByEmail(email: String): Boolean
    fun existsByPhoneNumber(phoneNumber: String): Boolean
    fun findByEmail(email: String): Optional<UserAccount>
    fun findByPhoneNumber(phoneNumber: String): Optional<UserAccount>
    fun findByGoogleSubject(googleSubject: String): Optional<UserAccount>
    fun findByFirebaseUid(firebaseUid: String): Optional<UserAccount>
}
