package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.auth.RefreshTokenRepository
import com.findprofessional.marketplace.auth.IdentityDeletionGateway
import com.findprofessional.marketplace.notification.NotificationDeviceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class AccountDeletionServiceTest {
    private val users = mock(UserAccountRepository::class.java)
    private val tokens = mock(RefreshTokenRepository::class.java)
    private val devices = mock(NotificationDeviceRepository::class.java)
    private val identityDeletion = mock(IdentityDeletionGateway::class.java)
    private val now = Instant.parse("2026-10-10T12:00:00Z")
    private val service = AccountDeletionService(
        users,
        tokens,
        devices,
        identityDeletion,
        Clock.fixed(now, ZoneOffset.UTC)
    )

    @Test
    fun `deletion revokes authentication and anonymizes personal account data`() {
        val user = UserAccount(
            email = "person@example.com",
            phoneNumber = "+46701234567",
            passwordHash = "hash",
            displayName = "Person",
            profileImageUrl = "/uploads/profile.png",
            googleSubject = "google-subject",
            firebaseUid = "firebase-uid",
            phoneVerified = true,
            analyticsConsentGranted = true
        )
        `when`(users.findLockedById(user.id)).thenReturn(Optional.of(user))
        `when`(users.save(user)).thenReturn(user)

        service.delete(user.id)

        verify(tokens).deleteAllByUserId(user.id)
        verify(devices).deleteAllByUserId(user.id)
        verify(identityDeletion).delete("firebase-uid")
        assertNull(user.email)
        assertNull(user.phoneNumber)
        assertNull(user.googleSubject)
        assertNull(user.firebaseUid)
        assertNull(user.profileImageUrl)
        assertFalse(user.phoneVerified)
        assertFalse(user.analyticsConsentGranted)
        assertEquals(emptySet<UserRole>(), user.roles)
        assertEquals("Deleted user", user.displayName)
        assertEquals(now, user.deletedAt)
    }

    @Test
    fun `repeated deletion is idempotent`() {
        val user = UserAccount(displayName = "Deleted user", deletedAt = now)
        `when`(users.findLockedById(user.id)).thenReturn(Optional.of(user))

        service.delete(user.id)

        verify(users).findLockedById(user.id)
    }
}
