package com.findprofessional.marketplace.notification

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.notifications.firebase")
data class NotificationProperties(
    val enabled: Boolean = false,
    val projectId: String = "",
    val serviceAccountJson: String = ""
)
