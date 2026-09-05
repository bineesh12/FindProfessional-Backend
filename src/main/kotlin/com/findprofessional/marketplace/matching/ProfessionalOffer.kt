package com.findprofessional.marketplace.matching

import com.findprofessional.marketplace.request.CustomerRequest
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class ProfessionalOfferStatus { DRAFT, SUBMITTED, WITHDRAWN }

@Entity
@Table(name = "professional_offers")
class ProfessionalOffer(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "professional_user_id", nullable = false)
    val professionalUserId: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    val request: CustomerRequest,

    @Column(precision = 12, scale = 2)
    var amount: BigDecimal? = null,

    @Column(nullable = false, length = 3)
    var currency: String,

    @Column(length = 1000)
    var message: String? = null,

    @Column(name = "estimated_days")
    var estimatedDays: Int? = null,

    @Column(name = "available_start_date")
    var availableStartDate: LocalDate? = null,

    @Column(name = "scope_included", length = 2000)
    var scopeIncluded: String? = null,

    @Column(name = "scope_excluded", length = 2000)
    var scopeExcluded: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ProfessionalOfferStatus = ProfessionalOfferStatus.DRAFT,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun beforeUpdate() {
        updatedAt = Instant.now()
    }
}
