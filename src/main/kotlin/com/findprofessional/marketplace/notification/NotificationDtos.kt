package com.findprofessional.marketplace.notification

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class MarketplaceNotificationResponse(
    val id: UUID,
    val type: MarketplaceNotificationType,
    val title: String,
    val body: String,
    val requestId: UUID?,
    val offerId: UUID?,
    val conversationId: UUID?,
    val messageId: UUID?,
    val readAt: Instant?,
    val createdAt: Instant
)

data class NotificationListResponse(
    val notifications: List<MarketplaceNotificationResponse>,
    val unreadCount: Long
)

data class UnreadNotificationCountResponse(val unreadCount: Long)

data class RegisterNotificationDeviceRequest(
    @field:NotBlank
    @field:Size(max = 4096)
    val token: String,
    val platform: NotificationPlatform
)

data class NotificationDeviceResponse(
    val id: UUID,
    val platform: NotificationPlatform
)
