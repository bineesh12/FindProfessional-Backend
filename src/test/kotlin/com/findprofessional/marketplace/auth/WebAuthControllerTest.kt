package com.findprofessional.marketplace.auth

import com.findprofessional.marketplace.common.ApiExceptionHandler
import com.findprofessional.marketplace.user.UserResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockCookie
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID

class WebAuthControllerTest {
    private lateinit var authService: AuthService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        authService = mock(AuthService::class.java)
        val properties = AuthProperties(
            jwtSecret = "a".repeat(32),
            refreshTokenDays = 30,
            webCookieSecure = true
        )
        mockMvc = MockMvcBuilders
            .standaloneSetup(WebAuthController(authService, properties))
            .setControllerAdvice(ApiExceptionHandler())
            .build()
    }

    @Test
    fun `firebase login keeps refresh token out of response and sets secure cookie`() {
        `when`(authService.loginWithFirebase(anyValue())).thenReturn(authResponse())

        mockMvc.perform(
            post("/api/web-auth/firebase")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"idToken":"firebase-id-token"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").doesNotExist())
            .andExpect(cookie().httpOnly("arbio_refresh", true))
            .andExpect(cookie().secure("arbio_refresh", true))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("SameSite=Lax")))
    }

    @Test
    fun `refresh rotates cookie token`() {
        `when`(authService.refresh(anyValue())).thenReturn(authResponse())

        mockMvc.perform(
            post("/api/web-auth/refresh")
                .cookie(MockCookie("arbio_refresh", "old-refresh-token"))
        )
            .andExpect(status().isOk)
            .andExpect(cookie().value("arbio_refresh", "refresh-token"))
    }

    @Test
    fun `refresh rejects a missing cookie`() {
        mockMvc.perform(post("/api/web-auth/refresh"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Authentication session is unavailable"))
    }

    @Test
    fun `logout revokes cookie token and clears cookie`() {
        mockMvc.perform(
            post("/api/web-auth/logout")
                .cookie(MockCookie("arbio_refresh", "refresh-token"))
        )
            .andExpect(status().isNoContent)
            .andExpect(cookie().maxAge("arbio_refresh", 0))

        verify(authService).logout(LogoutRequest("refresh-token"))
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
            roles = emptySet(),
            phoneVerified = false,
            roleSelectionRequired = true
        )
    )
}
