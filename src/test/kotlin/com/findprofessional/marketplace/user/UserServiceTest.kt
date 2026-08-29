package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.auth.AuthException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
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
}
