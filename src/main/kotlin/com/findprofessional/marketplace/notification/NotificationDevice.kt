package com.findprofessional.marketplace.notification

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class NotificationPlatform { ANDROID, IOS }

@Entity
@Table(name = "notification_devices")
class NotificationDevice(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(nullable = false, unique = true, length = 4096)
    val token: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var platform: NotificationPlatform,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PrePersist
    fun beforeInsert() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun beforeUpdate() {
        updatedAt = Instant.now()
    }
}
