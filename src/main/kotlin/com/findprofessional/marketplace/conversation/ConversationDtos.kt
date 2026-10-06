package com.findprofessional.marketplace.conversation

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class OpenConversationRequest(
    val requestId: UUID,
    val professionalId: UUID? = null
)

data class SendConversationMessageRequest(
    @field:NotBlank
    @field:Size(max = 4000)
    val content: String
)

data class ConversationSummaryResponse(
    val id: UUID,
    val requestId: UUID,
    val requestTitle: String,
    val otherParticipantId: UUID,
    val otherParticipantName: String,
    val lastMessage: String?,
    val lastMessageAt: Instant?,
    val unreadCount: Long,
    val canSendMessages: Boolean
)

data class ConversationMessageResponse(
    val id: UUID,
    val senderId: UUID,
    val content: String,
    val createdAt: Instant,
    val readAt: Instant?
)

data class ConversationThreadResponse(
    val conversation: ConversationSummaryResponse,
    val messages: List<ConversationMessageResponse>
)
