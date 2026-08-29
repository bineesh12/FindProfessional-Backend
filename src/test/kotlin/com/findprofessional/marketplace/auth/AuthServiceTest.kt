package com.findprofessional.marketplace.auth

import com.findprofessional.marketplace.user.UserAccount
import com.findprofessional.marketplace.user.UserAccountRepository
import com.findprofessional.marketplace.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional

class AuthServiceTest {
    private lateinit var users: UserAccountRepository
    private lateinit var phoneVerifications: PhoneVerificationRepository
    private lateinit var refreshTokens: RefreshTokenRepository
    private lateinit var service: AuthService

    private val savedUsers = mutableListOf<UserAccount>()
    private val savedVerifications = mutableListOf<PhoneVerification>()
    private val savedRefreshTokens = mutableListOf<RefreshToken>()
    private val clock = Clock.fixed(Instant.parse("2026-08-19T12:00:00Z"), ZoneOffset.UTC)

    @BeforeEach
    fun setUp() {
        users = mock(UserAccountRepository::class.java)
        phoneVerifications = mock(PhoneVerificationRepository::class.java)
        refreshTokens = mock(RefreshTokenRepository::class.java)

        `when`(users.existsByEmail(anyValue())).thenAnswer { invocation ->
            savedUsers.any { it.email == invocation.arguments[0] }
        }
        `when`(users.existsByPhoneNumber(anyValue())).thenAnswer { invocation ->
            savedUsers.any { it.phoneNumber == invocation.arguments[0] }
        }
        `when`(users.findByEmail(anyValue())).thenAnswer { invocation ->
            Optional.ofNullable(savedUsers.firstOrNull { it.email == invocation.arguments[0] })
        }
        `when`(users.findByPhoneNumber(anyValue())).thenAnswer { invocation ->
            Optional.ofNullable(savedUsers.firstOrNull { it.phoneNumber == invocation.arguments[0] })
        }
        `when`(users.findByGoogleSubject(anyValue())).thenAnswer { invocation ->
            Optional.ofNullable(savedUsers.firstOrNull { it.googleSubject == invocation.arguments[0] })
        }
        `when`(users.findByFirebaseUid(anyValue())).thenAnswer { invocation ->
            Optional.ofNullable(savedUsers.firstOrNull { it.firebaseUid == invocation.arguments[0] })
        }
        `when`(users.save(anyValue())).thenAnswer { invocation ->
            val user = invocation.arguments[0] as UserAccount
            savedUsers.removeIf { it.id == user.id }
            savedUsers.add(user)
            user
        }

        `when`(phoneVerifications.save(anyValue())).thenAnswer { invocation ->
            val verification = invocation.arguments[0] as PhoneVerification
            savedVerifications.add(verification)
            verification
        }
        `when`(
            phoneVerifications.findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(anyValue())
        ).thenAnswer { invocation ->
            Optional.ofNullable(
                savedVerifications
                    .filter { it.phoneNumber == invocation.arguments[0] && it.consumedAt == null }
                    .maxByOrNull { it.createdAt }
            )
        }

        `when`(refreshTokens.save(anyValue())).thenAnswer { invocation ->
            val token = invocation.arguments[0] as RefreshToken
            savedRefreshTokens.removeIf { it.id == token.id }
            savedRefreshTokens.add(token)
            token
        }
        `when`(refreshTokens.saveAll(anyValue<Iterable<RefreshToken>>())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val tokens = invocation.arguments[0] as Iterable<RefreshToken>
            tokens.forEach { token ->
                savedRefreshTokens.removeIf { it.id == token.id }
                savedRefreshTokens.add(token)
            }
            tokens
        }
        `when`(refreshTokens.findByTokenHash(anyValue())).thenAnswer { invocation ->
            Optional.ofNullable(savedRefreshTokens.firstOrNull { it.tokenHash == invocation.arguments[0] })
        }
        `when`(refreshTokens.findAllByFamilyIdAndRevokedAtIsNull(anyValue())).thenAnswer { invocation ->
            savedRefreshTokens.filter { it.familyId == invocation.arguments[0] && it.revokedAt == null }
        }

        val authProperties = AuthProperties(exposePhoneCode = true)
        val refreshTokenService = RefreshTokenService(refreshTokens, authProperties, clock)

        service = AuthService(
            userRepository = users,
            phoneVerificationRepository = phoneVerifications,
            passwordEncoder = BCryptPasswordEncoder(),
            jwtTokenService = StubJwtTokenService(),
            refreshTokenService = refreshTokenService,
            googleIdentityVerifier = StubGoogleIdentityVerifier(),
            firebaseIdentityVerifier = StubFirebaseIdentityVerifier(),
            authProperties = authProperties,
            clock = clock
        )
    }

    @Test
    fun `register creates customer with normalized email`() {
        val response = service.register(
            RegisterRequest(
                email = "USER@example.COM",
                password = "strong-password",
                displayName = "A Customer",
                phoneNumber = "+46701234567"
            )
        )

        assertEquals("user@example.com", response.user.email)
        assertEquals("+46701234567", response.user.phoneNumber)
        assertEquals(setOf(UserRole.CUSTOMER), response.user.roles)
        assertTrue(response.user.roleSelectionRequired)
        assertNotNull(response.accessToken)
        assertTrue(response.refreshToken.isNotBlank())
        assertEquals(900, response.expiresInSeconds)
    }

    @Test
    fun `register rejects duplicate email`() {
        service.register(RegisterRequest("user@example.com", "strong-password", "A Customer"))

        assertThrows(AuthException::class.java) {
            service.register(RegisterRequest("USER@example.com", "strong-password", "Other Customer"))
        }
    }

    @Test
    fun `login rejects incorrect password`() {
        service.register(RegisterRequest("user@example.com", "strong-password", "A Customer"))

        assertThrows(AuthException::class.java) {
            service.login(LoginRequest("user@example.com", "wrong-password"))
        }
    }

    @Test
    fun `phone verification creates verified phone user`() {
        val codeResponse = service.requestPhoneCode(PhoneCodeRequest("+46701234567"))

        val response = service.verifyPhone(
            VerifyPhoneRequest(
                phoneNumber = "+46701234567",
                code = requireNotNull(codeResponse.devCode),
                displayName = "Phone Customer"
            )
        )

        assertEquals("+46701234567", response.user.phoneNumber)
        assertEquals(true, response.user.phoneVerified)
    }

    @Test
    fun `google login creates customer and returns token pair`() {
        val response = service.loginWithGoogle(GoogleLoginRequest("valid-google-id-token"))

        assertEquals("google@example.com", response.user.email)
        assertEquals(setOf(UserRole.CUSTOMER), response.user.roles)
        assertTrue(response.user.roleSelectionRequired)
        assertTrue(response.accessToken.isNotBlank())
        assertTrue(response.refreshToken.isNotBlank())
    }

    @Test
    fun `firebase login creates user and links subsequent login by uid`() {
        val first = service.loginWithFirebase(FirebaseLoginRequest("valid-firebase-id-token"))
        val second = service.loginWithFirebase(FirebaseLoginRequest("valid-firebase-id-token"))

        assertEquals("firebase-user-id", savedUsers.single().firebaseUid)
        assertEquals("firebase@example.com", first.user.email)
        assertEquals(first.user.id, second.user.id)
        assertEquals(setOf(UserRole.CUSTOMER), first.user.roles)
    }

    @Test
    fun `firebase phone login creates verified user and backend token pair`() {
        val response = service.loginWithFirebase(FirebaseLoginRequest("firebase-phone-id-token"))

        assertEquals("firebase-phone-user-id", savedUsers.single().firebaseUid)
        assertEquals("+46701234567", response.user.phoneNumber)
        assertEquals(true, response.user.phoneVerified)
        assertTrue(response.accessToken.isNotBlank())
        assertTrue(response.refreshToken.isNotBlank())
    }

    @Test
    fun `refresh rotates token and reuse revokes family`() {
        val login = service.register(RegisterRequest("user@example.com", "strong-password", "A Customer"))

        val refreshed = service.refresh(RefreshTokenRequest(login.refreshToken))

        assertNotEquals(login.refreshToken, refreshed.refreshToken)
        assertThrows(AuthException::class.java) {
            service.refresh(RefreshTokenRequest(login.refreshToken))
        }
        assertTrue(savedRefreshTokens.all { it.revokedAt != null })
    }

    @Test
    fun `logout revokes refresh token and remains idempotent`() {
        val login = service.register(RegisterRequest("user@example.com", "strong-password", "A Customer"))

        service.logout(LogoutRequest(login.refreshToken))
        service.logout(LogoutRequest(login.refreshToken))

        assertNotNull(savedRefreshTokens.single().revokedAt)
    }
}

private class StubJwtTokenService : JwtTokenService(
    jwtEncoder = mock(JwtEncoder::class.java),
    authProperties = AuthProperties()
) {
    override fun createAccessToken(user: UserAccount): String = "test-token-${user.id}"
}

private class StubGoogleIdentityVerifier : GoogleIdentityVerifier {
    override fun verify(idToken: String): GoogleIdentity =
        GoogleIdentity(subject = "google-subject", email = "google@example.com", displayName = "Google User")
}

private class StubFirebaseIdentityVerifier : FirebaseIdentityVerifier {
    override fun verify(idToken: String): FirebaseIdentity =
        if (idToken == "firebase-phone-id-token") {
            FirebaseIdentity(
                uid = "firebase-phone-user-id",
                email = null,
                emailVerified = false,
                phoneNumber = "+46701234567",
                displayName = null
            )
        } else {
            FirebaseIdentity(
                uid = "firebase-user-id",
                email = "firebase@example.com",
                emailVerified = true,
                phoneNumber = null,
                displayName = "Firebase User"
            )
        }
}
