package com.findprofessional.marketplace.auth

import com.findprofessional.marketplace.user.UserAccount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.JwtException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class JwtTokenServiceTest {
    private val issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS)
    private val clock = Clock.fixed(issuedAt, ZoneOffset.UTC)
    private val properties = AuthProperties(
        issuer = "find-professional-test",
        jwtSecret = "test-signing-secret-with-at-least-32-bytes",
        accessTokenMinutes = 15
    )
    private val securityConfig = SecurityConfig(properties)

    @Test
    fun `access token contains stable identity issuer and expiry`() {
        val user = UserAccount(
            displayName = "Professional",
            email = "professional@example.com"
        )
        val service = JwtTokenService(securityConfig.jwtEncoder(), properties, clock)

        val jwt = securityConfig.jwtDecoder().decode(service.createAccessToken(user))

        assertEquals(user.id.toString(), jwt.subject)
        assertEquals("find-professional-test", jwt.getClaimAsString("iss"))
        assertEquals(null, jwt.claims["roles"])
        assertEquals("access", jwt.getClaimAsString("token_type"))
        assertEquals(issuedAt.plusSeconds(900), jwt.expiresAt)
    }

    @Test
    fun `decoder rejects token from another issuer`() {
        val otherProperties = properties.copy(issuer = "another-issuer")
        val token = JwtTokenService(SecurityConfig(otherProperties).jwtEncoder(), otherProperties, clock)
            .createAccessToken(UserAccount(displayName = "User", email = "user@example.com"))

        assertThrows(JwtException::class.java) {
            securityConfig.jwtDecoder().decode(token)
        }
    }
}
