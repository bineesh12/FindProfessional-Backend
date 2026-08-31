package com.findprofessional.marketplace.professional

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
@Table(name = "portfolio_images")
class PortfolioImage(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    val project: PortfolioProject,

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    val storageKey: String,

    @Column(name = "original_filename", nullable = false)
    val originalFilename: String,

    @Column(name = "content_type", nullable = false, length = 100)
    val contentType: String,

    @Column(name = "size_bytes", nullable = false)
    val sizeBytes: Long,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
