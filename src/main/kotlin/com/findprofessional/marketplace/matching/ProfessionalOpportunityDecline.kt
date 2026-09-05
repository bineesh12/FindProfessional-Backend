package com.findprofessional.marketplace.matching

import com.findprofessional.marketplace.request.CustomerRequest
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
@Table(name = "professional_opportunity_declines")
class ProfessionalOpportunityDecline(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "professional_user_id", nullable = false)
    val professionalUserId: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    val request: CustomerRequest,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
