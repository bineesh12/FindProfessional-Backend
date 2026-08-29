package com.findprofessional.marketplace.service

import com.findprofessional.marketplace.category.ServiceCategory
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
@Table(name = "marketplace_services")
class MarketplaceService(
    @Id
    val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    val category: ServiceCategory,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String,

    @Column(name = "short_description", nullable = false)
    val shortDescription: String,

    @Column(name = "icon_key", nullable = false)
    val iconKey: String,

    @Column(name = "image_url")
    val imageUrl: String? = null,

    @Column(name = "search_keywords", nullable = false)
    val searchKeywords: String = "",

    @Column(nullable = false)
    val popular: Boolean = false,

    @Column(name = "popularity_rank")
    val popularityRank: Int? = null,

    @Column(nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
