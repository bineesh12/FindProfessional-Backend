package com.findprofessional.marketplace.notification

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class MarketplaceNotificationType {
    MESSAGE_RECEIVED,
    OFFER_RECEIVED,
    OFFER_ACCEPTED,
    OFFER_DECLINED,
    REQUEST_UPDATED,
    REQUEST_CANCELLED,
    WORK_FINISHED,
    REQUEST_COMPLETED,
    REVIEW_RECEIVED,
    NEW_OPPORTUNITY
}

@Entity
@Table(name = "notifications")
class MarketplaceNotification(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val type: MarketplaceNotificationType,

    @Column(nullable = false, length = 120)
    val title: String,

    @Column(nullable = false, length = 500)
    val body: String,

    @Column(name = "request_id")
    val requestId: UUID? = null,

    @Column(name = "offer_id")
    val offerId: UUID? = null,

    @Column(name = "conversation_id")
    val conversationId: UUID? = null,

    @Column(name = "message_id")
    val messageId: UUID? = null,

    @Column(name = "read_at")
    var readAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
