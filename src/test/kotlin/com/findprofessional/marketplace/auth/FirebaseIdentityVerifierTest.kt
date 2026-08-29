package com.findprofessional.marketplace.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtException
import java.time.Instant

class FirebaseIdentityVerifierTest {
    @Test
    fun `maps verified Firebase claims to identity`() {
        val token = Jwt(
            "firebase-token",
            Instant.parse("2026-08-29T08:00:00Z"),
            Instant.parse("2026-08-29T09:00:00Z"),
            mapOf("alg" to "RS256"),
            mapOf(
                "sub" to "firebase-user-id",
                "email" to "user@example.com",
                "email_verified" to true,
                "phone_number" to "+46701234567",
                "name" to "Firebase User"
            )
        )
        val verifier = FirebaseJwtIdentityVerifier(FirebaseTokenDecoder { token })

        val identity = verifier.verify("firebase-token")

        assertEquals("firebase-user-id", identity.uid)
        assertEquals("user@example.com", identity.email)
        assertEquals(true, identity.emailVerified)
        assertEquals("+46701234567", identity.phoneNumber)
        assertEquals("Firebase User", identity.displayName)
    }

    @Test
    fun `rejects token decoding failures with authentication error`() {
        val verifier = FirebaseJwtIdentityVerifier(
            FirebaseTokenDecoder { throw JwtException("invalid") }
        )

        val error = assertThrows(AuthException::class.java) {
            verifier.verify("invalid-token")
        }

        assertEquals("INVALID_FIREBASE_TOKEN", error.code)
        assertEquals(HttpStatus.UNAUTHORIZED, error.status)
    }
}
