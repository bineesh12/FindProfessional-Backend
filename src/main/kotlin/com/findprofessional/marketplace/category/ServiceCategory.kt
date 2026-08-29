package com.findprofessional.marketplace.category

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "service_categories")
class ServiceCategory(
    @Id
    val id: UUID,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String,

    @Column(name = "icon_key", nullable = false)
    val iconKey: String,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
