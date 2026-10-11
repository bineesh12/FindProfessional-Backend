package com.findprofessional.marketplace.auth

import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/api/web-auth")
class WebAuthController(
    private val authService: AuthService,
    private val properties: AuthProperties
) {
    @PostMapping("/firebase")
    fun loginWithFirebase(
        @Valid @RequestBody request: FirebaseLoginRequest,
        response: HttpServletResponse
    ): WebAuthResponse = authService.loginWithFirebase(request).withRefreshCookie(response)

    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(name = RefreshCookieName, required = false) refreshToken: String?,
        response: HttpServletResponse
    ): WebAuthResponse {
        val token = refreshToken?.takeIf(String::isNotBlank)
            ?: throw AuthException(
                message = "Authentication session is unavailable",
                code = "INVALID_REFRESH_TOKEN",
                status = HttpStatus.UNAUTHORIZED
            )
        return authService.refresh(RefreshTokenRequest(token)).withRefreshCookie(response)
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue(name = RefreshCookieName, required = false) refreshToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        refreshToken?.takeIf(String::isNotBlank)?.let {
            authService.logout(LogoutRequest(it))
        }
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
        return ResponseEntity.noContent().build()
    }

    private fun AuthResponse.withRefreshCookie(response: HttpServletResponse): WebAuthResponse {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            refreshCookie(refreshToken, Duration.ofDays(properties.refreshTokenDays)).toString()
        )
        return toWebResponse()
    }

    private fun refreshCookie(value: String, maxAge: Duration): ResponseCookie =
        ResponseCookie.from(RefreshCookieName, value)
            .httpOnly(true)
            .secure(properties.webCookieSecure)
            .sameSite(properties.webCookieSameSite)
            .path("/api/web-auth")
            .maxAge(maxAge)
            .build()

    private companion object {
        const val RefreshCookieName = "arbio_refresh"
    }
}
