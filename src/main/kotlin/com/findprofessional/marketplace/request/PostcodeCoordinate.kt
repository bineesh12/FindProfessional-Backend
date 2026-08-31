package com.findprofessional.marketplace.request

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "postcode_coordinates")
class PostcodeCoordinate(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "country_code", nullable = false)
    val countryCode: String,

    @Column(name = "postal_code", nullable = false)
    val postalCode: String,

    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false)
    val longitude: Double,

    @Column(nullable = false)
    val source: String,

    @Column(name = "resolved_at", nullable = false)
    val resolvedAt: Instant = Instant.now()
)
