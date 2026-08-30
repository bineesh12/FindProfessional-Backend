package com.findprofessional.marketplace.request

import com.findprofessional.marketplace.category.ServiceCategory
import com.findprofessional.marketplace.question.ServiceQuestion
import com.findprofessional.marketplace.service.MarketplaceService
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

enum class RequestSessionStatus {
    NEEDS_DESCRIPTION,
    NEEDS_SERVICE,
    COLLECTING_ANSWERS,
    READY_FOR_REVIEW,
    CONFIRMED
}

@Entity
@Table(name = "request_sessions")
class RequestSession(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "customer_id", nullable = false)
    val customerId: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    var category: ServiceCategory,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    var service: MarketplaceService? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RequestSessionStatus,

    @Column(name = "initial_description")
    var initialDescription: String? = null,

    @Column(name = "draft_title")
    var draftTitle: String? = null,

    @Column(name = "draft_description")
    var draftDescription: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_question_id")
    var currentQuestion: ServiceQuestion? = null,

    @Column(name = "next_message_sequence", nullable = false)
    var nextMessageSequence: Int = 1,

    @Version
    var version: Long = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    fun touch() {
        updatedAt = Instant.now()
    }
}
