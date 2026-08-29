package com.findprofessional.marketplace.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

class UserControllerTest {
    @Test
    fun `role selection uses authenticated jwt subject`() {
        val userId = UUID.randomUUID()
        val service = mock(UserService::class.java)
        val request = SelectRoleRequest(RoleSelection.PROFESSIONAL)
        val response = UserResponse(
            id = userId,
            email = null,
            phoneNumber = "+46701234567",
            displayName = "Professional",
            roles = setOf(UserRole.PROFESSIONAL),
            phoneVerified = true,
            roleSelectionRequired = false
        )
        `when`(service.selectRole(userId, request)).thenReturn(response)
        val jwt = Jwt.withTokenValue("access-token")
            .header("alg", "HS256")
            .subject(userId.toString())
            .build()

        val actual = UserController(service).selectRole(jwt, request)

        assertEquals(response, actual)
    }
}
