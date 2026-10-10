package com.findprofessional.marketplace.notification

import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.UUID
import com.findprofessional.marketplace.localization.LocalizedTextService
import com.findprofessional.marketplace.user.UserAccountRepository

data class CreateNotification(
    val userId: UUID,
    val type: MarketplaceNotificationType,
    val title: String? = null,
    val body: String? = null,
    val titleKey: String? = null,
    val bodyKey: String? = null,
    val titleArguments: List<Any> = emptyList(),
    val bodyArguments: List<Any> = emptyList(),
    val requestId: UUID? = null,
    val offerId: UUID? = null,
    val conversationId: UUID? = null,
    val messageId: UUID? = null
)

@Service
class NotificationService(
    private val notifications: MarketplaceNotificationRepository,
    private val devices: NotificationDeviceRepository,
    private val push: PushNotificationGateway,
    private val users: UserAccountRepository,
    private val text: LocalizedTextService
) {
    @Transactional(readOnly = true)
    fun list(userId: UUID, limit: Int): NotificationListResponse {
        if (limit !in 1..MaximumListSize) {
            throw NotificationException("Notification limit must be between 1 and $MaximumListSize", "INVALID_LIMIT")
        }
        return NotificationListResponse(
            notifications.findAllByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(0, limit))
                .map { it.toResponse() },
            notifications.countByUserIdAndReadAtIsNull(userId)
        )
    }

    @Transactional(readOnly = true)
    fun unreadCount(userId: UUID) = UnreadNotificationCountResponse(
        notifications.countByUserIdAndReadAtIsNull(userId)
    )

    @Transactional
    fun markRead(userId: UUID, notificationId: UUID): MarketplaceNotificationResponse {
        val notification = notifications.findByIdAndUserId(notificationId, userId).orElseThrow {
            NotificationException("Notification was not found", "NOTIFICATION_NOT_FOUND", HttpStatus.NOT_FOUND)
        }
        if (notification.readAt == null) notification.readAt = Instant.now()
        return notification.toResponse()
    }

    @Transactional
    fun markAllRead(userId: UUID) {
        val now = Instant.now()
        notifications.findAllByUserIdAndReadAtIsNull(userId).forEach { it.readAt = now }
    }

    @Transactional
    fun markConversationRead(userId: UUID, conversationId: UUID) {
        val now = Instant.now()
        notifications.findAllByUserIdAndConversationIdAndReadAtIsNull(userId, conversationId)
            .forEach { it.readAt = now }
    }

    @Transactional
    fun registerDevice(userId: UUID, input: RegisterNotificationDeviceRequest): NotificationDeviceResponse {
        val token = input.token.trim()
        val device = devices.findByToken(token).orElseGet {
            NotificationDevice(userId = userId, token = token, platform = input.platform)
        }
        device.userId = userId
        device.platform = input.platform
        return devices.save(device).let { NotificationDeviceResponse(it.id, it.platform) }
    }

    @Transactional
    fun unregisterDevice(userId: UUID, deviceId: UUID) {
        devices.deleteByIdAndUserId(deviceId, userId)
    }

    fun create(input: CreateNotification): MarketplaceNotification {
        val locale = users.findById(input.userId).map { it.preferredLocale }.orElse("en")
        val title = input.titleKey?.let { text.get(it, locale, *input.titleArguments.toTypedArray()) }
            ?: requireNotNull(input.title) { "Notification title or titleKey is required" }
        val body = input.bodyKey?.let { text.get(it, locale, *input.bodyArguments.toTypedArray()) }
            ?: requireNotNull(input.body) { "Notification body or bodyKey is required" }
        val saved = notifications.save(
            MarketplaceNotification(
                userId = input.userId,
                type = input.type,
                title = title.take(120),
                body = body.take(500),
                requestId = input.requestId,
                offerId = input.offerId,
                conversationId = input.conversationId,
                messageId = input.messageId
            )
        )
        dispatchAfterCommit(saved)
        return saved
    }

    private fun dispatchAfterCommit(notification: MarketplaceNotification) {
        val actionData = buildMap {
            put("notificationId", notification.id.toString())
            put("type", notification.type.name)
            notification.requestId?.let { put("requestId", it.toString()) }
            notification.offerId?.let { put("offerId", it.toString()) }
            notification.conversationId?.let { put("conversationId", it.toString()) }
        }
        val dispatch = {
            push.send(notification.userId, PushNotification(notification.title, notification.body, actionData))
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            dispatch()
        } else {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() = dispatch()
            })
        }
    }

    private fun MarketplaceNotification.toResponse() = MarketplaceNotificationResponse(
        id, type, title, body, requestId, offerId, conversationId, messageId, readAt, createdAt
    )

    private companion object {
        const val MaximumListSize = 100
    }
}
