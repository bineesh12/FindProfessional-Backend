package com.findprofessional.marketplace.professional

import com.findprofessional.marketplace.auth.AuthException
import com.findprofessional.marketplace.user.UserAccount
import com.findprofessional.marketplace.user.UserAccountRepository
import com.findprofessional.marketplace.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class ProfessionalAuthorizationServiceTest {
    private val users = mock(UserAccountRepository::class.java)
    private val authorization = ProfessionalAuthorizationService(users)

    @Test
    fun `professional role grants access`() {
        val user = UserAccount(
            displayName = "Professional",
            roles = mutableSetOf(UserRole.PROFESSIONAL)
        )
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))

        assertEquals(user, authorization.requireProfessional(user.id))
    }

    @Test
    fun `customer role cannot access professional profile`() {
        val user = UserAccount(displayName = "Customer")
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))

        val error = assertThrows(AuthException::class.java) {
            authorization.requireProfessional(user.id)
        }

        assertEquals("PROFESSIONAL_ROLE_REQUIRED", error.code)
    }
}
