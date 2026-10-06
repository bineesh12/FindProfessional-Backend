package com.findprofessional.marketplace.conversation

import com.findprofessional.marketplace.request.CustomerRequest
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "conversations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["request_id", "professional_id"])]
)
class Conversation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    val request: CustomerRequest,

    @Column(name = "customer_id", nullable = false)
    val customerId: UUID,

    @Column(name = "professional_id", nullable = false)
    val professionalId: UUID,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
