package com.findprofessional.marketplace.auth

import com.findprofessional.marketplace.common.ApiExceptionHandler
import com.findprofessional.marketplace.user.UserRole
import com.findprofessional.marketplace.user.UserResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID

class AuthControllerTest {
    private lateinit var authService: AuthService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        authService = mock(AuthService::class.java)
        mockMvc = MockMvcBuilders.standaloneSetup(AuthController(authService))
            .setControllerAdvice(ApiExceptionHandler())
            .build()
    }

    @Test
    fun `google login returns access and refresh tokens`() {
        `when`(authService.loginWithGoogle(anyValue())).thenReturn(authResponse())

        mockMvc.perform(
            post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"idToken":"google-id-token"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.expiresInSeconds").value(900))
    }

    @Test
    fun `firebase login exchanges identity token for backend session`() {
        `when`(authService.loginWithFirebase(anyValue())).thenReturn(authResponse())

        mockMvc.perform(
            post("/api/auth/firebase")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"idToken":"firebase-id-token"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
    }

    @Test
    fun `refresh rejects blank token`() {
        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `refresh returns rotated token pair`() {
        `when`(authService.refresh(anyValue())).thenReturn(authResponse())

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"previous-refresh-token"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
    }

    @Test
    fun `logout returns no content`() {
        doNothing().`when`(authService).logout(anyValue())

        mockMvc.perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"refresh-token"}""")
        ).andExpect(status().isNoContent)
    }

    private fun authResponse() = AuthResponse(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresInSeconds = 900,
        user = UserResponse(
            id = UUID.randomUUID(),
            email = "user@example.com",
            phoneNumber = null,
            displayName = "User",
            roles = setOf(UserRole.CUSTOMER),
            phoneVerified = false,
            roleSelectionRequired = true
        )
    )
}
