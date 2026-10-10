package com.findprofessional.marketplace.auth

import com.findprofessional.marketplace.user.UserAccountRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant
import java.util.UUID

class ActiveAccountFilterTest {
    private val users = mock(UserAccountRepository::class.java)
    private val filter = ActiveAccountFilter(users)

    @AfterEach
    fun clearSecurityContext() = SecurityContextHolder.clearContext()

    @Test
    fun `deleted account is rejected even while jwt is valid`() {
        val userId = UUID.randomUUID()
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt(userId))
        `when`(users.existsByIdAndDeletedAtIsNull(userId)).thenReturn(false)
        val response = MockHttpServletResponse()

        filter.doFilter(MockHttpServletRequest(), response, MockFilterChain())

        assertEquals(401, response.status)
    }

    @Test
    fun `active account continues through the filter chain`() {
        val userId = UUID.randomUUID()
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt(userId))
        `when`(users.existsByIdAndDeletedAtIsNull(userId)).thenReturn(true)
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(MockHttpServletRequest(), response, chain)

        assertEquals(200, response.status)
        assertEquals(true, chain.request != null)
    }

    private fun jwt(userId: UUID): Jwt = Jwt(
        "token",
        Instant.now(),
        Instant.now().plusSeconds(60),
        mapOf("alg" to "none"),
        mapOf("sub" to userId.toString())
    )
}
