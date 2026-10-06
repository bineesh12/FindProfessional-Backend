package com.findprofessional.marketplace.conversation

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
@Table(name = "conversation_messages")
class ConversationMessage(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    val conversation: Conversation,

    @Column(name = "sender_id", nullable = false)
    val senderId: UUID,

    @Column(nullable = false, length = 4000)
    val content: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "read_at")
    var readAt: Instant? = null
)
