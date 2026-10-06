package com.findprofessional.marketplace.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

class UserControllerTest {
    @Test
    fun `current profile uses authenticated jwt subject`() {
        val userId = UUID.randomUUID()
        val service = mock(UserService::class.java)
        val response = userResponse(userId)
        `when`(service.getCurrentUser(userId)).thenReturn(response)

        val actual = UserController(service).currentUser(jwt(userId))

        assertEquals(response, actual)
    }

    @Test
    fun `profile update uses authenticated jwt subject`() {
        val userId = UUID.randomUUID()
        val service = mock(UserService::class.java)
        val request = UpdateUserProfileRequest("Updated professional")
        val response = userResponse(userId).copy(displayName = request.displayName)
        `when`(service.updateProfile(userId, request)).thenReturn(response)

        val actual = UserController(service).updateProfile(jwt(userId), request)

        assertEquals(response, actual)
    }

    @Test
    fun `privacy preferences use authenticated jwt subject`() {
        val userId = UUID.randomUUID()
        val service = mock(UserService::class.java)
        val response = PrivacyPreferencesResponse(analyticsEnabled = false, updatedAt = null)
        `when`(service.getPrivacyPreferences(userId)).thenReturn(response)

        val actual = UserController(service).privacyPreferences(jwt(userId))

        assertEquals(response, actual)
    }

    @Test
    fun `privacy update uses authenticated jwt subject`() {
        val userId = UUID.randomUUID()
        val service = mock(UserService::class.java)
        val request = UpdatePrivacyPreferencesRequest(analyticsEnabled = true)
        val response = PrivacyPreferencesResponse(analyticsEnabled = true, updatedAt = null)
        `when`(service.updatePrivacyPreferences(userId, request)).thenReturn(response)

        val actual = UserController(service).updatePrivacyPreferences(jwt(userId), request)

        assertEquals(response, actual)
    }

    @Test
    fun `role selection uses authenticated jwt subject`() {
        val userId = UUID.randomUUID()
        val service = mock(UserService::class.java)
        val request = SelectRoleRequest(RoleSelection.PROFESSIONAL)
        val response = userResponse(userId)
        `when`(service.selectRole(userId, request)).thenReturn(response)

        val actual = UserController(service).selectRole(jwt(userId), request)

        assertEquals(response, actual)
    }

    private fun userResponse(userId: UUID) = UserResponse(
        id = userId,
        email = null,
        phoneNumber = "+46701234567",
        displayName = "Professional",
        roles = setOf(UserRole.PROFESSIONAL),
        phoneVerified = true,
        roleSelectionRequired = false
    )

    private fun jwt(userId: UUID) = Jwt.withTokenValue("access-token")
        .header("alg", "HS256")
        .subject(userId.toString())
        .build()
}
