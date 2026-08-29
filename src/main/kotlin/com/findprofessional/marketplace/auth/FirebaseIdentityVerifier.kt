package com.findprofessional.marketplace.auth

import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component

data class FirebaseIdentity(
    val uid: String,
    val email: String?,
    val emailVerified: Boolean,
    val phoneNumber: String?,
    val displayName: String?
)

interface FirebaseIdentityVerifier {
    fun verify(idToken: String): FirebaseIdentity
}

@Component
class FirebaseJwtIdentityVerifier(
    private val tokenDecoder: FirebaseTokenDecoder
) : FirebaseIdentityVerifier {
    override fun verify(idToken: String): FirebaseIdentity {
        val token = try {
            tokenDecoder.decode(idToken)
        } catch (_: JwtException) {
            throw AuthException(
                message = "Firebase identity token is invalid",
                code = "INVALID_FIREBASE_TOKEN",
                status = HttpStatus.UNAUTHORIZED
            )
        }
        return FirebaseIdentity(
            uid = token.subject,
            email = token.getClaimAsString("email"),
            emailVerified = token.getClaim<Boolean>("email_verified") == true,
            phoneNumber = token.getClaimAsString("phone_number"),
            displayName = token.getClaimAsString("name")
        )
    }
}

fun interface FirebaseTokenDecoder {
    fun decode(idToken: String): Jwt
}

@Component
class NimbusFirebaseTokenDecoder(
    authProperties: AuthProperties
) : FirebaseTokenDecoder {
    private val decoder: JwtDecoder

    init {
        val projectId = authProperties.firebaseProjectId.trim()
        require(projectId.isNotEmpty()) { "FIREBASE_PROJECT_ID must be configured" }
        val issuer = "https://securetoken.google.com/$projectId"
        decoder = NimbusJwtDecoder
            .withJwkSetUri(FirebaseJwksUri)
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build()
            .also {
                it.setJwtValidator(
                    DelegatingOAuth2TokenValidator(
                        JwtValidators.createDefaultWithIssuer(issuer),
                        FirebaseAudienceValidator(projectId),
                        FirebaseSubjectValidator
                    )
                )
            }
    }

    override fun decode(idToken: String): Jwt = decoder.decode(idToken)

    private companion object {
        const val FirebaseJwksUri =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
    }
}

private class FirebaseAudienceValidator(
    private val projectId: String
) : OAuth2TokenValidator<Jwt> {
    override fun validate(token: Jwt): OAuth2TokenValidatorResult =
        if (projectId in token.audience) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(InvalidAudience)
        }

    private companion object {
        val InvalidAudience = OAuth2Error("invalid_token", "Firebase token audience is invalid", null)
    }
}

private object FirebaseSubjectValidator : OAuth2TokenValidator<Jwt> {
    override fun validate(token: Jwt): OAuth2TokenValidatorResult =
        if (!token.subject.isNullOrBlank()) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(InvalidSubject)
        }

    private val InvalidSubject = OAuth2Error("invalid_token", "Firebase token subject is missing", null)
}
