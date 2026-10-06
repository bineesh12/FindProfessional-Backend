package com.findprofessional.marketplace.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import java.util.Optional
import java.util.UUID

class NotificationServiceTest {
    private val notifications = mock(MarketplaceNotificationRepository::class.java)
    private val devices = mock(NotificationDeviceRepository::class.java)
    private val push = mock(PushNotificationGateway::class.java)
    private val service = NotificationService(notifications, devices, push)

    @Test
    fun `list only returns notifications belonging to the authenticated user`() {
        val userId = UUID.randomUUID()
        val notification = MarketplaceNotification(
            userId = userId,
            type = MarketplaceNotificationType.OFFER_RECEIVED,
            title = "New offer received",
            body = "An offer arrived"
        )
        `when`(notifications.findAllByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(0, 50)))
            .thenReturn(listOf(notification))
        `when`(notifications.countByUserIdAndReadAtIsNull(userId)).thenReturn(1)

        val result = service.list(userId, 50)

        assertEquals(1, result.notifications.size)
        assertEquals(1, result.unreadCount)
        assertEquals(notification.id, result.notifications.single().id)
    }

    @Test
    fun `mark read rejects a notification owned by another user`() {
        val userId = UUID.randomUUID()
        val notificationId = UUID.randomUUID()
        `when`(notifications.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.empty())

        val error = assertThrows(NotificationException::class.java) {
            service.markRead(userId, notificationId)
        }

        assertEquals("NOTIFICATION_NOT_FOUND", error.code)
    }

    @Test
    fun `registering an existing token transfers it to the current user`() {
        val oldUserId = UUID.randomUUID()
        val currentUserId = UUID.randomUUID()
        val device = NotificationDevice(
            userId = oldUserId,
            token = "firebase-token",
            platform = NotificationPlatform.ANDROID
        )
        `when`(devices.findByToken(device.token)).thenReturn(Optional.of(device))
        `when`(devices.save(device)).thenReturn(device)

        val response = service.registerDevice(
            currentUserId,
            RegisterNotificationDeviceRequest(device.token, NotificationPlatform.IOS)
        )

        assertEquals(currentUserId, device.userId)
        assertEquals(NotificationPlatform.IOS, response.platform)
        assertNotNull(response.id)
        verify(devices).save(device)
    }

    @Test
    fun `opening a conversation marks its notifications read`() {
        val userId = UUID.randomUUID()
        val conversationId = UUID.randomUUID()
        val notification = MarketplaceNotification(
            userId = userId,
            type = MarketplaceNotificationType.MESSAGE_RECEIVED,
            title = "New message",
            body = "Hello",
            conversationId = conversationId
        )
        `when`(
            notifications.findAllByUserIdAndConversationIdAndReadAtIsNull(userId, conversationId)
        ).thenReturn(listOf(notification))

        service.markConversationRead(userId, conversationId)

        assertNotNull(notification.readAt)
    }
}
