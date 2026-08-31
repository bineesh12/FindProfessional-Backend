package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.service.MarketplaceService
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class CustomerRequestStatus { PUBLISHED }

@Entity
@Table(name = "customer_requests")
class CustomerRequest(
    @Id
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    val session: RequestSession,

    @Column(name = "customer_id", nullable = false)
    val customerId: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    val category: ServiceCategory,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    val service: MarketplaceService,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: CustomerRequestStatus = CustomerRequestStatus.PUBLISHED,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val description: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
