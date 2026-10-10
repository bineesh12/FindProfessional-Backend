package com.findprofessional.marketplace.subscription

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/professional/subscription")
class ProfessionalSubscriptionController(
    private val subscriptions: ProfessionalSubscriptionService
) {
    @GetMapping
    fun getStatus(@AuthenticationPrincipal jwt: Jwt): ProfessionalSubscriptionStatusResponse =
        subscriptions.getStatus(UUID.fromString(jwt.subject))

    @PostMapping("/google-play/verify")
    fun verifyGooglePlay(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: VerifyGooglePlaySubscriptionRequest
    ): ProfessionalSubscriptionStatusResponse =
        subscriptions.verifyGooglePlayPurchase(UUID.fromString(jwt.subject), request)

    @PostMapping("/app-store/verify")
    fun verifyAppStore(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: VerifyAppStoreSubscriptionRequest
    ): ProfessionalSubscriptionStatusResponse =
        subscriptions.verifyAppStorePurchase(UUID.fromString(jwt.subject), request)
}

@RestController
@RequestMapping("/api/admin/professional-subscriptions")
class ProfessionalSubscriptionAdminController(
    private val subscriptions: ProfessionalSubscriptionService
) {
    @PostMapping("/promotional-grants")
    fun grantPromotionalAccess(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: GrantPromotionalSubscriptionRequest
    ): ProfessionalSubscriptionStatusResponse =
        subscriptions.grantPromotionalAccess(UUID.fromString(jwt.subject), request)
}
