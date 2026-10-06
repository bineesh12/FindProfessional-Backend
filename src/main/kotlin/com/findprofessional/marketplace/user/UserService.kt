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
    @Transactional(readOnly = true)
    fun getCurrentUser(userId: UUID): UserResponse = requireUser(userId).toResponse()

    @Transactional
    fun updateProfile(userId: UUID, request: UpdateUserProfileRequest): UserResponse {
        val displayName = request.displayName.trim()
        if (displayName.length !in 2..100) {
            throw AuthException(
                "Display name must contain between 2 and 100 characters",
                "VALIDATION_ERROR",
                HttpStatus.BAD_REQUEST
            )
        }

        val user = requireUser(userId)
        user.displayName = displayName
        user.updatedAt = Instant.now(clock)
        return userRepository.save(user).toResponse()
    }

    @Transactional
    fun selectRole(userId: UUID, request: SelectRoleRequest): UserResponse {
        val user = requireUser(userId)
        user.roles.clear()
        user.roles.addAll(request.role.toRoles())
        user.roleSelectedAt = Instant.now(clock)
        return userRepository.save(user).toResponse()
    }

    private fun requireUser(userId: UUID): UserAccount =
        userRepository.findById(userId).orElseThrow {
            AuthException("User account was not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
}
