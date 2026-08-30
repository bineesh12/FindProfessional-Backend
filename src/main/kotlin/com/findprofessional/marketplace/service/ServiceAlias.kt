package com.findprofessional.marketplace.service

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "service_aliases")
class ServiceAlias(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    val service: MarketplaceService,

    @Column(nullable = false)
    val phrase: String,

    @Column(nullable = false)
    val weight: Int,

    @Column(nullable = false)
    val active: Boolean = true
)
