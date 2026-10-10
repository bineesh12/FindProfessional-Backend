package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.auth.AuthException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID
import com.findprofessional.marketplace.localization.SupportedLocale

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

    @Transactional(readOnly = true)
    fun getPrivacyPreferences(userId: UUID): PrivacyPreferencesResponse =
        requireUser(userId).toPrivacyPreferencesResponse()

    @Transactional
    fun updatePrivacyPreferences(
        userId: UUID,
        request: UpdatePrivacyPreferencesRequest
    ): PrivacyPreferencesResponse {
        val user = requireUser(userId)
        if (user.analyticsConsentGranted != request.analyticsEnabled) {
            user.analyticsConsentGranted = request.analyticsEnabled
            user.analyticsConsentUpdatedAt = Instant.now(clock)
            userRepository.save(user)
        }
        return user.toPrivacyPreferencesResponse()
    }

    @Transactional
    fun selectRole(userId: UUID, request: SelectRoleRequest): UserResponse {
        val user = requireUser(userId)
        user.roles.clear()
        user.roles.addAll(request.role.toRoles())
        user.roleSelectedAt = Instant.now(clock)
        return userRepository.save(user).toResponse()
    }

    @Transactional
    fun updateLocale(userId: UUID, request: UpdateLocaleRequest): LocaleResponse {
        val user = requireUser(userId)
        val normalized = SupportedLocale.normalize(request.languageTag)
        if (user.preferredLocale != normalized) {
            user.preferredLocale = normalized
            userRepository.save(user)
        }
        return LocaleResponse(normalized)
    }

    private fun requireUser(userId: UUID): UserAccount =
        userRepository.findById(userId).filter { it.deletedAt == null }.orElseThrow {
            AuthException("User account was not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
}

private fun UserAccount.toPrivacyPreferencesResponse() = PrivacyPreferencesResponse(
    analyticsEnabled = analyticsConsentGranted,
    updatedAt = analyticsConsentUpdatedAt
)
