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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/users/me")
class UserController(
    private val userService: UserService,
    private val profileImageService: UserProfileImageService
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

    @PostMapping("/image")
    fun uploadProfileImage(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestPart("file") file: MultipartFile
    ): UserResponse = profileImageService.upload(UUID.fromString(jwt.subject), file)

    @DeleteMapping("/image")
    fun deleteProfileImage(
        @AuthenticationPrincipal jwt: Jwt
    ): UserResponse = profileImageService.delete(UUID.fromString(jwt.subject))

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

    @PutMapping("/locale")
    fun updateLocale(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: UpdateLocaleRequest
    ): LocaleResponse = userService.updateLocale(UUID.fromString(jwt.subject), request)
}
