package com.findprofessional.marketplace.professional

import com.findprofessional.marketplace.service.MarketplaceService
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "professional_services")
class ProfessionalServiceOffering(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "professional_user_id", nullable = false)
    val professionalUserId: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    val service: MarketplaceService,

    @Column(name = "is_primary", nullable = false)
    val primary: Boolean,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
