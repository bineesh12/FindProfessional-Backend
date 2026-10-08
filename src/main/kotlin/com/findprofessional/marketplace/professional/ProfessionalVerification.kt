package com.findprofessional.marketplace.professional

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

enum class ProfessionalBusinessType { COMPANY, SOLE_TRADER }

enum class ProfessionalVerificationStatus { NOT_STARTED, PENDING, VERIFIED, CHANGES_REQUIRED, EXPIRED }

@Entity
@Table(name = "professional_verifications")
class ProfessionalVerification(
    @Id
    @Column(name = "professional_user_id")
    val professionalUserId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 30)
    var businessType: ProfessionalBusinessType,

    @Column(name = "country_code", nullable = false, length = 2)
    var countryCode: String,

    @Column(name = "organization_number", nullable = false, length = 32)
    var organizationNumber: String,

    @Column(name = "f_tax_confirmed", nullable = false)
    var fTaxConfirmed: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: ProfessionalVerificationStatus = ProfessionalVerificationStatus.PENDING,

    @Column(name = "review_note", length = 500)
    var reviewNote: String? = null,

    @Column(name = "submitted_at", nullable = false)
    var submittedAt: Instant = Instant.now(),

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,

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
