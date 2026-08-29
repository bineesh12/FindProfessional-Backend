package com.findprofessional.marketplace.auth

import com.findprofessional.marketplace.user.UserRole
import com.findprofessional.marketplace.user.UserResponse
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 128)
    val password: String,

    @field:NotBlank
    @field:Size(max = 160)
    val displayName: String,

    val phoneNumber: String? = null
)

data class LoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val password: String
)

data class GoogleLoginRequest(
    @field:NotBlank
    val idToken: String
)

data class FirebaseLoginRequest(
    @field:NotBlank
    val idToken: String
)

data class PhoneCodeRequest(
    @field:NotBlank
    val phoneNumber: String
)

data class VerifyPhoneRequest(
    @field:NotBlank
    val phoneNumber: String,

    @field:NotBlank
    val code: String,

    val displayName: String? = null
)

data class RefreshTokenRequest(
    @field:NotBlank
    val refreshToken: String
)

data class LogoutRequest(
    @field:NotBlank
    val refreshToken: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val tokenType: String = "Bearer",
    val user: UserResponse
)

data class PhoneCodeResponse(
    val verificationId: UUID,
    val expiresAt: Instant,
    val devCode: String? = null
)
