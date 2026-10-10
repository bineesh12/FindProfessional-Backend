package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.auth.AuthException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class UserServiceTest {
    private val users = mock(UserAccountRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-25T12:00:00Z"), ZoneOffset.UTC)
    private val service = UserService(users, clock)

    @Test
    fun `current user returns authenticated account`() {
        val user = UserAccount(displayName = "Bineesh", email = "bineesh@example.com")
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))

        val response = service.getCurrentUser(user.id)

        assertEquals(user.id, response.id)
        assertEquals("Bineesh", response.displayName)
        assertEquals("bineesh@example.com", response.email)
    }

    @Test
    fun `profile update trims and persists display name`() {
        val user = UserAccount(displayName = "Old name")
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))
        `when`(users.save(user)).thenReturn(user)

        val response = service.updateProfile(user.id, UpdateUserProfileRequest("  New name  "))

        assertEquals("New name", response.displayName)
        assertEquals("New name", user.displayName)
        assertEquals(Instant.parse("2026-08-25T12:00:00Z"), user.updatedAt)
    }

    @Test
    fun `profile update rejects a blank display name`() {
        val error = assertThrows(AuthException::class.java) {
            service.updateProfile(UUID.randomUUID(), UpdateUserProfileRequest("   "))
        }

        assertEquals("VALIDATION_ERROR", error.code)
        assertTrue(error.message!!.contains("Display name"))
    }

    @Test
    fun `privacy preferences default analytics consent to disabled`() {
        val user = UserAccount(displayName = "User")
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))

        val response = service.getPrivacyPreferences(user.id)

        assertFalse(response.analyticsEnabled)
        assertEquals(null, response.updatedAt)
    }

    @Test
    fun `privacy update records analytics consent choice`() {
        val user = UserAccount(displayName = "User")
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))
        `when`(users.save(user)).thenReturn(user)

        val response = service.updatePrivacyPreferences(
            user.id,
            UpdatePrivacyPreferencesRequest(analyticsEnabled = true)
        )

        assertTrue(response.analyticsEnabled)
        assertEquals(Instant.parse("2026-08-25T12:00:00Z"), response.updatedAt)
        assertTrue(user.analyticsConsentGranted)
    }

    @Test
    fun `both selection replaces initial role and completes onboarding`() {
        val user = UserAccount(displayName = "User")
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))
        `when`(users.save(user)).thenReturn(user)

        val response = service.selectRole(user.id, SelectRoleRequest(RoleSelection.BOTH))

        assertEquals(setOf(UserRole.CUSTOMER, UserRole.PROFESSIONAL), response.roles)
        assertEquals(Instant.parse("2026-08-25T12:00:00Z"), user.roleSelectedAt)
        assertFalse(response.roleSelectionRequired)
    }

    @Test
    fun `selection cannot update an unknown user`() {
        val userId = UUID.randomUUID()
        `when`(users.findById(userId)).thenReturn(Optional.empty())

        assertThrows(AuthException::class.java) {
            service.selectRole(userId, SelectRoleRequest(RoleSelection.PROFESSIONAL))
        }
    }

    @Test
    fun `locale update normalizes a regional language tag`() {
        val user = UserAccount(displayName = "User")
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))
        `when`(users.save(user)).thenReturn(user)

        val response = service.updateLocale(user.id, UpdateLocaleRequest("sv-SE"))

        assertEquals("sv", response.languageTag)
        assertEquals("sv", user.preferredLocale)
    }

    @Test
    fun `unsupported locale falls back to English`() {
        val user = UserAccount(displayName = "User", preferredLocale = "sv")
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))
        `when`(users.save(user)).thenReturn(user)

        assertEquals("en", service.updateLocale(user.id, UpdateLocaleRequest("ja-JP")).languageTag)
    }
}
