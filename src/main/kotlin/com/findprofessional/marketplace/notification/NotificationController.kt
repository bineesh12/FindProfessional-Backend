package com.findprofessional.marketplace.notification

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/notifications")
class NotificationController(private val service: NotificationService) {
    @GetMapping
    fun list(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "50") limit: Int
    ) = service.list(UUID.fromString(jwt.subject), limit)

    @GetMapping("/unread-count")
    fun unreadCount(@AuthenticationPrincipal jwt: Jwt) = service.unreadCount(UUID.fromString(jwt.subject))

    @PutMapping("/{notificationId}/read")
    fun markRead(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable notificationId: UUID
    ) = service.markRead(UUID.fromString(jwt.subject), notificationId)

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markAllRead(@AuthenticationPrincipal jwt: Jwt) = service.markAllRead(UUID.fromString(jwt.subject))

    @PostMapping("/devices")
    fun registerDevice(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody input: RegisterNotificationDeviceRequest
    ) = service.registerDevice(UUID.fromString(jwt.subject), input)

    @DeleteMapping("/devices/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unregisterDevice(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable deviceId: UUID
    ) = service.unregisterDevice(UUID.fromString(jwt.subject), deviceId)
}
