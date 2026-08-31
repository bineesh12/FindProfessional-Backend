package com.findprofessional.marketplace.professional

import com.findprofessional.marketplace.service.MarketplaceService
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "portfolio_projects")
class PortfolioProject(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "professional_user_id", nullable = false)
    val professionalUserId: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    var service: MarketplaceService,

    @Column(nullable = false, length = 120)
    var title: String,

    @Column(nullable = false, length = 500)
    var description: String,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int,

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
