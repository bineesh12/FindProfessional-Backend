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

enum class MessageSender { USER, ASSISTANT }

@Entity
@Table(name = "request_messages")
class RequestMessage(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    val session: RequestSession,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val sender: MessageSender,

    @Column(nullable = false)
    var content: String,

    @Column(name = "question_key")
    var questionKey: String? = null,

    @Column(name = "sequence_number", nullable = false)
    val sequenceNumber: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
