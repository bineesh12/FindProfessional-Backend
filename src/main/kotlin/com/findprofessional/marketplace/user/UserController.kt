package com.findprofessional.marketplace.user

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
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
    @PutMapping("/role")
    fun selectRole(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SelectRoleRequest
    ): UserResponse = userService.selectRole(UUID.fromString(jwt.subject), request)
}
