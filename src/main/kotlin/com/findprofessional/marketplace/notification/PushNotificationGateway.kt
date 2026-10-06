package com.findprofessional.marketplace.notification

import java.util.UUID

data class PushNotification(
    val title: String,
    val body: String,
    val data: Map<String, String>
)

fun interface PushNotificationGateway {
    fun send(userId: UUID, notification: PushNotification)
}
