package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.auth.AuthException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CustomerAuthorizationService(
    private val users: UserAccountRepository
) {
    @Transactional(readOnly = true)
    fun requireCustomer(userId: UUID) {
        val user = users.findById(userId).orElseThrow {
            AuthException("User account was not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        if (UserRole.CUSTOMER !in user.roles) {
            throw AuthException(
                "Customer access is required",
                "CUSTOMER_ROLE_REQUIRED",
                HttpStatus.FORBIDDEN
            )
        }
    }
}
