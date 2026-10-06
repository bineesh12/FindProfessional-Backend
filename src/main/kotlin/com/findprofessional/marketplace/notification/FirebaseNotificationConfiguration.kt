package com.findprofessional.marketplace.notification

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream

@Configuration
class FirebaseNotificationConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "app.notifications.firebase", name = ["enabled"], havingValue = "true")
    fun firebaseApp(properties: NotificationProperties): FirebaseApp {
        FirebaseApp.getApps().firstOrNull()?.let { return it }
        val options = FirebaseOptions.builder()
            .setCredentials(firebaseCredentials(properties))
            .apply { properties.projectId.takeIf(String::isNotBlank)?.let(::setProjectId) }
            .build()
        return FirebaseApp.initializeApp(options)
    }

    private fun firebaseCredentials(properties: NotificationProperties): GoogleCredentials =
        properties.serviceAccountJson.takeIf(String::isNotBlank)
            ?.let { json -> GoogleCredentials.fromStream(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))) }
            ?: GoogleCredentials.getApplicationDefault()

    @Bean
    @ConditionalOnProperty(prefix = "app.notifications.firebase", name = ["enabled"], havingValue = "true")
    fun firebasePushNotificationGateway(
        app: FirebaseApp,
        devices: NotificationDeviceRepository
    ): PushNotificationGateway = FirebasePushNotificationGateway(FirebaseMessaging.getInstance(app), devices)

    @Bean
    @ConditionalOnProperty(
        prefix = "app.notifications.firebase",
        name = ["enabled"],
        havingValue = "false",
        matchIfMissing = true
    )
    fun noOpPushNotificationGateway(): PushNotificationGateway = PushNotificationGateway { _, _ -> }
}

private class FirebasePushNotificationGateway(
    private val messaging: FirebaseMessaging,
    private val devices: NotificationDeviceRepository
) : PushNotificationGateway {
    override fun send(userId: java.util.UUID, notification: PushNotification) {
        devices.findAllByUserId(userId).forEach { device ->
            runCatching {
                messaging.send(
                    Message.builder()
                        .setToken(device.token)
                        .setNotification(Notification.builder().setTitle(notification.title).setBody(notification.body).build())
                        .putAllData(notification.data)
                        .build()
                )
            }.onFailure { error ->
                logger.warn("Firebase notification delivery failed for device {}", device.id, error)
            }
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(FirebasePushNotificationGateway::class.java)
    }
}
