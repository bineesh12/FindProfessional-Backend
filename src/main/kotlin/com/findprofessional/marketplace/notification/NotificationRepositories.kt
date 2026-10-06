package com.findprofessional.marketplace.notification

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface MarketplaceNotificationRepository : JpaRepository<MarketplaceNotification, UUID> {
    fun findAllByUserIdOrderByCreatedAtDescIdDesc(userId: UUID, pageable: Pageable): List<MarketplaceNotification>
    fun countByUserIdAndReadAtIsNull(userId: UUID): Long
    fun findByIdAndUserId(id: UUID, userId: UUID): Optional<MarketplaceNotification>
    fun findAllByUserIdAndReadAtIsNull(userId: UUID): List<MarketplaceNotification>
    fun findAllByUserIdAndConversationIdAndReadAtIsNull(
        userId: UUID,
        conversationId: UUID
    ): List<MarketplaceNotification>
}

interface NotificationDeviceRepository : JpaRepository<NotificationDevice, UUID> {
    fun findByToken(token: String): Optional<NotificationDevice>
    fun findAllByUserId(userId: UUID): List<NotificationDevice>
    fun deleteByIdAndUserId(id: UUID, userId: UUID): Long
}
