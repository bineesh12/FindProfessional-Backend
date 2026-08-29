package com.findprofessional.marketplace.auth

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse =
        authService.register(request)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
        authService.login(request)

    @PostMapping("/google")
    fun loginWithGoogle(@Valid @RequestBody request: GoogleLoginRequest): AuthResponse =
        authService.loginWithGoogle(request)

    @PostMapping("/firebase")
    fun loginWithFirebase(@Valid @RequestBody request: FirebaseLoginRequest): AuthResponse =
        authService.loginWithFirebase(request)

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): AuthResponse =
        authService.refresh(request)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody request: LogoutRequest) {
        authService.logout(request)
    }

    @PostMapping("/phone/request-code")
    fun requestPhoneCode(@Valid @RequestBody request: PhoneCodeRequest): PhoneCodeResponse =
        authService.requestPhoneCode(request)

    @PostMapping("/phone/verify")
    fun verifyPhone(@Valid @RequestBody request: VerifyPhoneRequest): AuthResponse =
        authService.verifyPhone(request)
}
