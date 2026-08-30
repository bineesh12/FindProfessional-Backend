package com.findprofessional.marketplace.request

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class RequestLocationKind { SERVICE, PROJECT, PICKUP, DESTINATION }

@Entity
@Table(name = "request_locations")
class RequestLocation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    val request: CustomerRequest,

    @Enumerated(EnumType.STRING)
    @Column(name = "location_kind", nullable = false)
    val kind: RequestLocationKind,

    @Column(nullable = false)
    val municipality: String,

    @Column(name = "postal_code", nullable = false)
    val postalCode: String,

    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false)
    val longitude: Double,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
