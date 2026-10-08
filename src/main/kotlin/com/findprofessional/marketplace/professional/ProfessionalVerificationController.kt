package com.findprofessional.marketplace.professional

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/professional-profile/verification")
class ProfessionalVerificationController(private val service: ProfessionalVerificationService) {
    @GetMapping
    fun get(@AuthenticationPrincipal jwt: Jwt): ProfessionalVerificationResponse =
        service.get(UUID.fromString(jwt.subject))

    @PutMapping
    fun submit(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SubmitProfessionalVerificationRequest
    ): ProfessionalVerificationResponse = service.submit(UUID.fromString(jwt.subject), request)
}
