package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.auth.AuthException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserAccountRepository,
    private val clock: Clock = Clock.systemUTC()
) {
    @Transactional
    fun selectRole(userId: UUID, request: SelectRoleRequest): UserResponse {
        val user = userRepository.findById(userId).orElseThrow {
            AuthException("User account was not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        user.roles.clear()
        user.roles.addAll(request.role.toRoles())
        user.roleSelectedAt = Instant.now(clock)
        return userRepository.save(user).toResponse()
    }
}
