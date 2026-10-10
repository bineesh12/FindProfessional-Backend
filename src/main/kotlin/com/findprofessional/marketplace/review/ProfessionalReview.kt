package com.findprofessional.marketplace.review

import com.findprofessional.marketplace.matching.ProfessionalOffer
import com.findprofessional.marketplace.request.CustomerRequest
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "professional_reviews")
class ProfessionalReview(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    val request: CustomerRequest,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false, unique = true)
    val offer: ProfessionalOffer,

    @Column(name = "customer_user_id", nullable = false)
    val customerUserId: UUID,

    @Column(name = "professional_user_id", nullable = false)
    val professionalUserId: UUID,

    @Column(nullable = false)
    var rating: Int,

    @Column(length = 1000)
    var comment: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun beforeUpdate() {
        updatedAt = Instant.now()
    }
}
