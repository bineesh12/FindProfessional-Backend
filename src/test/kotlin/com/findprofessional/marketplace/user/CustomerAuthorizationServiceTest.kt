package com.findprofessional.marketplace.user

import com.findprofessional.marketplace.auth.AuthException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class CustomerAuthorizationServiceTest {
    private val users = mock(UserAccountRepository::class.java)
    private val authorization = CustomerAuthorizationService(users)

    @Test
    fun `customer role can access customer catalog`() {
        val user = UserAccount(displayName = "Customer", roles = mutableSetOf(UserRole.CUSTOMER))
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))

        assertDoesNotThrow { authorization.requireCustomer(user.id) }
    }

    @Test
    fun `professional-only user cannot access customer catalog`() {
        val user = UserAccount(displayName = "Professional", roles = mutableSetOf(UserRole.PROFESSIONAL))
        `when`(users.findById(user.id)).thenReturn(Optional.of(user))

        val error = assertThrows(AuthException::class.java) {
            authorization.requireCustomer(user.id)
        }

        assertEquals("CUSTOMER_ROLE_REQUIRED", error.code)
    }

    @Test
    fun `unknown user cannot access customer catalog`() {
        val userId = UUID.randomUUID()
        `when`(users.findById(userId)).thenReturn(Optional.empty())

        val error = assertThrows(AuthException::class.java) {
            authorization.requireCustomer(userId)
        }

        assertEquals("USER_NOT_FOUND", error.code)
    }
}
