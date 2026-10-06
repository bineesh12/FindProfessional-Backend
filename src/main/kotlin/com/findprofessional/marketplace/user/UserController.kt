package com.findprofessional.marketplace.user

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users/me")
class UserController(
    private val userService: UserService
) {
    @GetMapping
    fun currentUser(
        @AuthenticationPrincipal jwt: Jwt
    ): UserResponse = userService.getCurrentUser(UUID.fromString(jwt.subject))

    @PatchMapping
    fun updateProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: UpdateUserProfileRequest
    ): UserResponse = userService.updateProfile(UUID.fromString(jwt.subject), request)

    @GetMapping("/privacy")
    fun privacyPreferences(
        @AuthenticationPrincipal jwt: Jwt
    ): PrivacyPreferencesResponse = userService.getPrivacyPreferences(UUID.fromString(jwt.subject))

    @PutMapping("/privacy")
    fun updatePrivacyPreferences(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: UpdatePrivacyPreferencesRequest
    ): PrivacyPreferencesResponse = userService.updatePrivacyPreferences(UUID.fromString(jwt.subject), request)

    @PutMapping("/role")
    fun selectRole(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SelectRoleRequest
    ): UserResponse = userService.selectRole(UUID.fromString(jwt.subject), request)
}
