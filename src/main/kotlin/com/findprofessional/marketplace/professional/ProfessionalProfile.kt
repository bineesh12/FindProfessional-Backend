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
@Table(name = "professional_profiles")
class ProfessionalProfile(
    @Id
    @Column(name = "user_id")
    val userId: UUID,

    @Column(name = "business_name", nullable = false)
    var businessName: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_service_id", nullable = false)
    var primaryService: MarketplaceService,

    @Column(name = "service_area", nullable = false)
    var serviceArea: String,

    @Column(name = "experience_years", nullable = false)
    var experienceYears: Int,

    @Column(name = "contact_email", nullable = false)
    var contactEmail: String,

    @Column(nullable = false, length = 500)
    var about: String,

    @Column(name = "service_postal_code", length = 20)
    var servicePostalCode: String? = null,

    @Column
    var latitude: Double? = null,

    @Column
    var longitude: Double? = null,

    @Column(name = "service_radius_km", nullable = false)
    var serviceRadiusKm: Double = 50.0,

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
