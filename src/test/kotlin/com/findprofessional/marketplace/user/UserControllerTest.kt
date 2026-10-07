package com.findprofessional.marketplace.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

class UserControllerTest {
    @Test
    fun `current profile uses authenticated jwt subject`() {
        val userId = UUID.randomUUID()
        val service = mock(UserService::class.java)
        val response = userResponse(userId)
        `when`(service.getCurrentUser(userId)).thenReturn(response)

        val actual = controller(service).currentUser(jwt(userId))

        assertEquals(response, actual)
    }

    @Test
    fun `profile update uses authenticated jwt subject`() {
        val userId = UUID.randomUUID()
        val service = mock(UserService::class.java)
        val request = UpdateUserProfileRequest("Updated professional")
        val response = userResponse(userId).copy(displayName = request.displayName)
        `when`(service.updateProfile(userId, request)).thenReturn(response)

        val actual = controller(service).updateProfile(jwt(userId), request)

        assertEquals(response, actual)
    }

    @Test
    fun `privacy preferences use authenticated jwt subject`() {
        val userId = UUID.randomUUID()
        val service = mock(UserService::class.java)
        val response = PrivacyPreferencesResponse(analyticsEnabled = false, updatedAt = null)
        `when`(service.getPrivacyPreferences(userId)).thenReturn(response)

        val actual = controller(service).privacyPreferences(jwt(userId))

        assertEquals(response, actual)
    }

    @Test
    fun `privacy update uses authenticated jwt subject`() {
        val userId = UUID.randomUUID()
        val service = mock(UserService::class.java)
        val request = UpdatePrivacyPreferencesRequest(analyticsEnabled = true)
        val response = PrivacyPreferencesResponse(analyticsEnabled = true, updatedAt = null)
        `when`(service.updatePrivacyPreferences(userId, request)).thenReturn(response)

        val actual = controller(service).updatePrivacyPreferences(jwt(userId), request)

        assertEquals(response, actual)
    }

    @Test
    fun `role selection uses authenticated jwt subject`() {
        val userId = UUID.randomUUID()
        val service = mock(UserService::class.java)
        val request = SelectRoleRequest(RoleSelection.PROFESSIONAL)
        val response = userResponse(userId)
        `when`(service.selectRole(userId, request)).thenReturn(response)

        val actual = controller(service).selectRole(jwt(userId), request)

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

    @Test
    fun `profile image upload uses authenticated jwt subject`() {
        val userId = UUID.randomUUID()
        val userService = mock(UserService::class.java)
        val imageService = mock(UserProfileImageService::class.java)
        val file = MockMultipartFile("file", "profile.png", "image/png", byteArrayOf(1))
        val response = userResponse(userId).copy(profileImageUrl = "/uploads/profiles/profile.png")
        `when`(imageService.upload(userId, file)).thenReturn(response)

        val actual = UserController(userService, imageService).uploadProfileImage(jwt(userId), file)

        assertEquals(response, actual)
    }

    private fun controller(service: UserService) =
        UserController(service, mock(UserProfileImageService::class.java))

    private fun jwt(userId: UUID) = Jwt.withTokenValue("access-token")
        .header("alg", "HS256")
        .subject(userId.toString())
        .build()
}
