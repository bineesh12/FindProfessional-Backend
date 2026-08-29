package com.findprofessional.marketplace.auth

import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component

interface GoogleIdentityVerifier {
    fun verify(idToken: String): GoogleIdentity
}

@Component
class GoogleJwtIdentityVerifier(
    private val authProperties: AuthProperties
) : GoogleIdentityVerifier {
    private val allowedClientIds: Set<String>
        get() = authProperties.googleClientIds.filter(String::isNotBlank).toSet()

    private val decoder: NimbusJwtDecoder by lazy {
        NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build().also { decoder ->
            decoder.setJwtValidator(
                DelegatingOAuth2TokenValidator(
                    JwtValidators.createDefault(),
                    issuerValidator(),
                    audienceValidator(),
                    verifiedEmailValidator()
                )
            )
        }
    }

    override fun verify(idToken: String): GoogleIdentity {
        if (allowedClientIds.isEmpty()) {
            throw AuthException("Google login is not configured", code = "GOOGLE_AUTH_NOT_CONFIGURED")
        }

        val jwt = try {
            decoder.decode(idToken)
        } catch (_: JwtException) {
            throw invalidGoogleToken()
        }
        val subject = jwt.getClaimAsString(JwtClaimNames.SUB)?.takeIf(String::isNotBlank)
            ?: throw invalidGoogleToken()
        val email = jwt.getClaimAsString("email")?.takeIf(String::isNotBlank)
            ?: throw invalidGoogleToken()

        return GoogleIdentity(
            subject = subject,
            email = email,
            displayName = jwt.getClaimAsString("name")
        )
    }

    private fun issuerValidator() = OAuth2TokenValidator<Jwt> { jwt ->
        if (jwt.issuer?.toString() in GOOGLE_ISSUERS) success() else failure("Google token issuer is invalid")
    }

    private fun audienceValidator() = OAuth2TokenValidator<Jwt> { jwt ->
        if (jwt.audience.any(allowedClientIds::contains)) success() else failure("Google token audience is invalid")
    }

    private fun verifiedEmailValidator() = OAuth2TokenValidator<Jwt> { jwt ->
        if (jwt.getClaimAsBoolean("email_verified") == true) success() else failure("Google email is not verified")
    }

    private fun success(): OAuth2TokenValidatorResult = OAuth2TokenValidatorResult.success()

    private fun failure(description: String): OAuth2TokenValidatorResult =
        OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", description, null))

    private fun invalidGoogleToken() = AuthException(
        message = "Google identity token is invalid",
        code = "INVALID_GOOGLE_TOKEN",
        status = HttpStatus.UNAUTHORIZED
    )

    private companion object {
        const val GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs"
        val GOOGLE_ISSUERS = setOf("https://accounts.google.com", "accounts.google.com")
    }
}
