package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.auth.AuthException
import com.findprofessional.marketplace.auth.RefreshTokenRepository
import com.findprofessional.marketplace.auth.IdentityDeletionGateway
import com.findprofessional.marketplace.notification.NotificationDeviceRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class AccountDeletionService(
    private val users: UserAccountRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val notificationDevices: NotificationDeviceRepository,
    private val identityDeletion: IdentityDeletionGateway,
    private val clock: Clock = Clock.systemUTC()
) {
    @Transactional
    fun delete(userId: UUID) {
        val user = users.findLockedById(userId).orElseThrow {
            AuthException("User account was not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        if (user.deletedAt != null) return

        identityDeletion.delete(user.firebaseUid)
        refreshTokens.deleteAllByUserId(userId)
        notificationDevices.deleteAllByUserId(userId)
        user.email = null
        user.phoneNumber = null
        user.passwordHash = null
        user.googleSubject = null
        user.firebaseUid = null
        user.phoneVerified = false
        user.roles.clear()
        user.profileImageUrl = null
        user.displayName = "Deleted user"
        user.analyticsConsentGranted = false
        user.analyticsConsentUpdatedAt = null
        user.deletedAt = Instant.now(clock)
        users.save(user)
    }
}
