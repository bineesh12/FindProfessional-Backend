package com.findprofessional.marketplace.professional

import com.findprofessional.marketplace.auth.AuthException
import com.findprofessional.marketplace.user.UserAccount
import com.findprofessional.marketplace.user.UserAccountRepository
import com.findprofessional.marketplace.user.UserRole
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProfessionalAuthorizationService(
    private val users: UserAccountRepository
) {
    @Transactional(readOnly = true)
    fun requireProfessional(userId: UUID): UserAccount {
        val user = users.findById(userId).orElseThrow {
            AuthException("User account was not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        if (UserRole.PROFESSIONAL !in user.roles) {
            throw AuthException(
                "Professional access is required",
                "PROFESSIONAL_ROLE_REQUIRED",
                HttpStatus.FORBIDDEN
            )
        }
        return user
    }
}
