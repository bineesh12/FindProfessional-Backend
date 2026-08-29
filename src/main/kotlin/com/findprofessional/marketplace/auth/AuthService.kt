package com.findprofessional.marketplace.auth

import com.findprofessional.marketplace.user.UserAccount
import com.findprofessional.marketplace.user.UserAccountRepository
import com.findprofessional.marketplace.user.UserRole
import com.findprofessional.marketplace.user.toResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserAccountRepository,
    private val phoneVerificationRepository: PhoneVerificationRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val googleIdentityVerifier: GoogleIdentityVerifier,
    private val firebaseIdentityVerifier: FirebaseIdentityVerifier,
    private val authProperties: AuthProperties,
    private val clock: Clock = Clock.systemUTC()
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val email = normalizeEmail(request.email)
        val phone = request.phoneNumber?.let(::normalizePhone)
        if (userRepository.existsByEmail(email)) {
            throw AuthException("Email is already registered")
        }
        if (phone != null && userRepository.existsByPhoneNumber(phone)) {
            throw AuthException("Phone number is already registered")
        }

        val user = userRepository.save(
            UserAccount(
                email = email,
                phoneNumber = phone,
                passwordHash = passwordEncoder.encode(request.password),
                displayName = request.displayName.trim(),
                roles = mutableSetOf(UserRole.CUSTOMER)
            )
        )
        return authResponse(user)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(normalizeEmail(request.email))
            .orElseThrow { AuthException("Invalid email or password") }

        val passwordHash = user.passwordHash ?: throw AuthException("Invalid email or password")
        if (!passwordEncoder.matches(request.password, passwordHash)) {
            throw AuthException("Invalid email or password")
        }

        return authResponse(user)
    }

    @Transactional
    fun loginWithGoogle(request: GoogleLoginRequest): AuthResponse {
        val identity = googleIdentityVerifier.verify(request.idToken)
        val user = userRepository.findByGoogleSubject(identity.subject).orElseGet {
            val email = normalizeEmail(identity.email)
            userRepository.findByEmail(email).orElse(null)?.also {
                it.googleSubject = identity.subject
                return@orElseGet it
            }

            UserAccount(
                email = email,
                displayName = identity.displayName?.trim().takeUnless { it.isNullOrBlank() } ?: "Google user",
                googleSubject = identity.subject,
                roles = mutableSetOf(UserRole.CUSTOMER)
            )
        }

        return authResponse(userRepository.save(user))
    }

    @Transactional
    fun loginWithFirebase(request: FirebaseLoginRequest): AuthResponse {
        val identity = firebaseIdentityVerifier.verify(request.idToken)
        if (identity.email != null && !identity.emailVerified) {
            throw AuthException("Email address must be verified", code = "EMAIL_NOT_VERIFIED")
        }
        val user = userRepository.findByFirebaseUid(identity.uid).orElseGet {
            identity.email?.let(::normalizeEmail)
                ?.let(userRepository::findByEmail)
                ?.orElse(null)
                ?: identity.phoneNumber?.let(::normalizePhone)
                    ?.let(userRepository::findByPhoneNumber)
                    ?.orElse(null)
                ?: UserAccount(
                    email = identity.email?.let(::normalizeEmail),
                    phoneNumber = identity.phoneNumber?.let(::normalizePhone),
                    displayName = identity.displayName?.trim().takeUnless { it.isNullOrBlank() }
                        ?: identity.email?.substringBefore('@')
                        ?: "Phone user",
                    roles = mutableSetOf(UserRole.CUSTOMER)
                )
        }
        user.firebaseUid = identity.uid
        if (user.email == null) user.email = identity.email?.let(::normalizeEmail)
        if (user.phoneNumber == null) user.phoneNumber = identity.phoneNumber?.let(::normalizePhone)
        if (identity.phoneNumber != null) user.phoneVerified = true
        return authResponse(userRepository.save(user))
    }

    @Transactional(noRollbackFor = [AuthException::class])
    fun refresh(request: RefreshTokenRequest): AuthResponse {
        val replacement = refreshTokenService.rotate(request.refreshToken)
        return authResponse(replacement.entity.user, replacement.value)
    }

    @Transactional
    fun logout(request: LogoutRequest) {
        refreshTokenService.revoke(request.refreshToken)
    }

    @Transactional
    fun requestPhoneCode(request: PhoneCodeRequest): PhoneCodeResponse {
        val phone = normalizePhone(request.phoneNumber)
        val code = secureRandom.nextInt(1_000_000).toString().padStart(6, '0')
        val verification = phoneVerificationRepository.save(
            PhoneVerification(
                phoneNumber = phone,
                codeHash = passwordEncoder.encode(code),
                expiresAt = Instant.now(clock).plusSeconds(authProperties.phoneCodeMinutes * 60)
            )
        )

        return PhoneCodeResponse(
            verificationId = verification.id,
            expiresAt = verification.expiresAt,
            devCode = code.takeIf { authProperties.exposePhoneCode }
        )
    }

    @Transactional
    fun verifyPhone(request: VerifyPhoneRequest): AuthResponse {
        val phone = normalizePhone(request.phoneNumber)
        val verification = phoneVerificationRepository
            .findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(phone)
            .orElseThrow { AuthException("Phone verification code is invalid") }

        if (verification.expiresAt.isBefore(Instant.now(clock))) {
            throw AuthException("Phone verification code has expired")
        }
        if (verification.attempts >= 5) {
            throw AuthException("Too many verification attempts")
        }

        verification.attempts += 1
        if (!passwordEncoder.matches(request.code, verification.codeHash)) {
            throw AuthException("Phone verification code is invalid")
        }
        verification.consumedAt = Instant.now(clock)

        val user = userRepository.findByPhoneNumber(phone).orElseGet {
            UserAccount(
                phoneNumber = phone,
                displayName = request.displayName?.trim().takeUnless { it.isNullOrBlank() } ?: "Phone user",
                roles = mutableSetOf(UserRole.CUSTOMER)
            )
        }
        user.phoneVerified = true
        return authResponse(userRepository.save(user))
    }

    private fun authResponse(
        user: UserAccount,
        refreshToken: String = refreshTokenService.issue(user).value
    ): AuthResponse =
        AuthResponse(
            accessToken = jwtTokenService.createAccessToken(user),
            refreshToken = refreshToken,
            expiresInSeconds = jwtTokenService.accessTokenExpiresInSeconds(),
            user = user.toResponse()
        )

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun normalizePhone(phoneNumber: String): String {
        val normalized = phoneNumber.trim().replace(" ", "")
        if (!normalized.matches(Regex("^\\+[1-9]\\d{7,14}$"))) {
            throw AuthException("Phone number must be in E.164 format, for example +46701234567")
        }
        return normalized
    }
}
