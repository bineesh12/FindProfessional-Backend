package com.findprofessional.marketplace.subscription

import com.findprofessional.marketplace.request.CustomerRequest
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

enum class SubscriptionSource { GOOGLE_PLAY, APP_STORE, PROMOTIONAL }

@Entity
@Table(name = "professional_subscription_entitlements")
class ProfessionalSubscriptionEntitlement(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "professional_user_id", nullable = false)
    val professionalUserId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val source: SubscriptionSource,

    @Column(name = "external_reference", length = 128)
    val externalReference: String? = null,

    @Column(name = "starts_at", nullable = false)
    var startsAt: Instant,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Column(length = 500)
    val note: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant
)

@Entity
@Table(
    name = "professional_opportunity_views",
    uniqueConstraints = [UniqueConstraint(
        name = "uq_professional_opportunity_view",
        columnNames = ["professional_user_id", "request_id"]
    )]
)
class ProfessionalOpportunityView(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "professional_user_id", nullable = false)
    val professionalUserId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: CustomerRequest,

    @Column(name = "viewed_at", nullable = false)
    val viewedAt: Instant
)
